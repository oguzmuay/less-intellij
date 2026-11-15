package dev.lessai.inbox

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import dev.lessai.session.AiProvider
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.concurrent.CopyOnWriteArrayList

/** Per-project agent mailbox: a folder of inbox markdown files that agents and the user append to. */
@Service(Service.Level.PROJECT)
class MailboxService(private val project: Project) : Disposable {

    private val listeners = CopyOnWriteArrayList<MailboxListener>()
    private val readCounts = mutableMapOf<String, Int>()
    private val lock = Any()
    private var watchService: WatchService? = null
    private var readStateLoaded = false

    /** Absolute mailbox directory for this project. */
    val dir: Path by lazy {
        val base = project.basePath ?: project.name
        Paths.get(PathManager.getSystemPath(), "less-ai", "mailboxes", "${project.name}-${shortHash(base)}")
    }

    /** Creates the folder, README, .gitignore, inbox files and shared dir if missing; idempotent. */
    fun ensureCreated() {
        synchronized(lock) {
            Files.createDirectories(dir.resolve(INBOX_DIR))
            Files.createDirectories(dir.resolve(SHARED_DIR))
            writeIfMissing(dir.resolve("README.md"), readmeText())
            writeIfMissing(dir.resolve(".gitignore"), "*\n")
            agentNames().forEach { writeIfMissing(inboxFileFor(it), "") }
        }
        startWatching()
    }

    /** Environment variables for an agent process: its name and the mailbox directory. */
    fun environmentFor(agentName: String): Map<String, String> =
        mapOf("LESS_AI_NAME" to agentName, "LESS_AI_DIR" to dir.toString())

    /** System prompt fragment that teaches an agent how to use the mailbox. */
    fun systemPromptFor(agentName: String): String {
        if (agentName == AiProvider.OLLAMA.displayName) {
            return "You only receive messages from the user and other agents; you cannot read files. " +
                "Your inbox is $dir/inbox/$agentName.md and its new messages are passed to you directly."
        }
        return "To message another agent, append to $dir/inbox/<agent>.md a section starting with " +
            "'## from <your name> <time>'. Your inbox is $dir/inbox/$agentName.md; read it when the user asks " +
            "or when you are told you have new mail. Put files you want to share in $dir/shared/. " +
            "Only message other agents when the task needs it."
    }

    /** Path of the inbox markdown file belonging to [agentName]. */
    fun inboxFileFor(agentName: String): Path = dir.resolve(INBOX_DIR).resolve("$agentName.md")

    /** Appends a message section from [from] to the inbox of [to] and notifies listeners. */
    fun send(from: String, to: String, message: String) {
        ensureCreated()
        val section = "\n## from $from ${LocalDateTime.now().withNano(0)}\n$message\n"
        synchronized(lock) {
            Files.writeString(inboxFileFor(to), section, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
        }
        fire(to)
    }

    /** Parses the inbox file of [agentName] into messages, oldest first. */
    fun messages(agentName: String): List<MailMessage> {
        val file = inboxFileFor(agentName)
        if (!Files.exists(file)) return emptyList()
        val text = synchronized(lock) { runCatching { Files.readString(file) }.getOrDefault("") }
        return parse(text)
    }

    /** Number of messages in the inbox of [agentName] that were not marked read yet. */
    fun unreadCount(agentName: String): Int {
        val read = synchronized(lock) { loadReadState(); readCounts[agentName] ?: 0 }
        return (messages(agentName).size - read).coerceAtLeast(0)
    }

    /** Marks every current message of [agentName] as read, persists the state and notifies listeners. */
    fun markRead(agentName: String) {
        val total = messages(agentName).size
        val changed = synchronized(lock) {
            loadReadState()
            val old = readCounts[agentName] ?: 0
            readCounts[agentName] = total
            if (old != total) saveReadState()
            old != total
        }
        if (changed) fire(agentName)
    }

    /** Registers a listener (called on the EDT) that is removed when [parent] is disposed. */
    fun addListener(listener: MailboxListener, parent: Disposable) {
        listeners.add(listener)
        Disposer.register(parent) { listeners.remove(listener) }
    }

    /** Stops the file watcher and drops all listeners. */
    override fun dispose() {
        runCatching { watchService?.close() }
        watchService = null
        listeners.clear()
    }

    /** Names of all agents that have an inbox. */
    private fun agentNames(): List<String> = AiProvider.entries.map { it.displayName }

    /** Builds the README text placed in the mailbox folder. */
    private fun readmeText(): String = "# LESS AI mailbox\n" +
        "Agents in this editor talk through this folder.\n" +
        "- Send: append to `inbox/<agent>.md` a section `## from <your name> <time>` followed by the message.\n" +
        "- Receive: read `inbox/<your name>.md` (your name is in `\$LESS_AI_NAME`). New messages are at the end.\n" +
        "- Share files in `shared/`. Ollama agents cannot read files; they only receive messages.\n\n" +
        "## Agents\n" + agentNames().joinToString("\n") { "- $it" } + "\n"

    /** Writes [content] to [path] only if the file does not exist yet. */
    private fun writeIfMissing(path: Path, content: String) {
        if (!Files.exists(path)) Files.writeString(path, content)
    }

    /** Splits inbox text into messages at `## from ` headings. */
    private fun parse(text: String): List<MailMessage> {
        val result = mutableListOf<MailMessage>()
        var header: String? = null
        val body = StringBuilder()
        fun flush() {
            val h = header ?: return
            val idx = h.lastIndexOf(' ')
            val from = if (idx > 0) h.substring(0, idx).trim() else h.trim()
            val time = if (idx > 0) h.substring(idx + 1).trim() else ""
            result.add(MailMessage(from, time, body.toString().trim()))
        }
        for (line in text.lines()) {
            if (line.startsWith(HEADING_PREFIX)) {
                flush()
                header = line.removePrefix(HEADING_PREFIX)
                body.setLength(0)
            } else if (header != null) {
                body.appendLine(line)
            }
        }
        flush()
        return result
    }

    /** Loads `.read-state` into memory once. */
    private fun loadReadState() {
        if (readStateLoaded) return
        readStateLoaded = true
        val file = dir.resolve(READ_STATE_FILE)
        if (!Files.exists(file)) return
        runCatching {
            val type = object : TypeToken<Map<String, Int>>() {}.type
            Gson().fromJson<Map<String, Int>>(Files.readString(file), type)?.let { readCounts.putAll(it) }
        }
    }

    /** Persists the read counts to `.read-state` as json. */
    private fun saveReadState() {
        runCatching {
            Files.createDirectories(dir)
            Files.writeString(dir.resolve(READ_STATE_FILE), Gson().toJson(readCounts))
        }
    }

    /** Starts the daemon WatchService loop on `inbox/` once; failures are ignored. */
    private fun startWatching() {
        synchronized(lock) {
            if (watchService != null) return
            val service = runCatching {
                dir.fileSystem.newWatchService().also {
                    dir.resolve(INBOX_DIR).register(
                        it, StandardWatchEventKinds.ENTRY_MODIFY, StandardWatchEventKinds.ENTRY_CREATE
                    )
                }
            }.getOrNull() ?: return
            watchService = service
            AppExecutorUtil.getAppExecutorService().execute { watchLoop(service) }
        }
    }

    /** Blocks on the watch service and fires a change for each modified inbox file. */
    private fun watchLoop(service: WatchService) {
        try {
            while (true) {
                val key = service.take()
                key.pollEvents().forEach { event ->
                    val name = (event.context() as? Path)?.fileName?.toString()?.removeSuffix(".md")
                    if (name != null && name in agentNames()) fire(name)
                }
                if (!key.reset()) break
            }
        } catch (_: Exception) {
            // closed or interrupted: stop watching
        }
    }

    /** Dispatches a change for [agentName] to all listeners on the EDT. */
    private fun fire(agentName: String) {
        ApplicationManager.getApplication().invokeLater({
            listeners.forEach { it.mailChanged(agentName) }
        }, project.disposed)
    }

    /** Returns the first 8 hex characters of the SHA-256 of [value]. */
    private fun shortHash(value: String): String =
        MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).take(4).joinToString("") { "%02x".format(it) }

    companion object {
        private const val INBOX_DIR = "inbox"
        private const val SHARED_DIR = "shared"
        private const val READ_STATE_FILE = ".read-state"
        private const val HEADING_PREFIX = "## from "

        /** Returns the project's mailbox service. */
        fun getInstance(project: Project): MailboxService = project.service()
    }
}

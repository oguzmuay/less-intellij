package dev.lessai.session

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import dev.lessai.inbox.MailboxService
import dev.lessai.status.StatusFiles
import java.nio.file.Files

/** Builds the mailbox part of a session launch: LESS_AI_* env vars and the inbox system prompt. */
object MailboxLaunch {

    private val LOG = logger<MailboxLaunch>()

    /**
     * Ensures the mailbox exists and returns env + prompt args for the provider; empty on failure.
     * Example: CLAUDE -> env {LESS_AI_NAME, LESS_AI_DIR}, args [--append-system-prompt "$(cat '<file>')"]
     */
    fun contextFor(project: Project, sessionId: String, provider: AiProvider): LaunchContext = try {
        val mailbox = MailboxService.getInstance(project)
        mailbox.ensureCreated()
        val agent = provider.displayName
        val env = mailbox.environmentFor(agent)
        val args = when (provider) {
            AiProvider.CLAUDE -> {
                val file = writePrompt(sessionId, agent, mailbox.systemPromptFor(agent))
                listOf("--append-system-prompt", catFile(file))
            }
            AiProvider.CODEX -> {
                // The file holds a TOML string literal so Codex parses it as one string value.
                val file = writePrompt(sessionId, agent, CommandBuilder.tomlString(mailbox.systemPromptFor(agent)))
                listOf("-c", "\"developer_instructions=\$(cat ${CommandBuilder.shellQuote(file)})\"")
            }
            AiProvider.GEMINI, AiProvider.OLLAMA -> emptyList()
        }
        LaunchContext(env, args)
    } catch (e: Throwable) {
        LOG.warn("Mailbox launch context unavailable for $provider", e)
        LaunchContext.EMPTY
    }

    /** Writes the prompt text to the session's prompt file and returns its path. */
    private fun writePrompt(sessionId: String, agent: String, text: String): String {
        val file = StatusFiles.promptFile(sessionId, agent)
        Files.writeString(file, text)
        return file.toString()
    }

    /**
     * Shell fragment that expands to the file's content as one argument.
     * Example: "/p/a.txt" -> "\"$(cat '/p/a.txt')\""
     */
    private fun catFile(path: String): String = "\"\$(cat ${CommandBuilder.shellQuote(path)})\""
}

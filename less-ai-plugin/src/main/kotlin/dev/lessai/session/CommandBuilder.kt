package dev.lessai.session

import com.intellij.openapi.application.ApplicationManager
import dev.lessai.settings.LessAiSettings
import java.io.File
import java.util.concurrent.TimeUnit

/** Resolves provider CLI binaries and builds the shell commands that start AI sessions. */
object CommandBuilder {

    private val home: String = System.getProperty("user.home")

    /** Well-known install directories checked before falling back to PATH. */
    val SEARCH_DIRS: List<String> = listOf(
        "/opt/homebrew/bin",
        "/usr/local/bin",
        "$home/.local/bin",
        "$home/.claude/local"
    )

    /**
     * Finds the executable for a provider: user-configured dirs, known dirs, then PATH.
     * Example: CLAUDE -> "/opt/homebrew/bin/claude", not installed -> null
     */
    fun resolveBinary(provider: AiProvider): String? {
        val name = provider.binary
        val pathEntries = System.getenv("PATH")
            ?.split(':')
            ?.filter { it.isNotBlank() }
            .orEmpty()
        for (dir in userBinaryDirs() + SEARCH_DIRS + pathEntries) {
            val candidate = File(dir, name)
            if (candidate.isFile && candidate.canExecute()) return candidate.path
        }
        return null
    }

    /**
     * Builds the shell command line to type into a terminal for the given request and launch context.
     * Example: CLAUDE, dir "/a b", args ["--x"] -> "CLAUDE_CONFIG_DIR='/a b' /opt/homebrew/bin/claude --x"
     */
    fun buildCommand(request: NewSessionRequest, context: LaunchContext = LaunchContext.EMPTY): String {
        val path = resolveBinary(request.provider) ?: request.provider.binary
        val bin = if (path.any { it.isWhitespace() }) shellQuote(path) else path
        val env = LinkedHashMap<String, String>()
        val providerArgs = mutableListOf<String>()
        when (request.provider) {
            AiProvider.CLAUDE -> request.claudeConfigDir?.takeIf { it.isNotBlank() }?.let { env["CLAUDE_CONFIG_DIR"] = it }
            AiProvider.OLLAMA -> {
                val model = request.ollamaModel?.trim()?.takeIf { it.isNotEmpty() } ?: "llama3"
                providerArgs += listOf("run", shellQuote(model))
            }
            AiProvider.CODEX, AiProvider.GEMINI -> Unit
        }
        env.putAll(context.env)
        val envPrefix = env.entries
            .filter { ENV_KEY.matches(it.key) }
            .map { "${it.key}=${shellQuote(it.value)}" }
        return (envPrefix + bin + providerArgs + context.extraArgs).joinToString(" ")
    }

    /**
     * Picks the first unused default name for a provider, e.g. "Claude", "Claude 2", "Claude 3".
     * Example: ("Claude", ["Claude"]) -> "Claude 2"
     */
    fun autoName(provider: AiProvider, existingNames: Collection<String>): String {
        val base = provider.displayName
        if (base !in existingNames) return base
        var n = 2
        while ("$base $n" in existingNames) n++
        return "$base $n"
    }

    /**
     * Runs `ollama list` and returns the model names, or an empty list on any failure.
     * Example: output "NAME ID SIZE\nllama3:latest abc 4GB" -> ["llama3:latest"]
     */
    fun listOllamaModels(): List<String> {
        val bin = resolveBinary(AiProvider.OLLAMA) ?: return emptyList()
        return try {
            val process = ProcessBuilder(bin, "list")
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start()
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly()
                return emptyList()
            }
            if (process.exitValue() != 0) return emptyList()
            process.inputStream.bufferedReader().readText()
                .lines()
                .filter { it.isNotBlank() }
                .drop(1)
                .mapNotNull { line -> line.trim().split(Regex("\\s+")).firstOrNull() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Wraps a string in single quotes for POSIX shells, escaping embedded single quotes.
     * Example: "it's" -> "'it'\\''s'"
     */
    fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"

    /**
     * Encodes a string as a TOML basic (double-quoted) string.
     * Example: "say \"hi\"\nnow" -> "\"say \\\"hi\\\"\\nnow\""
     */
    fun tomlString(s: String): String = jsonString(s)

    /**
     * Encodes a string as a JSON string literal (also valid as a TOML basic string).
     * Example: "a\"b" -> "\"a\\\"b\""
     */
    fun jsonString(s: String): String {
        val sb = StringBuilder(s.length + 2).append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c < ' ' || c == '\u007f' -> sb.append(String.format("\\u%04x", c.code))
                else -> sb.append(c)
            }
        }
        return sb.append('"').toString()
    }

    /** Reads extra binary directories from the plugin settings; empty if the service is unavailable. */
    private fun userBinaryDirs(): List<String> = try {
        ApplicationManager.getApplication()
            ?.getService(LessAiSettings::class.java)
            ?.extraBinaryDirs
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
    } catch (e: Throwable) {
        emptyList()
    }

    /** Valid POSIX environment variable names. */
    private val ENV_KEY = Regex("[A-Za-z_][A-Za-z0-9_]*")
}

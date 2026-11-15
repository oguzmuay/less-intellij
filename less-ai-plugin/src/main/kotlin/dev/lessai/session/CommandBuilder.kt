package dev.lessai.session

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
     * Finds the executable for a provider, checking known directories first and then PATH.
     * Example: CLAUDE -> "/opt/homebrew/bin/claude", not installed -> null
     */
    fun resolveBinary(provider: AiProvider): String? {
        val name = provider.binary
        for (dir in SEARCH_DIRS) {
            val candidate = File(dir, name)
            if (candidate.isFile && candidate.canExecute()) return candidate.path
        }
        val pathEntries = System.getenv("PATH")
            ?.split(':')
            ?.filter { it.isNotBlank() }
            ?: return null
        for (dir in pathEntries) {
            val candidate = File(dir, name)
            if (candidate.isFile && candidate.canExecute()) return candidate.path
        }
        return null
    }

    /**
     * Builds the shell command line to type into a terminal for the given request.
     * Example: CLAUDE with dir "/a b" -> "CLAUDE_CONFIG_DIR='/a b' /opt/homebrew/bin/claude"
     */
    fun buildCommand(request: NewSessionRequest): String {
        val path = resolveBinary(request.provider) ?: request.provider.binary
        val bin = if (path.any { it.isWhitespace() }) shellQuote(path) else path
        return when (request.provider) {
            AiProvider.CLAUDE -> {
                val dir = request.claudeConfigDir
                if (!dir.isNullOrBlank()) "CLAUDE_CONFIG_DIR=${shellQuote(dir)} $bin" else bin
            }
            AiProvider.OLLAMA -> {
                val model = request.ollamaModel?.trim()?.takeIf { it.isNotEmpty() } ?: "llama3"
                "$bin run $model"
            }
            AiProvider.CODEX, AiProvider.GEMINI -> bin
        }
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
    private fun shellQuote(s: String): String = "'" + s.replace("'", "'\\''") + "'"
}

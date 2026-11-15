package dev.lessai.status

import com.intellij.openapi.application.PathManager
import java.nio.file.Files
import java.nio.file.Path

/** Locations of the per-session temp files used for status detection and prompt injection. */
object StatusFiles {

    /** Root temp directory of the plugin, e.g. `<system>/less-ai`. */
    val baseDir: Path
        get() = Path.of(PathManager.getSystemPath(), "less-ai")

    /** Directory holding status, hook and settings files, created on demand. */
    fun statusDir(): Path = Files.createDirectories(baseDir.resolve("status"))

    /** Directory holding generated system prompt files, created on demand. */
    fun promptDir(): Path = Files.createDirectories(baseDir.resolve("prompts"))

    /**
     * File the hooks / notify command write the current status word into.
     * Example: "abc" -> `<system>/less-ai/status/abc.status`
     */
    fun statusFile(sessionId: String): Path = statusDir().resolve("$sessionId.status")

    /**
     * Claude `--settings` JSON generated for the session.
     * Example: "abc" -> `<system>/less-ai/status/abc.settings.json`
     */
    fun claudeSettingsFile(sessionId: String): Path = statusDir().resolve("$sessionId.settings.json")

    /**
     * POSIX sh script invoked by every Claude hook of the session.
     * Example: "abc" -> `<system>/less-ai/status/abc.hook.sh`
     */
    fun hookScript(sessionId: String): Path = statusDir().resolve("$sessionId.hook.sh")

    /**
     * System prompt file for a session and provider.
     * Example: ("abc", "Claude") -> `<system>/less-ai/prompts/abc.Claude.txt`
     */
    fun promptFile(sessionId: String, agentName: String): Path = promptDir().resolve("$sessionId.$agentName.txt")

    /** Deletes every temp file that belongs to the session; failures are ignored. */
    fun cleanup(sessionId: String) {
        val dirs = listOf(baseDir.resolve("status"), baseDir.resolve("prompts"))
        for (dir in dirs) {
            if (!Files.isDirectory(dir)) continue
            try {
                Files.newDirectoryStream(dir, "$sessionId.*").use { stream ->
                    stream.forEach { Files.deleteIfExists(it) }
                }
            } catch (e: Exception) {
                // best effort
            }
        }
    }
}

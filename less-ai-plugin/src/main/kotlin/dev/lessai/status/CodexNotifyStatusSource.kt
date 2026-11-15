package dev.lessai.status

import dev.lessai.session.CommandBuilder
import dev.lessai.session.LaunchContext
import dev.lessai.session.SessionStatus
import java.nio.file.Files

/** Best-effort Codex turn-end signal: Codex's `notify` command rewrites the status file with "idle". */
class CodexNotifyStatusSource(sessionId: String) : StatusSource {

    private val watcher = StatusFileWatcher(StatusFiles.statusFile(sessionId))

    /** Starts watching the notify status file. */
    override fun start(sink: (SessionStatus) -> Unit) = watcher.start(sink)

    /** Stops watching. */
    override fun dispose() = watcher.dispose()

    companion object {

        /**
         * Returns the `-c notify=[...]` override that makes Codex write "idle" after every finished turn.
         * Example: "abc" -> extraArgs ["-c", "'notify=[\"sh\",\"-c\",\"printf idle > ...abc.status\"]'"]
         */
        fun prepareLaunch(sessionId: String): LaunchContext {
            val statusFile = StatusFiles.statusFile(sessionId)
            Files.deleteIfExists(statusFile)
            // Codex appends the JSON event as an extra argv entry; with `sh -c` it lands in $0 and is ignored.
            val script = "printf idle > " + CommandBuilder.shellQuote(statusFile.toString())
            val toml = "notify=[\"sh\",\"-c\"," + CommandBuilder.tomlString(script) + "]"
            return LaunchContext(extraArgs = listOf("-c", CommandBuilder.shellQuote(toml)))
        }
    }
}

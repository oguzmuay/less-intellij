package dev.lessai.status

import com.intellij.util.concurrency.AppExecutorUtil
import dev.lessai.session.SessionStatus
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** Polls a status file every 500 ms and reports its word whenever the file is rewritten. */
class StatusFileWatcher(private val file: Path) : StatusSource {

    private var future: ScheduledFuture<*>? = null
    private var lastStamp: Long = Long.MIN_VALUE
    private var lastContent: String? = null

    /** Starts polling; a pre-existing file is ignored until it changes. */
    override fun start(sink: (SessionStatus) -> Unit) {
        lastStamp = stamp()
        future = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay(
            { poll(sink) }, POLL_MS, POLL_MS, TimeUnit.MILLISECONDS
        )
    }

    /** Stops polling. */
    override fun dispose() {
        future?.cancel(false)
        future = null
    }

    /** Reads the file when its modification stamp or content changed and forwards the parsed status. */
    private fun poll(sink: (SessionStatus) -> Unit) {
        try {
            val stamp = stamp()
            if (stamp == Long.MIN_VALUE) return
            val content = Files.readString(file).trim()
            if (stamp == lastStamp && content == lastContent) return
            lastStamp = stamp
            lastContent = content
            parse(content)?.let(sink)
        } catch (e: Exception) {
            // file may be mid-rename; retry next tick
        }
    }

    /** Modification time in ms, or Long.MIN_VALUE when the file does not exist. */
    private fun stamp(): Long = try {
        if (Files.exists(file)) Files.getLastModifiedTime(file).toMillis() else Long.MIN_VALUE
    } catch (e: Exception) {
        Long.MIN_VALUE
    }

    companion object {
        private const val POLL_MS = 500L

        /**
         * Maps a status word written by a hook to a status.
         * Example: "working" -> WORKING, "approval" -> WAITING_APPROVAL, "x" -> null
         */
        fun parse(word: String): SessionStatus? = when (word.trim().lowercase()) {
            "working" -> SessionStatus.WORKING
            "idle" -> SessionStatus.IDLE
            "approval" -> SessionStatus.WAITING_APPROVAL
            else -> null
        }
    }
}

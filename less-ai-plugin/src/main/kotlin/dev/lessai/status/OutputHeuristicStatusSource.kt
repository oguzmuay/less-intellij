package dev.lessai.status

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import com.intellij.util.concurrency.AppExecutorUtil
import com.jediterm.terminal.model.TerminalModelListener
import com.jediterm.terminal.model.TerminalTextBuffer
import dev.lessai.session.SessionStatus
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Provider-agnostic status guess from terminal output: changing text => WORKING, quiet for
 * [QUIET_MS] => IDLE, an approval prompt near the bottom of the screen => WAITING_APPROVAL.
 */
class OutputHeuristicStatusSource(private val widget: TerminalWidget) : StatusSource {

    @Volatile
    private var dirty = true
    @Volatile
    private var disposed = false
    private var future: ScheduledFuture<*>? = null
    private var buffer: TerminalTextBuffer? = null
    private var modelListener: TerminalModelListener? = null

    private var seenOutput = false
    private var lastTail: String? = null
    private var lastChangeAt = 0L
    private var reported: SessionStatus? = null

    /** Hooks the JediTerm buffer for change notifications (if available) and starts the EDT ticker. */
    override fun start(sink: (SessionStatus) -> Unit) {
        attachModelListener()
        future = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
            ApplicationManager.getApplication().invokeLater({ if (!disposed) tick(sink) }, ModalityState.any())
        }, TICK_MS, TICK_MS, TimeUnit.MILLISECONDS)
    }

    /** Stops the ticker and detaches the buffer listener. */
    override fun dispose() {
        disposed = true
        future?.cancel(false)
        future = null
        val l = modelListener
        if (l != null) buffer?.removeModelListener(l)
        modelListener = null
        buffer = null
    }

    /** Registers a JediTerm model listener so text is only re-read after the buffer changed. */
    private fun attachModelListener() {
        try {
            val textBuffer = JBTerminalWidget.asJediTermWidget(widget)?.terminalTextBuffer ?: return
            val listener = TerminalModelListener { dirty = true }
            textBuffer.addModelListener(listener)
            buffer = textBuffer
            modelListener = listener
        } catch (e: Throwable) {
            // Not a JediTerm widget (new terminal engine): fall back to reading the text every tick.
            buffer = null
        }
    }

    /** Re-reads the screen tail when needed, derives a status and reports it if it changed. */
    private fun tick(sink: (SessionStatus) -> Unit) {
        val now = System.currentTimeMillis()
        if (buffer == null || dirty) {
            dirty = false
            val tail = try {
                tailLines(widget.getText())
            } catch (e: Throwable) {
                return
            }
            if (tail != lastTail) {
                if (lastTail != null || tail.isNotBlank()) seenOutput = true
                lastTail = tail
                lastChangeAt = now
            }
        }
        if (!seenOutput) return
        val status = when {
            isApprovalPrompt(lastTail.orEmpty()) -> SessionStatus.WAITING_APPROVAL
            now - lastChangeAt < QUIET_MS -> SessionStatus.WORKING
            else -> SessionStatus.IDLE
        }
        if (status != reported) {
            reported = status
            sink(status)
        }
    }

    companion object {
        private const val TICK_MS = 500L
        private const val QUIET_MS = 2500L
        private const val TAIL_CHARS = 6000
        private const val TAIL_LINES = 15

        /** Phrases that indicate the CLI is blocked on a yes/no or approval prompt. */
        private val APPROVAL_PATTERNS = listOf(
            Regex("""(?i)\(y/n\)"""),
            Regex("""(?i)\[y/n]"""),
            Regex("""(?i)do you want to (proceed|make this edit|create|run|allow)"""),
            Regex("""(?i)\ballow (this|execution|command|once|always)\b"""),
            Regex("""(?i)\bapprove\b"""),
            Regex("""(?i)apply this change\?"""),
            Regex("""(?i)press enter to confirm""")
        )

        /**
         * Returns the last non-blank lines of the terminal text, trailing whitespace removed.
         * Example: "a\n\nb  \n\n" -> "a\nb"
         */
        fun tailLines(text: CharSequence): String {
            val start = (text.length - TAIL_CHARS).coerceAtLeast(0)
            return text.subSequence(start, text.length).toString()
                .lines()
                .map { it.trimEnd() }
                .filter { it.isNotEmpty() }
                .takeLast(TAIL_LINES)
                .joinToString("\n")
        }

        /**
         * Whether the screen tail shows an approval / confirmation prompt.
         * Example: "Do you want to proceed?\n1. Yes" -> true, "> " -> false
         */
        fun isApprovalPrompt(tail: String): Boolean = APPROVAL_PATTERNS.any { it.containsMatchIn(tail) }
    }
}

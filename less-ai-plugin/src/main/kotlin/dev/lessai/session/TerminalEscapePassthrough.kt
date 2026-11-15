package dev.lessai.session

import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import java.awt.event.KeyEvent

/**
 * Sends a plain Escape key to the AI CLI instead of letting IntelliJ move focus to the editor.
 * The classic terminal panel only keeps Escape inside the "Terminal" tool window; anywhere else
 * (our "AI" panel) its TerminalEscapeKeyListener eats it, so Claude could never leave a subagent view.
 */
object TerminalEscapePassthrough {

    private val ESCAPE_BYTES = byteArrayOf(0x1b)

    /** Installs the pre-key handler on the widget's terminal panel; no-op for non-classic widgets. */
    fun install(widget: TerminalWidget) {
        val jediTerm = JBTerminalWidget.asJediTermWidget(widget) ?: return
        jediTerm.terminalPanel.addPreKeyEventHandler { e -> handle(jediTerm, e) }
    }

    /** Writes ESC to the TTY on a plain Escape press and consumes it (and its typed char) so the IDE never sees it. */
    private fun handle(jediTerm: JBTerminalWidget, e: KeyEvent) {
        if (e.isConsumed || !isPlainEscape(e)) return
        if (e.id == KeyEvent.KEY_PRESSED) {
            val starter = jediTerm.terminalStarter ?: return
            starter.sendBytes(ESCAPE_BYTES, true)
        }
        e.consume()
    }

    /**
     * Tells whether the event is an unmodified Escape key press or its typed character.
     * Example: Esc -> true; Shift+Esc -> false; typed '\u001b' -> true
     */
    private fun isPlainEscape(e: KeyEvent): Boolean = when (e.id) {
        KeyEvent.KEY_PRESSED -> e.keyCode == KeyEvent.VK_ESCAPE && e.modifiersEx == 0
        KeyEvent.KEY_TYPED -> e.keyChar == '\u001b' && e.modifiersEx == 0
        else -> false
    }
}

package dev.lessai.session

import com.intellij.terminal.JBTerminalWidget
import com.intellij.terminal.ui.TerminalWidget
import java.awt.event.InputEvent
import java.awt.event.KeyEvent

/**
 * Lets Cmd+Shift+Left/Right switch AI sessions while a session terminal has focus.
 * With "Override IDE shortcuts" on, the classic terminal keeps every keystroke not on its own whitelist,
 * so the LessAi.*ToolWindowTab actions never fire from inside the terminal.
 */
object TerminalSessionSwitchKeys {

    private const val MODIFIERS = InputEvent.META_DOWN_MASK or InputEvent.SHIFT_DOWN_MASK

    /** Installs the pre-key handler on the widget's terminal panel; no-op for non-classic widgets. */
    fun install(widget: TerminalWidget, manager: SessionManager) {
        val jediTerm = JBTerminalWidget.asJediTermWidget(widget) ?: return
        jediTerm.terminalPanel.addPreKeyEventHandler { e ->
            if (e.isConsumed || e.id != KeyEvent.KEY_PRESSED || e.modifiersEx != MODIFIERS) return@addPreKeyEventHandler
            when (e.keyCode) {
                KeyEvent.VK_LEFT -> manager.selectPrevious()
                KeyEvent.VK_RIGHT -> manager.selectNext()
                else -> return@addPreKeyEventHandler
            }
            e.consume()
            manager.selected?.widget?.preferredFocusableComponent?.requestFocusInWindow()
        }
    }
}

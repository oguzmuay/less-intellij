package dev.lessai.session

import com.intellij.openapi.Disposable
import com.intellij.terminal.ui.TerminalWidget

/** One AI CLI session running inside its own embedded terminal widget. */
class AiSession(
    val id: String,
    name: String,
    val provider: AiProvider,
    val widget: TerminalWidget,
    val command: String,
    /** Parent disposable of the widget; disposing it tears the terminal down. */
    internal val disposable: Disposable
) {
    /** Display name; change it via [SessionManager.rename] so listeners are notified. */
    var name: String = name
        internal set

    /** Current status; change it via [SessionManager.setStatus] so listeners are notified. */
    var status: SessionStatus = SessionStatus.STARTING
        internal set

    /** Returns the display name, used by default list rendering and debugging. */
    override fun toString(): String = "$name (${provider.displayName}, ${status.label})"
}

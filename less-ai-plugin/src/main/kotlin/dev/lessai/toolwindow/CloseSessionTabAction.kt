package dev.lessai.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import dev.lessai.session.AiSession
import dev.lessai.session.SessionManager

/** The "x" button on a session tab; closes that tab's session (not necessarily the selected one). */
class CloseSessionTabAction(
    private val manager: SessionManager,
    private val session: AiSession,
) : DumbAwareAction("Close Session", "Close this AI session", AllIcons.Actions.Close) {

    init {
        templatePresentation.hoveredIcon = AllIcons.Actions.CloseHovered
    }

    /** Closes the session this button belongs to. */
    override fun actionPerformed(e: AnActionEvent) = manager.close(session)

    /** The action only touches Swing state, so it updates on the EDT. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT
}

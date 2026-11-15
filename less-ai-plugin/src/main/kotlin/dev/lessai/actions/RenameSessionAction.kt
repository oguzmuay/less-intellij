package dev.lessai.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import dev.lessai.dialog.NewSessionDialog
import dev.lessai.session.SessionManager

/** Renames the currently selected AI session. */
class RenameSessionAction : AnAction(), DumbAware {

    /** Runs the update on a background thread. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /** Enables the action only when a session is selected. */
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && (SessionManager.getInstance(project).selected != null)
    }

    /** Performs the action. */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val manager = SessionManager.getInstance(project)
        val session = manager.selected ?: return
        val name = Messages.showInputDialog(project, "Session name:", "Rename AI Session", null, session.name, null)
        if (!name.isNullOrBlank()) manager.rename(session, name.trim())
    }
}

package dev.lessai.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import dev.lessai.dialog.NewSessionDialog
import dev.lessai.session.SessionManager

/** Selects the next AI session. */
class NextSessionAction : AnAction(), DumbAware {

    /** Runs the update on a background thread. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /** Enables the action only when a project is open. */
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && (true)
    }

    /** Performs the action. */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val manager = SessionManager.getInstance(project)
        manager.selectNext()
    }
}

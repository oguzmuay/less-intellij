package dev.lessai.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/**
 * Switches tabs of the focused tool window; disabled elsewhere so editor shortcuts on the same keys still work.
 * Example: Cmd+Shift+Right with the Terminal focused -> next terminal tab; in the editor -> not handled here
 */
abstract class ToolWindowTabAction(private val delta: Int) : AnAction(), DumbAware {

    /** Focus and tool window state must be read on the UI thread. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    /** Enabled only while a tool window with several tabs has focus. */
    override fun update(e: AnActionEvent) {
        val project = e.project
        val toolWindow = project?.let { ToolWindowTabs.focused(it) }
        e.presentation.isEnabledAndVisible = project != null && toolWindow != null && ToolWindowTabs.canSwitch(project, toolWindow)
    }

    /** Moves to the neighbouring tab of the focused tool window. */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowTabs.focused(project) ?: return
        ToolWindowTabs.move(project, toolWindow, delta)
    }
}

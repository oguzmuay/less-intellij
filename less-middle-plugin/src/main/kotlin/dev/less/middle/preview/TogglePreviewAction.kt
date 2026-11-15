package dev.less.middle.preview

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAware

/** Cmd+P in LESS: toggles the current file between its text editor and its preview. */
class TogglePreviewAction : AnAction(), DumbAware {

    /** Editor state is read on the EDT. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

    /** Enabled only when the focused/selected editor has a preview. */
    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && PreviewToggle.isAvailable(project, e.getData(PlatformDataKeys.FILE_EDITOR))
    }

    /** Switches text <-> preview. */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        PreviewToggle.toggle(project, e.getData(PlatformDataKeys.FILE_EDITOR))
    }
}

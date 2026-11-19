package dev.less.middle.vim

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/** g? in Vim Normal mode: opens the Vim cheat-sheet popup. */
class OpenVimCheatSheetAction : AnAction(), DumbAware {

    /** Nothing to compute; keep the update on the background thread. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /** Shows the cheat sheet. */
    override fun actionPerformed(e: AnActionEvent) {
        VimCheatSheetDialog(e.project).show()
    }
}

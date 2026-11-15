package dev.less.middle.notes

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware

/** Cmd+N in Vim Normal/Visual mode: opens the centered notes modal. */
class OpenNotesAction : AnAction(), DumbAware {

    /** Nothing to compute; keep the update on the background thread. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    /** Shows the notes modal. */
    override fun actionPerformed(e: AnActionEvent) {
        NotesDialog(e.project).show()
    }
}

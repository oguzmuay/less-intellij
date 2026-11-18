package dev.less.monorepo

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware

/** Tools menu action that forces a fresh module scan, e.g. after a large checkout. */
class RescanModulesAction : AnAction(), DumbAware {
    /** Starts a rescan of the current project. */
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.service<MonorepoService>()?.scheduleRescan()
    }

    /** Enabled only when a project is open. */
    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = e.project != null
    }

    /** update() only reads the project, so it can run off the EDT. */
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT
}

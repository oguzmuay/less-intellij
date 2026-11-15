package dev.lessai.actions

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowManager
import dev.lessai.session.SessionManager

/** Helpers that switch tabs of whichever tool window currently has focus. */
object ToolWindowTabs {
    private const val AI_TOOL_WINDOW_ID = "AI"

    /** Returns the tool window that currently has keyboard focus, or null when focus is elsewhere (e.g. the editor). */
    fun focused(project: Project): ToolWindow? {
        val manager = ToolWindowManager.getInstance(project)
        return manager.toolWindowIds.mapNotNull { manager.getToolWindow(it) }.firstOrNull { it.isVisible && it.isActive }
    }

    /**
     * Tells whether the tool window has more than one tab to switch between.
     * Example: Terminal with 2 tabs -> true; Project view -> false; AI panel with 1 session -> false
     */
    fun canSwitch(project: Project, toolWindow: ToolWindow): Boolean =
        if (toolWindow.id == AI_TOOL_WINDOW_ID) SessionManager.getInstance(project).sessions.size > 1
        else (toolWindow.contentManagerIfCreated?.contentCount ?: 0) > 1

    /**
     * Selects the neighbouring tab with wrap-around; in the AI panel the tabs are the AI sessions.
     * Example: delta +1 on the last Terminal tab -> the first tab
     */
    fun move(project: Project, toolWindow: ToolWindow, delta: Int) {
        if (toolWindow.id == AI_TOOL_WINDOW_ID) {
            val sessions = SessionManager.getInstance(project)
            if (delta > 0) sessions.selectNext() else sessions.selectPrevious()
            return
        }
        val manager = toolWindow.contentManagerIfCreated ?: return
        val count = manager.contentCount
        if (count < 2) return
        val current = manager.selectedContent?.let { manager.getIndexOfContent(it) } ?: 0
        manager.setSelectedContent(manager.getContent(Math.floorMod(current + delta, count))!!, true)
    }
}

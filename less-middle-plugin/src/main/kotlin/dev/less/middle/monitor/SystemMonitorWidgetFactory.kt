package dev.less.middle.monitor

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory

/** Creates the system monitor widget for each project window's status bar. */
class SystemMonitorWidgetFactory : StatusBarWidgetFactory {
    /** Widget id shared with [SystemMonitorWidget]. */
    override fun getId(): String = ID

    /** Name shown in the status bar widget context menu. */
    override fun getDisplayName(): String = "LESS System Monitor"

    /** The widget is available in every project. */
    override fun isAvailable(project: Project): Boolean = true

    /** Builds a new widget instance. */
    override fun createWidget(project: Project): StatusBarWidget = SystemMonitorWidget(project)

    companion object {
        const val ID = "dev.less.middle.SystemMonitor"
    }
}

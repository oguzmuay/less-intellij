package dev.less.middle.monitor

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/** Status bar widget that shows RAM, CPU, disk and IP, refreshed every two seconds. */
class SystemMonitorWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.TextPresentation {
    @Volatile private var snapshot: SystemSnapshot? = null
    private var statusBar: StatusBar? = null
    private var task: ScheduledFuture<*>? = null

    /** Returns the widget id used by the factory and for updates. */
    override fun ID(): String = SystemMonitorWidgetFactory.ID

    /** Starts the background refresh once the widget is attached to a status bar. */
    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
        task = AppExecutorUtil.getAppScheduledExecutorService().scheduleWithFixedDelay({
            snapshot = SystemStats.read()
            statusBar.updateWidget(ID())
        }, 0, 2, TimeUnit.SECONDS)
    }

    /** Stops the background refresh. */
    override fun dispose() {
        task?.cancel(false)
        task = null
        statusBar = null
    }

    /** This widget is its own text presentation. */
    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    /** Current status line, or a placeholder before the first reading. */
    override fun getText(): String = snapshot?.let { SystemMonitorFormat.text(it) } ?: "RAM …  CPU …  Disk …"

    /** Left-aligned text inside the widget. */
    override fun getAlignment(): Float = 0f

    /** Tooltip with the leak explanation when relevant. */
    override fun getTooltipText(): String? =
        if (snapshot?.ipLeaked == true) "A VPN tunnel is up but the physical interface still exposes this IP" else "System monitor"

    /** No click action. */
    override fun getClickConsumer(): com.intellij.util.Consumer<java.awt.event.MouseEvent>? = null
}

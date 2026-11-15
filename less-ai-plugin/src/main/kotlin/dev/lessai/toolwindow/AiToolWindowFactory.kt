package dev.lessai.toolwindow

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions

/** Spike: embeds one terminal widget directly in the AI tool window. */
class AiToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val runner = LocalTerminalDirectRunner.createTerminalRunner(project)
        val options = ShellStartupOptions.Builder().workingDirectory(project.basePath).build()
        val widget = runner.startShellTerminalWidget(toolWindow.disposable, options, true)
        val content = ContentFactory.getInstance().createContent(widget.component, "Spike", false)
        toolWindow.contentManager.addContent(content)
    }
}

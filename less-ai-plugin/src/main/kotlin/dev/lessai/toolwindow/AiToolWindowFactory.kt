package dev.lessai.toolwindow

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.content.ContentFactory
import dev.lessai.session.SessionManager

/** Builds the AI tool window: session list on top, selected session's terminal below. */
class AiToolWindowFactory : ToolWindowFactory, DumbAware {

    /** Creates the splitter layout and wires it to the project's [SessionManager]; never auto-starts a session. */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val manager = SessionManager.getInstance(project)
        val parent = toolWindow.disposable
        val splitter = OnePixelSplitter(true, SPLIT_PROPORTION_KEY, 0.3f).apply {
            firstComponent = SessionListPanel(manager, parent)
            secondComponent = SessionContentPanel(manager, parent)
        }
        val content = ContentFactory.getInstance().createContent(splitter, null, false)
        toolWindow.contentManager.addContent(content)

        (ActionManager.getInstance().getAction(TOOLBAR_GROUP_ID) as? ActionGroup)?.let { group ->
            toolWindow.setTitleActions(listOf(group))
        }
    }

    private companion object {
        const val TOOLBAR_GROUP_ID = "LessAi.Toolbar"
        const val SPLIT_PROPORTION_KEY = "LessAi.SessionsSplitter"
    }
}

package dev.lessai.toolwindow

import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.content.ContentFactory
import dev.lessai.inbox.InboxNotifier
import dev.lessai.inbox.MailboxListener
import dev.lessai.inbox.MailboxService
import dev.lessai.session.AiProvider
import dev.lessai.session.SessionManager

/** Builds the AI tool window: a "Sessions" tab (list + terminal) and an "Inbox" tab. */
class AiToolWindowFactory : ToolWindowFactory, DumbAware {

    /** Creates both tabs, keeps the Inbox tab title in sync with unread mail and starts the notifier. */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val manager = SessionManager.getInstance(project)
        val parent = toolWindow.disposable
        val factory = ContentFactory.getInstance()
        val splitter = OnePixelSplitter(true, SPLIT_PROPORTION_KEY, 0.3f).apply {
            firstComponent = SessionListPanel(manager, parent)
            secondComponent = SessionContentPanel(manager, parent)
        }
        val sessions = factory.createContent(splitter, "Sessions", false).apply { isCloseable = false }
        toolWindow.contentManager.addContent(sessions)

        val mailbox = MailboxService.getInstance(project)
        val inbox = factory.createContent(InboxPanel(project, parent), INBOX_TITLE, false).apply { isCloseable = false }
        toolWindow.contentManager.addContent(inbox)
        val updateTitle = {
            val unread = AiProvider.entries.sumOf { mailbox.unreadCount(it.displayName) }
            inbox.displayName = if (unread > 0) "$INBOX_TITLE ($unread)" else INBOX_TITLE
        }
        updateTitle()
        mailbox.addListener(object : MailboxListener {
            override fun mailChanged(agentName: String) = updateTitle()
        }, parent)
        InboxNotifier(project).start(parent)

        (ActionManager.getInstance().getAction(TOOLBAR_GROUP_ID) as? ActionGroup)?.let { group ->
            toolWindow.setTitleActions(listOf(group))
        }
    }

    private companion object {
        const val TOOLBAR_GROUP_ID = "LessAi.Toolbar"
        const val SPLIT_PROPORTION_KEY = "LessAi.SessionsSplitter"
        const val INBOX_TITLE = "Inbox"
    }
}

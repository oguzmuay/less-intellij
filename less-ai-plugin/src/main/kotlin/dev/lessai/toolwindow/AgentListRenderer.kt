package dev.lessai.toolwindow

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import dev.lessai.inbox.MailboxService
import javax.swing.JList

/** Renders an agent name with its unread count badge in the inbox agent list. */
class AgentListRenderer(private val mailbox: MailboxService) : ColoredListCellRenderer<String>() {

    /** Appends the agent name and, when there is unread mail, a bold "(n)" badge. */
    override fun customizeCellRenderer(list: JList<out String>, value: String?, index: Int, selected: Boolean, hasFocus: Boolean) {
        val name = value ?: return
        append(name)
        val unread = mailbox.unreadCount(name)
        if (unread > 0) append("  ($unread)", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
    }
}

package dev.lessai.toolwindow

import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.ColorIcon
import com.intellij.util.ui.JBUI
import dev.lessai.session.AiSession
import dev.lessai.session.SessionStatus
import java.awt.Color
import javax.swing.JList

/** Renders a session row as: status dot, bold name, grey provider and status label. */
class SessionCellRenderer : ColoredListCellRenderer<AiSession>() {

    /** Fills the row's icon and text fragments for the given session. */
    override fun customizeCellRenderer(
        list: JList<out AiSession>,
        value: AiSession?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean
    ) {
        if (value == null) return
        icon = ColorIcon(JBUI.scale(DOT_SIZE), statusColor(value.status))
        iconTextGap = JBUI.scale(6)
        append(value.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
        append("  ${value.provider.displayName} · ${value.status.label}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    }

    /** Maps a status to its badge colour (amber starting, blue working, green idle, red waiting). */
    private fun statusColor(status: SessionStatus): Color = when (status) {
        SessionStatus.STARTING -> JBColor(Color(0xE0A100), Color(0xD9A400))
        SessionStatus.WORKING -> JBColor(Color(0x2F7FE0), Color(0x4A90E2))
        SessionStatus.IDLE -> JBColor(Color(0x3C9B3C), Color(0x5FB85F))
        SessionStatus.WAITING_APPROVAL -> JBColor(Color(0xD0342C), Color(0xE5534B))
    }

    private companion object {
        const val DOT_SIZE = 8
    }
}

package dev.lessai.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import dev.lessai.session.SessionListener
import dev.lessai.session.SessionManager
import java.awt.CardLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

/** Bottom part of the AI tool window: shows the selected session's terminal, or an empty hint. */
class SessionContentPanel(private val manager: SessionManager, parent: Disposable) : JPanel(CardLayout()), SessionListener {

    private val cards = mutableMapOf<String, JComponent>()

    init {
        val empty = JBLabel("No AI session. Press Cmd+T to start one.", SwingConstants.CENTER)
        empty.foreground = UIUtil.getContextHelpForeground()
        add(empty, EMPTY_CARD)
        manager.addListener(this, parent)
        sessionsChanged()
    }

    /** Adds cards for new sessions and removes cards of closed ones. */
    override fun sessionsChanged() {
        val current = manager.sessions.associateBy { it.id }
        cards.keys.filter { it !in current }.forEach { id -> cards.remove(id)?.let { remove(it) } }
        current.values.filter { it.id !in cards }.forEach { session ->
            val component = session.widget.component
            cards[session.id] = component
            add(component, session.id)
        }
        selectionChanged()
        revalidate()
        repaint()
    }

    /** Brings the selected session's terminal to the front. */
    override fun selectionChanged() {
        val id = manager.selected?.id?.takeIf { it in cards } ?: EMPTY_CARD
        (layout as CardLayout).show(this, id)
    }

    private companion object {
        const val EMPTY_CARD = "lessai.empty"
    }
}

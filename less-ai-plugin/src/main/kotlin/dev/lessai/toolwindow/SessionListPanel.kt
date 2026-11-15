package dev.lessai.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBList
import com.intellij.util.ui.JBUI
import dev.lessai.session.AiSession
import dev.lessai.session.SessionListener
import dev.lessai.session.SessionManager
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

/** Top part of the AI tool window: the list of sessions kept in sync with [SessionManager]. */
class SessionListPanel(private val manager: SessionManager, parent: Disposable) : JPanel(BorderLayout()), SessionListener {

    private val model = CollectionListModel<AiSession>()
    private val list = JBList(model)
    private var syncing = false

    init {
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = SessionCellRenderer()
        list.emptyText.text = "No AI sessions"
        list.border = JBUI.Borders.empty(2)
        list.addListSelectionListener { e ->
            if (!e.valueIsAdjusting && !syncing) list.selectedValue?.let { manager.selected = it }
        }
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) activateSelected()
            }
        })
        list.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER) {
                    activateSelected()
                    e.consume()
                }
            }
        })
        add(ScrollPaneFactory.createScrollPane(list, true), BorderLayout.CENTER)
        manager.addListener(this, parent)
        sessionsChanged()
    }

    /** Rebuilds the list from the manager's sessions. */
    override fun sessionsChanged() {
        withoutFeedback { model.replaceAll(manager.sessions) }
        selectionChanged()
    }

    /** Mirrors the manager's selection in the list. */
    override fun selectionChanged() {
        withoutFeedback {
            val selected = manager.selected
            if (selected == null) list.clearSelection() else list.setSelectedValue(selected, true)
        }
    }

    /** Repaints the row of a renamed or re-statused session. */
    override fun sessionUpdated(session: AiSession) {
        val index = model.getElementIndex(session)
        if (index >= 0) model.contentsChanged(session)
    }

    /** Selects the highlighted session and moves keyboard focus into its terminal. */
    private fun activateSelected() {
        val session = list.selectedValue ?: return
        manager.selected = session
        session.widget.preferredFocusableComponent.requestFocusInWindow()
    }

    /** Runs [action] while suppressing list-to-manager selection feedback. */
    private fun withoutFeedback(action: () -> Unit) {
        syncing = true
        try {
            action()
        } finally {
            syncing = false
        }
    }
}

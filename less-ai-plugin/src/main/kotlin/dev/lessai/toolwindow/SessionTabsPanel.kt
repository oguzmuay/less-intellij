package dev.lessai.toolwindow

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.tabs.JBTabsFactory
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import dev.lessai.session.AiSession
import dev.lessai.session.SessionListener
import dev.lessai.session.SessionManager
import java.awt.BorderLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * Sessions as editor-style tabs: one tab per session with a status dot, its name and a close button;
 * the tab's content is the session's terminal. Kept in sync with [SessionManager] in both directions.
 */
class SessionTabsPanel(
    private val project: Project,
    private val manager: SessionManager,
    parent: Disposable,
) : JPanel(BorderLayout()), SessionListener {

    private val tabs = JBTabsFactory.createEditorTabs(project, parent)
    private val infos = mutableMapOf<String, TabInfo>()
    private var syncing = false

    init {
        tabs.presentation
            .setTabDraggingEnabled(false)
            .setEmptyText("No AI session. Press Cmd+T to start one.")
        tabs.addListener(object : TabsListener {
            /** A tab picked by the user selects its session and focuses its terminal. */
            override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
                if (syncing) return
                val session = newSelection?.`object` as? AiSession ?: return
                manager.selected = session
                session.widget.preferredFocusableComponent.requestFocusInWindow()
            }
        }, parent)
        tabs.addTabMouseListener(object : MouseAdapter() {
            /** Middle click closes the tab, like in the editor. */
            override fun mouseReleased(e: MouseEvent) {
                if (SwingUtilities.isMiddleMouseButton(e)) sessionAt(e)?.let(manager::close)
            }

            /** Double click renames the session. */
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2) sessionAt(e)?.let(::rename)
            }
        })
        add(tabs.component, BorderLayout.CENTER)
        manager.addListener(this, parent)
        sessionsChanged()
    }

    /** Adds tabs for new sessions (at the end, matching the manager's order) and removes tabs of closed ones. */
    override fun sessionsChanged() {
        val current = manager.sessions.associateBy { it.id }
        withoutFeedback {
            infos.keys.filter { it !in current }.forEach { id -> infos.remove(id)?.let { tabs.removeTab(it) } }
            manager.sessions.filter { it.id !in infos }.forEach { session ->
                val info = TabInfo(session.widget.component)
                    .setObject(session)
                    .setTabLabelActions(DefaultActionGroup(CloseSessionTabAction(manager, session)), TAB_PLACE)
                present(info, session)
                infos[session.id] = info
                tabs.addTab(info)
            }
        }
        selectionChanged()
    }

    /** Shows the manager's selected session as the selected tab. */
    override fun selectionChanged() {
        val info = manager.selected?.let { infos[it.id] } ?: return
        if (tabs.selectedInfo === info) return
        withoutFeedback { tabs.select(info, false) }
    }

    /** Refreshes the tab of a renamed or re-statused session. */
    override fun sessionUpdated(session: AiSession) {
        infos[session.id]?.let { present(it, session) }
    }

    /** Sets the tab's status dot, name and tooltip from the session. */
    private fun present(info: TabInfo, session: AiSession) {
        info.setText(session.name)
            .setIcon(StatusDotIcon(SessionStatusColors.of(session.status)))
            .setTooltipText("${session.provider.displayName} · ${session.status.label}")
    }

    /** Session of the tab under the mouse, or null. */
    private fun sessionAt(e: MouseEvent): AiSession? = tabs.findInfo(e)?.`object` as? AiSession

    /** Asks for a new name for [session]. */
    private fun rename(session: AiSession) {
        val name = Messages.showInputDialog(project, "Session name:", "Rename AI Session", null, session.name, null)
        if (!name.isNullOrBlank()) manager.rename(session, name)
    }

    /** Runs [action] without echoing tab selection changes back to the manager. */
    private fun withoutFeedback(action: () -> Unit) {
        syncing = true
        try {
            action()
        } finally {
            syncing = false
        }
    }

    private companion object {
        const val TAB_PLACE = "LessAi.SessionTab"
    }
}

package dev.lessai.toolwindow

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.ui.JBSplitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextArea
import com.intellij.util.ui.JBUI
import dev.lessai.inbox.MailboxListener
import dev.lessai.inbox.MailboxService
import dev.lessai.session.AiProvider
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.HierarchyEvent
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.Box
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel
import javax.swing.SwingUtilities

/** Inbox tab: agent list with unread badges, selected agent's messages and a send box. */
class InboxPanel(private val project: Project, parent: Disposable) : JPanel(BorderLayout()), MailboxListener {

    private val mailbox = MailboxService.getInstance(project)
    private val agents = AiProvider.entries.map { it.displayName }
    private val listModel = CollectionListModel(agents)
    private val agentList = JBList(listModel)
    private val messageView = JBTextArea().apply { isEditable = false; lineWrap = true; wrapStyleWord = true }
    private val toCombo = JComboBox(agents.toTypedArray())
    private val input = JBTextArea(3, 20).apply { lineWrap = true; wrapStyleWord = true }
    private var syncing = false

    init {
        mailbox.ensureCreated()
        agentList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        agentList.cellRenderer = AgentListRenderer(mailbox)
        agentList.addListSelectionListener { e ->
            if (!e.valueIsAdjusting && !syncing) agentList.selectedValue?.let { onAgentSelected(it) }
        }
        syncing = true
        agentList.selectedIndex = 0
        syncing = false
        showMessages(agents.first())

        val splitter = JBSplitter(false, SPLIT_KEY, 0.25f).apply {
            firstComponent = ScrollPaneFactory.createScrollPane(agentList)
            secondComponent = ScrollPaneFactory.createScrollPane(messageView)
        }
        add(splitter, BorderLayout.CENTER)
        add(buildSendBox(), BorderLayout.SOUTH)

        mailbox.addListener(this, parent)
        addHierarchyListener { e ->
            if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L && isShowing) refresh()
        }
    }

    /** Refreshes badges and the open conversation when an inbox changed. */
    override fun mailChanged(agentName: String) = refresh()

    /** Builds the bottom area: recipient combo, message field, send button and shared-folder link. */
    private fun buildSendBox(): JPanel {
        val send = JButton("Send").apply { addActionListener { sendMessage() } }
        val sendAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) = sendMessage()
        }
        listOf("meta ENTER", "control ENTER").forEach {
            input.inputMap.put(KeyStroke.getKeyStroke(it), "sendMail")
        }
        input.actionMap.put("sendMail", sendAction)

        val bar = JPanel().apply {
            layout = javax.swing.BoxLayout(this, javax.swing.BoxLayout.X_AXIS)
            add(JLabel("To "))
            add(toCombo)
            add(Box.createHorizontalStrut(8))
            add(ActionLink("Open shared folder") { openShared() })
            add(Box.createHorizontalGlue())
            add(send)
        }
        return JPanel(BorderLayout(0, 4)).apply {
            border = JBUI.Borders.empty(6)
            add(bar, BorderLayout.NORTH)
            add(ScrollPaneFactory.createScrollPane(input), BorderLayout.CENTER)
        }
    }

    /** Sends the typed text as "You" to the chosen agent and clears the field. */
    private fun sendMessage() {
        val text = input.text.trim()
        val to = toCombo.selectedItem as? String ?: return
        if (text.isEmpty()) return
        mailbox.send(SENDER, to, text)
        input.text = ""
    }

    /** Reveals the mailbox `shared/` folder in the file manager. */
    private fun openShared() {
        val shared = mailbox.dir.resolve("shared")
        java.nio.file.Files.createDirectories(shared)
        RevealFileAction.openDirectory(shared)
    }

    /** Shows the agent's messages and marks them read. */
    private fun onAgentSelected(agent: String) {
        showMessages(agent)
        mailbox.markRead(agent)
    }

    /** Re-renders badges and the selected conversation; marks it read while the tab is visible. */
    private fun refresh() {
        SwingUtilities.invokeLater {
            val selected = agentList.selectedValue
            syncing = true
            listModel.replaceAll(agents)
            agentList.setSelectedValue(selected ?: agents.first(), false)
            syncing = false
            val agent = agentList.selectedValue ?: return@invokeLater
            showMessages(agent)
            if (isShowing) mailbox.markRead(agent)
        }
    }

    /** Renders "from X · time" headers and bodies, newest at the bottom. */
    private fun showMessages(agent: String) {
        val text = mailbox.messages(agent).joinToString("\n\n") { "from ${it.from} · ${it.time}\n${it.body}" }
        messageView.text = text
        messageView.caretPosition = messageView.document.length
    }

    private companion object {
        const val SENDER = "You"
        const val SPLIT_KEY = "LessAi.InboxSplitter"
    }
}

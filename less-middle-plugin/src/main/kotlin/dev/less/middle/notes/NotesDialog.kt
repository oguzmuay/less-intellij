package dev.less.middle.notes

import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import java.awt.Font
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.SwingConstants
import javax.swing.event.DocumentEvent

/**
 * Centered modal with one tab per note; each note has free text on the left and a checklist on the right.
 * Cmd+Shift+Left/Right switch notes, Cmd+T adds one, Cmd+W deletes the current one, double-click or Cmd+R / Cmd+Shift+R
 * renames, Cmd+L jumps between the text and the checklist, Esc or Cmd+N closes. Edits are saved as you type.
 */
class NotesDialog(project: Project?) : DialogWrapper(project, true, IdeModalityType.IDE) {

    private val state = NotesStorage.getInstance().state
    private val tabs = JBTabbedPane(SwingConstants.TOP)
    private val areas = mutableListOf<JBTextArea>()
    private val checklists = mutableListOf<ChecklistPanel>()

    init {
        title = "Notes"
        if (state.notes.isEmpty()) state.notes.add(Note(nextTitle()))
        state.notes.forEach { addTab(it) }
        selectTab(state.selectedIndex)
        tabs.addChangeListener { state.selectedIndex = tabs.selectedIndex.coerceAtLeast(0) }
        tabs.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && tabs.indexAtLocation(e.x, e.y) >= 0) renameCurrent()
            }
        })
        init()
    }

    /** The tabbed notes area, sized like a large centered sheet. */
    override fun createCenterPanel(): JComponent {
        tabs.preferredSize = JBUI.size(900, 560)
        registerShortcuts()
        return tabs
    }

    /** No OK/Cancel row; the modal closes with Esc or Cmd+N. */
    override fun createSouthPanel(): JComponent? = null

    /** Focus goes straight into the selected note's text. */
    override fun getPreferredFocusedComponent(): JComponent? = areas.getOrNull(tabs.selectedIndex)

    /** Remembers the dialog size and position between openings. */
    override fun getDimensionServiceKey(): String = "LessMiddle.NotesDialog"

    /** Binds the in-modal shortcuts on the root pane so they win over the text area's own keys. */
    private fun registerShortcuts() {
        val meta = InputEvent.META_DOWN_MASK
        val metaShift = meta or InputEvent.SHIFT_DOWN_MASK
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, metaShift)) { selectTab(tabs.selectedIndex + 1) }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, metaShift)) { selectTab(tabs.selectedIndex - 1) }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_T, meta)) { addNote() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_W, meta)) { deleteCurrent() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_N, meta)) { close(CANCEL_EXIT_CODE) }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_R, meta)) { renameCurrent() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_R, metaShift)) { renameCurrent() }
        bind(KeyStroke.getKeyStroke(KeyEvent.VK_L, meta)) { toggleChecklistFocus() }
    }

    /** Registers [action] for [stroke] on the whole dialog for its lifetime. */
    private fun bind(stroke: KeyStroke, action: () -> Unit) {
        DumbAwareAction.create { action() }.registerCustomShortcutSet(CustomShortcutSet(stroke), rootPane, disposable)
    }

    /** Appends a tab (text area + checklist side by side) whose edits are written back into [note]. */
    private fun addTab(note: Note) {
        val area = JBTextArea(note.text).apply {
            lineWrap = true
            wrapStyleWord = true
            border = JBUI.Borders.empty(8)
            font = editorFont()
            document.addDocumentListener(object : DocumentAdapter() {
                override fun textChanged(e: DocumentEvent) {
                    note.text = text
                }
            })
        }
        val checklist = ChecklistPanel(note)
        areas.add(area)
        checklists.add(checklist)
        val split = OnePixelSplitter(false, SPLIT_PROPORTION_KEY, 0.62f).apply {
            firstComponent = JBScrollPane(area).apply { border = JBUI.Borders.empty() }
            secondComponent = checklist
        }
        tabs.addTab(note.title, split)
    }

    /**
     * Selects the tab at [index] with wrap-around and focuses its text.
     * Example: 3 tabs, index 3 -> tab 0; index -1 -> tab 2
     */
    private fun selectTab(index: Int) {
        if (tabs.tabCount == 0) return
        val target = Math.floorMod(index, tabs.tabCount)
        tabs.selectedIndex = target
        areas[target].requestFocusInWindow()
    }

    /** Creates an empty note after the existing ones and switches to it. */
    private fun addNote() {
        val note = Note(nextTitle())
        state.notes.add(note)
        addTab(note)
        selectTab(tabs.tabCount - 1)
    }

    /** Moves focus from the note text to the checklist's "Add item" field, or back. */
    private fun toggleChecklistFocus() {
        val index = tabs.selectedIndex.takeIf { it >= 0 } ?: return
        if (checklists[index].hasFocusInside()) areas[index].requestFocusInWindow() else checklists[index].focusInput()
    }

    /** Deletes the current note, asking first when it has text or items; the last note is only cleared. */
    private fun deleteCurrent() {
        val index = tabs.selectedIndex.takeIf { it >= 0 } ?: return
        val note = state.notes[index]
        if ((note.text.isNotBlank() || note.items.isNotEmpty()) && Messages.showYesNoDialog(
                tabs, "Delete note \"${note.title}\"?", "Delete Note", Messages.getQuestionIcon()
            ) != Messages.YES
        ) return
        if (tabs.tabCount == 1) {
            areas[0].text = ""
            checklists[0].clear()
            return
        }
        state.notes.removeAt(index)
        areas.removeAt(index)
        checklists.removeAt(index)
        tabs.removeTabAt(index)
        selectTab(index.coerceAtMost(tabs.tabCount - 1))
    }

    /** Asks for a new title for the current note; blank input is ignored. */
    private fun renameCurrent() {
        val index = tabs.selectedIndex.takeIf { it >= 0 } ?: return
        val note = state.notes[index]
        val name = Messages.showInputDialog(tabs, "Note title:", "Rename Note", null, note.title, null)
            ?.trim()?.takeIf { it.isNotEmpty() } ?: return
        note.title = name
        tabs.setTitleAt(index, name)
        areas[index].requestFocusInWindow()
    }

    /**
     * Returns the first free "Note N" title.
     * Example: existing ["Note 1", "Ideas"] -> "Note 2"
     */
    private fun nextTitle(): String {
        val used = state.notes.map { it.title }.toSet()
        return generateSequence(1) { it + 1 }.map { "Note $it" }.first { it !in used }
    }

    /** Uses the editor's font family and size so notes look like code. */
    private fun editorFont(): Font {
        val scheme = EditorColorsManager.getInstance().globalScheme
        return Font(scheme.editorFontName, Font.PLAIN, scheme.editorFontSize)
    }

    private companion object {
        /** Remembers where the user dragged the text / checklist divider. */
        const val SPLIT_PROPORTION_KEY = "LessMiddle.NotesChecklistSplit"
    }
}

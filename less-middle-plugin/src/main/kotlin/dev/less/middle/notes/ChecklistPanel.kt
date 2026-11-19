package dev.less.middle.notes

import com.intellij.openapi.ui.Messages
import com.intellij.ui.CollectionListModel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.event.InputEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JPanel
import javax.swing.ListSelectionModel

/**
 * A note's checklist, keyboard first. In the "Add item" field: Enter adds, Down goes to the list.
 * In the list: Space toggles, Enter edits, Backspace/Delete removes, Opt+Up/Down moves, Up on the first row goes back to the field.
 * Clicking the box toggles; double-click edits. Every change is written straight into [note].
 */
class ChecklistPanel(private val note: Note) : JPanel(BorderLayout()) {

    private val model = CollectionListModel(note.items.toList())
    private val list = JBList(model)
    private val input = JBTextField()
    private val counter = JBLabel()

    init {
        border = JBUI.Borders.empty(6, 8)
        input.emptyText.text = "Add item (Enter)"
        input.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) = onInputKey(e)
        })
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.cellRenderer = ChecklistCellRenderer()
        list.emptyText.text = "No items"
        list.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) = onListKey(e)
        })
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) = onListClick(e)
        })
        counter.foreground = UIUtil.getContextHelpForeground()
        counter.border = JBUI.Borders.emptyBottom(4)

        val top = JPanel(BorderLayout()).apply {
            add(counter, BorderLayout.NORTH)
            add(input, BorderLayout.CENTER)
            border = JBUI.Borders.emptyBottom(6)
        }
        add(top, BorderLayout.NORTH)
        add(JBScrollPane(list).apply { border = JBUI.Borders.empty() }, BorderLayout.CENTER)
        updateCounter()
    }

    /** Moves keyboard focus to the "Add item" field. */
    fun focusInput() {
        input.requestFocusInWindow()
    }

    /** True if the keyboard focus is somewhere inside this checklist. */
    fun hasFocusInside(): Boolean = input.hasFocus() || list.hasFocus()

    /** Removes every item (used when the last note is "deleted", which only clears it). */
    fun clear() {
        model.removeAll()
        sync()
    }

    /** Enter adds the typed item and keeps the field focused for the next one; Down jumps into the list. */
    private fun onInputKey(e: KeyEvent) {
        when (e.keyCode) {
            KeyEvent.VK_ENTER -> {
                val text = input.text.trim()
                if (text.isNotEmpty()) {
                    model.add(ChecklistItem(text))
                    input.text = ""
                    sync()
                    list.ensureIndexIsVisible(model.size - 1)
                }
                e.consume()
            }
            KeyEvent.VK_DOWN -> if (model.size > 0) {
                list.selectedIndex = list.selectedIndex.coerceAtLeast(0)
                list.requestFocusInWindow()
                e.consume()
            }
        }
    }

    /** Keyboard handling inside the list; see the class comment for the keys. */
    private fun onListKey(e: KeyEvent) {
        val index = list.selectedIndex
        val alt = e.modifiersEx and InputEvent.ALT_DOWN_MASK != 0
        when {
            index < 0 -> return
            e.keyCode == KeyEvent.VK_SPACE -> toggle(index)
            e.keyCode == KeyEvent.VK_ENTER -> edit(index)
            e.keyCode == KeyEvent.VK_BACK_SPACE || e.keyCode == KeyEvent.VK_DELETE -> removeItem(index)
            alt && e.keyCode == KeyEvent.VK_UP -> moveItem(index, -1)
            alt && e.keyCode == KeyEvent.VK_DOWN -> moveItem(index, 1)
            e.keyCode == KeyEvent.VK_UP && index == 0 -> focusInput()
            else -> return
        }
        e.consume()
    }

    /** A click on the box toggles the item; a double click on the text edits it. */
    private fun onListClick(e: MouseEvent) {
        val index = list.locationToIndex(e.point).takeIf { it >= 0 && list.getCellBounds(it, it).contains(e.point) } ?: return
        val onBox = e.x - list.getCellBounds(index, index).x < JBUI.scale(BOX_HIT_WIDTH)
        when {
            e.clickCount == 1 && onBox -> toggle(index)
            e.clickCount == 2 && !onBox -> edit(index)
        }
    }

    /** Checks or unchecks the item. */
    private fun toggle(index: Int) {
        model.getElementAt(index).done = !model.getElementAt(index).done
        model.contentsChanged(model.getElementAt(index))
        sync()
    }

    /** Asks for new text; blank input keeps the old text. */
    private fun edit(index: Int) {
        val item = model.getElementAt(index)
        val text = Messages.showInputDialog(this, "Item:", "Edit Checklist Item", null, item.text, null)
            ?.trim()?.takeIf { it.isNotEmpty() } ?: return
        item.text = text
        model.contentsChanged(item)
        sync()
        list.requestFocusInWindow()
    }

    /** Removes the item and keeps a neighbouring row selected (or returns to the field when empty). */
    private fun removeItem(index: Int) {
        model.remove(index)
        sync()
        if (model.size == 0) focusInput() else list.selectedIndex = index.coerceAtMost(model.size - 1)
    }

    /**
     * Moves the item by [delta] rows, if there is room.
     * Example: items [a, b, c], moveItem(0, +1) -> [b, a, c]
     */
    private fun moveItem(index: Int, delta: Int) {
        val target = index + delta
        if (target !in 0 until model.size) return
        val item = model.getElementAt(index)
        model.remove(index)
        model.add(target, item)
        list.selectedIndex = target
        sync()
    }

    /** Writes the list back into the note and refreshes the "done / total" counter. */
    private fun sync() {
        note.items = model.items.toMutableList()
        updateCounter()
    }

    /** Shows e.g. "Checklist  2/5 done" above the field. */
    private fun updateCounter() {
        val done = model.items.count { it.done }
        counter.text = if (model.size == 0) "Checklist" else "Checklist  $done/${model.size} done"
    }

    private companion object {
        /** Width from the row's left edge that counts as a click on the checkbox itself. */
        const val BOX_HIT_WIDTH = 28
    }
}

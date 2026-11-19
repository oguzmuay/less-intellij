package dev.less.middle.notes

import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

/** Draws a checklist row as a checkbox; finished items are greyed out and struck through. */
class ChecklistCellRenderer : ListCellRenderer<ChecklistItem> {

    private val box = JBCheckBox().apply {
        border = JBUI.Borders.empty(3, 6)
        isOpaque = true
    }

    /** Configures the shared checkbox for one row. */
    override fun getListCellRendererComponent(
        list: JList<out ChecklistItem>,
        value: ChecklistItem?,
        index: Int,
        selected: Boolean,
        hasFocus: Boolean,
    ): Component {
        val item = value ?: return box
        box.isSelected = item.done
        box.text = if (item.done) "<html><s>${StringUtil.escapeXmlEntities(item.text)}</s></html>" else item.text
        box.font = list.font
        box.background = UIUtil.getListBackground(selected, list.hasFocus())
        box.foreground = when {
            selected && list.hasFocus() -> UIUtil.getListSelectionForeground(true)
            item.done -> UIUtil.getContextHelpForeground()
            else -> UIUtil.getListForeground()
        }
        return box
    }
}

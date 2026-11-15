package dev.less.middle

import com.intellij.openapi.fileEditor.impl.EditorEmptyTextPainter
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

/** Paints the LESS logo and a shortcut cheat sheet on the empty editor area. */
class LessEmptyTextPainter : EditorEmptyTextPainter() {

    /** Draws the centered title, then one row per shortcut: label on the left, key caps on the right. */
    override fun paintEmptyText(splitters: JComponent, component: JComponent, g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val rows = ShortcutRows.DEFAULT.map { it to ShortcutKeys.capsFor(it.actionId) }
            val rowHeight = JBUI.scale(30)
            val tableWidth = JBUI.scale(340)
            val titleHeight = JBUI.scale(70)
            val totalHeight = titleHeight + rows.size * rowHeight
            val left = (component.width - tableWidth) / 2
            var y = (component.height - totalHeight) / 2

            g2.font = UIUtil.getLabelFont().deriveFont(Font.BOLD, JBUI.scale(40).toFloat())
            g2.color = JBColor.namedColor("Editor.foreground", UIUtil.getInactiveTextColor())
            val title = "LESS"
            g2.drawString(title, (component.width - g2.fontMetrics.stringWidth(title)) / 2, y + JBUI.scale(40))
            y += titleHeight

            for ((row, caps) in rows) {
                paintRow(g2, row.label, caps, left, y, tableWidth, rowHeight)
                y += rowHeight
            }
        } finally {
            g2.dispose()
        }
    }

    /** Draws one label with its key caps right-aligned inside the table width. */
    private fun paintRow(g2: Graphics2D, label: String, caps: List<String>, left: Int, y: Int, width: Int, height: Int) {
        val baseline = y + height / 2 + g2.fontMetrics.ascent / 2 - JBUI.scale(2)
        g2.font = UIUtil.getLabelFont().deriveFont(JBUI.scale(14).toFloat())
        g2.color = UIUtil.getInactiveTextColor()
        g2.drawString(label, left, baseline)

        val capSize = JBUI.scale(24)
        val gap = JBUI.scale(4)
        var x = left + width - caps.size * (capSize + gap) + gap
        for (cap in caps) {
            g2.color = JBColor.namedColor("Button.default.startBackground", JBColor.border())
            g2.drawRoundRect(x, y + (height - capSize) / 2, capSize, capSize, JBUI.scale(6), JBUI.scale(6))
            g2.color = UIUtil.getInactiveTextColor()
            g2.drawString(cap, x + (capSize - g2.fontMetrics.stringWidth(cap)) / 2, baseline)
            x += capSize + gap
        }
    }
}

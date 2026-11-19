package dev.less.middle

import com.intellij.openapi.fileEditor.impl.EditorEmptyTextPainter
import com.intellij.ui.JBColor
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.less.middle.vim.VimShortcuts
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.JComponent

/**
 * Paints the LESS logo and a shortcut cheat sheet on the empty editor area: IDE shortcuts on the left,
 * Vim selection/clipboard shortcuts on the right (the Vim column is dropped when the area is too small).
 */
class LessEmptyTextPainter : EditorEmptyTextPainter() {

    /** Draws the centered title and one or two columns of label + key cap rows. */
    override fun paintEmptyText(splitters: JComponent, component: JComponent, g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            val ide = ShortcutRows.DEFAULT.map { it.label to ShortcutKeys.capsFor(it.actionId) }
            val vim = VimShortcuts.EMPTY_SCREEN.map { it.label to it.keys }

            val rowHeight = JBUI.scale(30)
            val columnWidth = JBUI.scale(340)
            val columnGap = JBUI.scale(80)
            val titleHeight = JBUI.scale(70)
            val headerHeight = JBUI.scale(28)
            val twoColumns = component.width >= 2 * columnWidth + columnGap + JBUI.scale(40) &&
                component.height >= titleHeight + headerHeight + vim.size * rowHeight + JBUI.scale(20)
            val columns = if (twoColumns) listOf("IDE" to ide, "Vim" to vim) else listOf("" to ide)

            val tallest = columns.maxOf { it.second.size }
            val totalHeight = titleHeight + (if (twoColumns) headerHeight else 0) + tallest * rowHeight
            val totalWidth = columns.size * columnWidth + (columns.size - 1) * columnGap
            var y = (component.height - totalHeight) / 2

            g2.font = UIUtil.getLabelFont().deriveFont(Font.BOLD, JBUI.scale(40).toFloat())
            g2.color = JBColor.namedColor("Editor.foreground", UIUtil.getInactiveTextColor())
            val title = "LESS"
            g2.drawString(title, (component.width - g2.fontMetrics.stringWidth(title)) / 2, y + JBUI.scale(40))
            y += titleHeight

            columns.forEachIndexed { index, (header, rows) ->
                val left = (component.width - totalWidth) / 2 + index * (columnWidth + columnGap)
                var rowY = y
                if (twoColumns) {
                    g2.font = UIUtil.getLabelFont().deriveFont(Font.BOLD, JBUI.scale(12).toFloat())
                    g2.color = UIUtil.getInactiveTextColor()
                    g2.drawString(header.uppercase(), left, rowY + JBUI.scale(16))
                    rowY += headerHeight
                }
                for ((label, caps) in rows) {
                    paintRow(g2, label, caps, left, rowY, columnWidth, rowHeight)
                    rowY += rowHeight
                }
            }
        } finally {
            g2.dispose()
        }
    }

    /** Draws one label with its key caps right-aligned inside the column width. */
    private fun paintRow(g2: Graphics2D, label: String, caps: List<String>, left: Int, y: Int, width: Int, height: Int) {
        g2.font = UIUtil.getLabelFont().deriveFont(JBUI.scale(14).toFloat())
        val baseline = y + height / 2 + g2.fontMetrics.ascent / 2 - JBUI.scale(2)
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

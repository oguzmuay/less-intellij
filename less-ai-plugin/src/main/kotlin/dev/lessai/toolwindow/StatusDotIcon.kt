package dev.lessai.toolwindow

import com.intellij.util.ui.JBUI
import java.awt.Color
import java.awt.Component
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import javax.swing.Icon

/** Small filled circle used as a session tab icon to show the session's status colour. */
class StatusDotIcon(private val color: Color) : Icon {

    /** Icon box width, the same as a regular 16px icon so tab labels line up. */
    override fun getIconWidth(): Int = JBUI.scale(BOX)

    /** Icon box height. */
    override fun getIconHeight(): Int = JBUI.scale(BOX)

    /** Paints an antialiased dot centred in the icon box. */
    override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g2.color = color
            val d = JBUI.scale(DOT)
            val offset = (iconWidth - d) / 2
            g2.fillOval(x + offset, y + offset, d, d)
        } finally {
            g2.dispose()
        }
    }

    private companion object {
        const val BOX = 16
        const val DOT = 8
    }
}

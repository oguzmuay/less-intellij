package dev.less.middle.vim

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.TitledSeparator
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JPanel

/** Modal listing the Vim selection / edit / clipboard shortcuts; closes with Esc. */
class VimCheatSheetDialog(project: Project?) : DialogWrapper(project, true, IdeModalityType.IDE) {

    init {
        title = "Vim Cheat Sheet"
        init()
    }

    /** All sections in one scrollable column: label on the left, keys in monospace on the right. */
    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val c = GridBagConstraints().apply { gridy = 0; anchor = GridBagConstraints.WEST }
        val keyFont = Font(Font.MONOSPACED, Font.BOLD, JBUI.scale(13))
        for (section in VimShortcuts.SECTIONS) {
            c.gridx = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.weightx = 1.0
            c.insets = JBUI.insetsTop(if (c.gridy == 0) 0 else 10)
            panel.add(TitledSeparator(section.title), c)
            c.gridy++
            c.gridwidth = 1; c.fill = GridBagConstraints.NONE; c.insets = JBUI.insets(2, 10, 2, 24)
            for (shortcut in section.shortcuts) {
                c.gridx = 0; c.weightx = 1.0
                panel.add(JBLabel(shortcut.label).apply { foreground = UIUtil.getContextHelpForeground() }, c)
                c.gridx = 1; c.weightx = 0.0
                panel.add(JBLabel(shortcut.keys.joinToString(" ")).apply { font = keyFont }, c)
                c.gridy++
            }
        }
        return JBScrollPane(panel).apply {
            border = JBUI.Borders.empty()
            preferredSize = JBUI.size(560, 620)
        }
    }

    /** No OK/Cancel row; Esc closes it. */
    override fun createSouthPanel(): JComponent? = null
}

package dev.lessai.settings

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBList
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel

/** Settings page (Settings | Tools | LESS AI) for binary folders and session defaults. */
class LessAiConfigurable : BoundConfigurable("LESS AI") {

    private val listModel = CollectionListModel<String>()
    private val folderList = JBList(listModel)
    private val configDirField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(null, FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Default Claude Config Folder"))
    }
    private val modelField = com.intellij.ui.components.JBTextField()

    /** Builds the settings form bound to LessAiSettings via apply/reset/isModified. */
    override fun createPanel(): DialogPanel {
        val decorated = ToolbarDecorator.createDecorator(folderList)
            .setAddAction {
                val file = FileChooser.chooseFile(FileChooserDescriptorFactory.createSingleFolderDescriptor(), null, null)
                if (file != null && file.path !in listModel.items) listModel.add(file.path)
            }
            .setRemoveAction { folderList.selectedValuesList.forEach { listModel.remove(it) } }
            .createPanel()
        return panel {
            group("Binary Search Folders") {
                row { cell(decorated).align(AlignX.FILL) }
                row { comment("/opt/homebrew/bin and /usr/local/bin are always searched. Folders listed here are searched first.") }
            }
            group("Session Defaults") {
                row("Claude config folder:") { cell(configDirField).align(AlignX.FILL) }
                row("Ollama model:") { cell(modelField).align(AlignX.FILL) }
            }
            onReset { loadFromSettings() }
            onIsModified { isFormModified() }
            onApply { saveToSettings() }
        }
    }

    /** Copies persisted settings into the form controls. */
    private fun loadFromSettings() {
        val s = LessAiSettings.getInstance()
        listModel.replaceAll(s.extraBinaryDirs)
        configDirField.text = s.defaultClaudeConfigDir
        modelField.text = s.defaultOllamaModel
    }

    /** Returns whether the form differs from persisted settings. */
    private fun isFormModified(): Boolean {
        val s = LessAiSettings.getInstance()
        return listModel.items != s.extraBinaryDirs ||
            configDirField.text.trim() != s.defaultClaudeConfigDir ||
            modelField.text.trim() != s.defaultOllamaModel
    }

    /** Writes the form values into persisted settings. */
    private fun saveToSettings() {
        val s = LessAiSettings.getInstance()
        s.extraBinaryDirs = listModel.items.toMutableList()
        s.defaultClaudeConfigDir = configDirField.text.trim()
        s.defaultOllamaModel = modelField.text.trim()
    }
}

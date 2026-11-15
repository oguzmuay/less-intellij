package dev.lessai.dialog

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import dev.lessai.session.AiProvider
import dev.lessai.session.CommandBuilder
import dev.lessai.session.NewSessionRequest
import dev.lessai.settings.LessAiSettings
import javax.swing.DefaultComboBoxModel
import javax.swing.JComponent

/** Dialog that collects the options for a new AI session. */
class NewSessionDialog(project: Project) : DialogWrapper(project) {

    private val providerBox = ComboBox(DefaultComboBoxModel(AiProvider.values())).apply {
        renderer = com.intellij.ui.SimpleListCellRenderer.create("") { it.displayName }
    }
    private val nameField = JBTextField().apply { emptyText.text = "Automatic" }
    private val configDirField = TextFieldWithBrowseButton().apply {
        addBrowseFolderListener(project, FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("Claude Config Folder"))
        text = LessAiSettings.getInstance().defaultClaudeConfigDir
    }
    private val modelBox = ComboBox<String>().apply {
        isEditable = true
        val defaultModel = LessAiSettings.getInstance().defaultOllamaModel
        if (defaultModel.isNotBlank()) selectedItem = defaultModel
    }
    private var modelsLoaded = false

    init {
        title = "New AI session"
        init()
        providerBox.addActionListener { loadModelsIfNeeded() }
        loadModelsIfNeeded()
    }

    /** Builds the dialog form with provider-dependent rows. */
    override fun createCenterPanel(): JComponent = panel {
        row("Provider:") { cell(providerBox).align(AlignX.FILL) }
        row("Tab name:") { cell(nameField).align(AlignX.FILL) }
        row("Claude config folder:") { cell(configDirField).align(AlignX.FILL) }
            .visibleIf(providerIs(AiProvider.CLAUDE))
        row("Ollama model:") { cell(modelBox).align(AlignX.FILL) }
            .visibleIf(providerIs(AiProvider.OLLAMA))
    }

    /** Returns a predicate bound to the provider combo for the given provider. */
    private fun providerIs(provider: AiProvider) = object : com.intellij.ui.layout.ComponentPredicate() {
        /** Whether the given provider is currently selected. */
        override fun invoke(): Boolean = providerBox.selectedItem == provider

        /** Re-evaluates on every provider change. */
        override fun addListener(listener: (Boolean) -> Unit) {
            providerBox.addActionListener { listener(invoke()) }
        }
    }

    /** Loads installed Ollama models off the EDT the first time OLLAMA is selected. */
    private fun loadModelsIfNeeded() {
        if (modelsLoaded || providerBox.selectedItem != AiProvider.OLLAMA) return
        modelsLoaded = true
        ApplicationManager.getApplication().executeOnPooledThread {
            val models = CommandBuilder.listOllamaModels()
            ApplicationManager.getApplication().invokeLater({
                val preferred = (modelBox.editor.item as? String)?.trim().orEmpty()
                models.forEach { modelBox.addItem(it) }
                if (preferred.isNotEmpty()) modelBox.selectedItem = preferred
            }, ModalityState.any())
        }
    }

    /** Requires a non-blank model when Ollama is selected. */
    override fun doValidate(): ValidationInfo? {
        val model = (modelBox.editor.item as? String)?.trim().orEmpty()
        return if (providerBox.selectedItem == AiProvider.OLLAMA && model.isEmpty()) {
            ValidationInfo("Ollama model is required", modelBox)
        } else null
    }

    /** Builds the session request from the current form values. */
    fun toRequest(): NewSessionRequest {
        val provider = providerBox.selectedItem as AiProvider
        val model = (modelBox.editor.item as? String)?.trim()
        return NewSessionRequest(
            provider = provider,
            name = nameField.text.trim().ifEmpty { null },
            claudeConfigDir = if (provider == AiProvider.CLAUDE) configDirField.text.trim().ifEmpty { null } else null,
            ollamaModel = if (provider == AiProvider.OLLAMA) model?.ifEmpty { null } else null,
        )
    }
}

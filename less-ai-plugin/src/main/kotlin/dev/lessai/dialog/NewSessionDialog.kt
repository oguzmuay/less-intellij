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
import dev.lessai.session.ClaudeProfile
import dev.lessai.session.ClaudeProfiles
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
    private val profiles = ClaudeProfiles.detect()
    private val customProfile = ClaudeProfile("Other folder…", null)
    private val profileBox = ComboBox(DefaultComboBoxModel((profiles + customProfile).toTypedArray())).apply {
        selectedItem = initialProfile()
    }
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
        row("Claude profile:") { cell(profileBox).align(AlignX.FILL) }
            .visibleIf(providerIs(AiProvider.CLAUDE))
        row("Config folder:") { cell(configDirField).align(AlignX.FILL) }
            .visibleIf(customProfileSelected())
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

    /**
     * Picks the profile to preselect: the one used last, else the settings default folder, else the first detected.
     * Example: last used "/Users/x/.claude-work" -> that profile; nothing remembered -> first profile
     */
    private fun initialProfile(): ClaudeProfile {
        val settings = LessAiSettings.getInstance()
        val remembered = if (settings.hasLastClaudeProfile) {
            profiles.firstOrNull { (it.configDir ?: "") == settings.lastClaudeProfileDir }
        } else null
        val fromDefault = settings.defaultClaudeConfigDir.takeIf { it.isNotBlank() }
            ?.let { dir -> profiles.firstOrNull { it.configDir == dir } }
        return remembered ?: fromDefault ?: profiles.firstOrNull() ?: customProfile
    }

    /** Predicate that is true while Claude is selected together with the "Other folder…" entry. */
    private fun customProfileSelected() = object : com.intellij.ui.layout.ComponentPredicate() {
        /** Whether the custom-folder row should be visible. */
        override fun invoke(): Boolean =
            providerBox.selectedItem == AiProvider.CLAUDE && profileBox.selectedItem === customProfile

        /** Re-evaluates on provider or profile change. */
        override fun addListener(listener: (Boolean) -> Unit) {
            providerBox.addActionListener { listener(invoke()) }
            profileBox.addActionListener { listener(invoke()) }
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

    /** Builds the session request from the current form values and remembers the chosen Claude profile. */
    fun toRequest(): NewSessionRequest {
        val provider = providerBox.selectedItem as AiProvider
        val model = (modelBox.editor.item as? String)?.trim()
        val profile = profileBox.selectedItem as? ClaudeProfile
        val configDir = when {
            provider != AiProvider.CLAUDE -> null
            profile === customProfile -> configDirField.text.trim().ifEmpty { null }
            else -> profile?.configDir
        }
        if (provider == AiProvider.CLAUDE && profile != null && profile !== customProfile) {
            LessAiSettings.getInstance().apply {
                lastClaudeProfileDir = profile.configDir ?: ""
                hasLastClaudeProfile = true
            }
        }
        var name = nameField.text.trim().ifEmpty { null }
        if (name == null && provider == AiProvider.CLAUDE && profile != null && profile !== customProfile && profile.configDir != null) {
            name = "${provider.displayName} (${profile.name})"
        }
        return NewSessionRequest(
            provider = provider,
            name = name,
            claudeConfigDir = configDir,
            ollamaModel = if (provider == AiProvider.OLLAMA) model?.ifEmpty { null } else null,
        )
    }
}

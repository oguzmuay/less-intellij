package dev.lessai.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** Application-level persistent settings for the LESS AI plugin. */
@Service(Service.Level.APP)
@State(name = "LessAiSettings", storages = [Storage("less-ai.xml")])
class LessAiSettings : PersistentStateComponent<LessAiSettingsState> {

    private var state = LessAiSettingsState()

    /** Directories searched first for claude/codex/gemini/ollama binaries. */
    var extraBinaryDirs: MutableList<String>
        get() = state.extraBinaryDirs
        set(value) { state.extraBinaryDirs = value.toMutableList() }

    /** Default Claude config folder for new sessions. */
    var defaultClaudeConfigDir: String
        get() = state.defaultClaudeConfigDir
        set(value) { state.defaultClaudeConfigDir = value }

    /** Default Ollama model for new sessions. */
    var defaultOllamaModel: String
        get() = state.defaultOllamaModel
        set(value) { state.defaultOllamaModel = value }

    /** Returns the current state for persistence. */
    override fun getState(): LessAiSettingsState = state

    /** Loads persisted state. */
    override fun loadState(state: LessAiSettingsState) {
        this.state = state
    }

    companion object {
        /** Returns the application-wide settings instance. */
        fun getInstance(): LessAiSettings = ApplicationManager.getApplication().getService(LessAiSettings::class.java)
    }
}

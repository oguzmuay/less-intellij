package dev.lessai.settings

/** Serializable state holder for LESS AI application-level settings. */
class LessAiSettingsState {
    /** Extra directories searched first for AI CLI binaries. */
    var extraBinaryDirs: MutableList<String> = mutableListOf()

    /** Default CLAUDE_CONFIG_DIR pre-filled in the new session dialog. */
    var defaultClaudeConfigDir: String = ""

    /** Default Ollama model pre-filled in the new session dialog. */
    var defaultOllamaModel: String = ""
}

package dev.lessai.session

/** User-supplied options for creating a new AI session. */
data class NewSessionRequest(
    /** Provider whose CLI should be launched. */
    val provider: AiProvider,
    /** Optional display name; auto-generated when null. */
    val name: String? = null,
    /** Optional Claude config directory, passed as CLAUDE_CONFIG_DIR. */
    val claudeConfigDir: String? = null,
    /** Optional Ollama model name, used only by the OLLAMA provider. */
    val ollamaModel: String? = null
)

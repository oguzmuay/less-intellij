package dev.lessai.session

/** AI CLI providers that can be launched as a terminal session. */
enum class AiProvider(val displayName: String, val binary: String) {
    /** Anthropic Claude Code CLI. */
    CLAUDE("Claude", "claude"),

    /** OpenAI Codex CLI. */
    CODEX("Codex", "codex"),

    /** Google Gemini CLI. */
    GEMINI("Gemini", "gemini"),

    /** Local models served through the Ollama CLI. */
    OLLAMA("Ollama", "ollama")
}

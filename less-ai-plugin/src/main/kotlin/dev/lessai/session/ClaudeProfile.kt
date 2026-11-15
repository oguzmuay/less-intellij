package dev.lessai.session

/**
 * A Claude Code account profile backed by a config folder.
 * [configDir] is null for the default profile (~/.claude), which needs no CLAUDE_CONFIG_DIR.
 */
data class ClaudeProfile(val name: String, val configDir: String?) {
    /** Text shown in the profile drop-down. Example: ClaudeProfile("claude-work", "/Users/x/.claude-work") -> "claude-work" */
    override fun toString(): String = name
}

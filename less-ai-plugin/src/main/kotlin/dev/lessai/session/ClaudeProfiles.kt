package dev.lessai.session

import java.io.File

/** Discovers Claude Code config folders in the user's home directory. */
object ClaudeProfiles {
    private val MARKERS = listOf("settings.json", "projects", "history.jsonl", "sessions")

    /**
     * Finds profiles: ~/.claude (default) and any ~/.claude-* / ~/.claude_* folder that looks like a config dir.
     * Example: ~/.claude, ~/.claude-work -> [ClaudeProfile("claude", null), ClaudeProfile("claude-work", "/Users/x/.claude-work")]
     */
    fun detect(home: File = File(System.getProperty("user.home"))): List<ClaudeProfile> {
        val dirs = home.listFiles { f -> f.isDirectory && isProfileName(f.name) && looksLikeConfigDir(f) }
            .orEmpty().sortedBy { it.name }
        val profiles = dirs.map { dir ->
            val isDefault = dir.name == ".claude"
            ClaudeProfile(dir.name.removePrefix("."), if (isDefault) null else dir.path)
        }
        return profiles.sortedByDescending { it.configDir == null }
    }

    /**
     * Checks the folder name pattern.
     * Example: ".claude" -> true, ".claude-work" -> true, ".claude.json" -> false, ".config" -> false
     */
    private fun isProfileName(name: String): Boolean =
        name == ".claude" || name.startsWith(".claude-") || name.startsWith(".claude_")

    /** True when the folder contains at least one file or folder Claude Code creates in a config dir. */
    private fun looksLikeConfigDir(dir: File): Boolean = MARKERS.any { File(dir, it).exists() }
}

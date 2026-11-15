package dev.less.middle

/** The default list of rows shown on the empty editor screen. */
object ShortcutRows {
    /** Rows in display order; shortcuts are looked up from the active keymap at paint time. */
    val DEFAULT: List<ShortcutRow> = listOf(
        ShortcutRow("Find file", "GotoFile"),
        ShortcutRow("Find in file", "Find"),
        ShortcutRow("Find in project", "FindInPath"),
        ShortcutRow("Recent files", "RecentFiles"),
        ShortcutRow("Command palette", "GotoAction"),
        ShortcutRow("Search everywhere", "SearchEverywhere"),
        ShortcutRow("Files panel", "ActivateProjectToolWindow"),
    )
}

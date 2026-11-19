package dev.less.middle.vim

/** The Vim shortcuts of the LESS ideavimrc (ide-config/ideavimrc), for the empty screen and the cheat-sheet popup. */
object VimShortcuts {

    /** Short list painted next to the IDE shortcuts on the empty editor screen. */
    val EMPTY_SCREEN: List<VimShortcut> = listOf(
        s("Select word", "viw"),
        s("To end of word", "ve"),
        s("To end of line", "v$"),
        s("Whole line", "V"),
        s("Inside { } block", "vi{"),
        s("Inside quotes", "vi\""),
        s("Grow selection", "vv"),
        s("Shrink selection", "⌥↓"),
        s("Copy (clipboard)", "y"),
        s("Delete (no clipboard)", "d"),
        s("Cut selection", "x"),
        s("Vim cheat sheet", "g?"),
    )

    /** Everything, grouped, for the popup opened with g?. */
    val SECTIONS: List<VimShortcutSection> = listOf(
        VimShortcutSection(
            "Select (v + range; use y / d / c instead of v to copy / delete / change it)",
            listOf(
                s("Word under cursor", "viw"),
                s("Word with trailing space", "vaw"),
                s("Word incl. dots / dashes", "viW"),
                s("Cursor to end of word", "ve"),
                s("Cursor to end of line", "v$"),
                s("Cursor to end of line (no newline)", "vg_"),
                s("Cursor to line start", "v0"),
                s("Whole line", "V"),
                s("Inside { } block", "vi{"),
                s("{ } block with braces", "va{"),
                s("Inside ( )  /  [ ]", "vi("),
                s("Inside quotes", "vi\""),
                s("Inside tag", "vit"),
                s("Paragraph", "vip"),
                s("Same indentation block", "vii"),
                s("Function argument", "via"),
                s("Whole file", "vae"),
                s("Grow to enclosing scope (repeat)", "vv"),
                s("Shrink selection", "⌥↓"),
                s("Block (column) selection", "⌃v"),
            ),
        ),
        VimShortcutSection(
            "Edit",
            listOf(
                s("Change inside quotes", "ci\""),
                s("Empty the parentheses", "di("),
                s("Change to end of word", "cw"),
                s("Delete / change to end of line", "D"),
                s("Delete line", "dd"),
                s("Change line", "cc"),
                s("Move line up / down", "⌥⇧↑"),
                s("Toggle comment", "gcc"),
                s("Undo / redo", "u"),
                s("Repeat last change", "."),
            ),
        ),
        VimShortcutSection(
            "Clipboard (shared with macOS)",
            listOf(
                s("Copy selection", "y"),
                s("Copy line", "yy"),
                s("Copy whole file", "yae"),
                s("Paste after / before", "p"),
                s("Cut selection", "x"),
                s("Delete without touching clipboard", "d"),
            ),
        ),
    )

    /**
     * Builds a shortcut whose keys are the characters of [keys], one cap each.
     * Example: s("Select word", "viw") -> caps ["v", "i", "w"]
     */
    private fun s(label: String, keys: String): VimShortcut =
        VimShortcut(label, keys.codePoints().toArray().map { String(Character.toChars(it)) })
}

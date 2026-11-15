package dev.less.middle

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil

/** Reads real shortcuts from the active keymap and splits them into key caps. */
object ShortcutKeys {
    /**
     * Returns the key caps of the first keyboard shortcut bound to the action, or an empty list.
     * Example: GotoFile on LESS keymap -> ["⌘", "O"]; SearchEverywhere -> ["⇧", "⇧"]; unbound action -> []
     */
    fun capsFor(actionId: String): List<String> {
        val shortcuts = KeymapManager.getInstance().activeKeymap.getShortcuts(actionId)
        val text = shortcuts.filterIsInstance<KeyboardShortcut>().firstOrNull()?.let { KeymapUtil.getShortcutText(it) }
            ?: shortcuts.firstOrNull()?.let { KeymapUtil.getShortcutText(it) }
            ?: DOUBLE_MODIFIER_FALLBACK[actionId]
            ?: return emptyList()
        return if (text.contains('+')) text.split('+').filter { it.isNotBlank() }
        else text.codePoints().toArray().map { String(Character.toChars(it)) }
    }

    /** Actions triggered by a double key press are not always exposed as keymap shortcuts. */
    private val DOUBLE_MODIFIER_FALLBACK = mapOf("SearchEverywhere" to "\u21E7\u21E7")
}

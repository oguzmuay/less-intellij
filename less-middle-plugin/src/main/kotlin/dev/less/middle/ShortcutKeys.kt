package dev.less.middle

import com.intellij.openapi.actionSystem.KeyboardShortcut
import com.intellij.openapi.keymap.KeymapManager
import com.intellij.openapi.keymap.KeymapUtil

/** Reads real shortcuts from the active keymap and splits them into key caps. */
object ShortcutKeys {
    /**
     * Returns the key caps of the first keyboard shortcut bound to the action, or an empty list.
     * Example: GotoFile on LESS keymap -> ["⌘", "O"]; unbound action -> []
     */
    fun capsFor(actionId: String): List<String> {
        val shortcut = KeymapManager.getInstance().activeKeymap.getShortcuts(actionId)
            .filterIsInstance<KeyboardShortcut>()
            .firstOrNull() ?: return emptyList()
        val text = KeymapUtil.getShortcutText(shortcut)
        return if (text.contains('+')) text.split('+').filter { it.isNotBlank() }
        else text.codePoints().toArray().map { String(Character.toChars(it)) }
    }
}

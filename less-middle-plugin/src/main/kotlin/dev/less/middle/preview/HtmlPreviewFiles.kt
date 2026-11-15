package dev.less.middle.preview

import com.intellij.openapi.vfs.VirtualFile

/** Decides which files get the LESS HTML preview (types the IDE has no same-tab preview for). */
object HtmlPreviewFiles {
    private val extensions = setOf("html", "htm")

    /** True for local .html/.htm files; e.g. "index.HTML" -> true, "logo.svg" -> false. */
    fun isPreviewable(file: VirtualFile): Boolean =
        !file.isDirectory && file.isInLocalFileSystem && file.extension?.lowercase() in extensions
}

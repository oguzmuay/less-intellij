package dev.less.middle.preview

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.vfs.VirtualFile

/** Makes every newly opened text+preview editor (Markdown, SVG, HTML, ...) start in text-only layout, as in LESS. */
class TextFirstLayoutListener : FileEditorManagerListener {

    /** Switches split editors of the freshly opened file to text-only. */
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        source.getAllEditors(file)
            .filterIsInstance<TextEditorWithPreview>()
            .filter { it.getLayout() != TextEditorWithPreview.Layout.SHOW_EDITOR }
            .forEach { it.setLayout(TextEditorWithPreview.Layout.SHOW_EDITOR) }
    }
}

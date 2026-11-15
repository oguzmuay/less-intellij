package dev.less.middle.preview

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider
import com.intellij.openapi.project.DumbAware

/** Opens .html/.htm as a normal text editor plus a hidden JCEF preview (same mechanism as Markdown). */
class HtmlPreviewEditorProvider : TextEditorWithPreviewProvider(HtmlJcefPreviewProvider()), DumbAware {
    /** Wraps both halves in a split editor that starts in text-only layout. */
    override fun createSplitEditor(firstEditor: TextEditor, secondEditor: FileEditor): FileEditor =
        TextEditorWithPreview(firstEditor, secondEditor, "HTML Editor", TextEditorWithPreview.Layout.SHOW_EDITOR)
}

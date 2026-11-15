package dev.less.middle.preview

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.IdeFocusManager

/** Text <-> preview switching: split editors (Markdown, SVG, Mermaid, our HTML) first, then multi-editor tabs. */
object PreviewToggle {

    /** True when the selected editor (or its tab) offers a preview to toggle to. */
    fun isAvailable(project: Project, contextEditor: FileEditor?): Boolean {
        val editor = resolveEditor(project, contextEditor) ?: return false
        return splitEditorOf(editor) != null || alternateProviderId(project, editor) != null
    }

    /** Toggles the selected editor between text and preview; does nothing when no preview exists. */
    fun toggle(project: Project, contextEditor: FileEditor?) {
        val editor = resolveEditor(project, contextEditor) ?: return
        val split = splitEditorOf(editor)
        if (split != null) {
            toggleSplit(project, split)
            return
        }
        val file = editor.file ?: return
        val providerId = alternateProviderId(project, editor) ?: return
        FileEditorManager.getInstance(project).setSelectedEditor(file, providerId)
    }

    /** Strategy A: flips the split layout between text-only and preview-only and focuses the visible half. */
    private fun toggleSplit(project: Project, split: TextEditorWithPreview) {
        val showText = split.getLayout() != TextEditorWithPreview.Layout.SHOW_EDITOR
        split.setLayout(if (showText) TextEditorWithPreview.Layout.SHOW_EDITOR else TextEditorWithPreview.Layout.SHOW_PREVIEW)
        val target = if (showText) split.textEditor else split.previewEditor
        target.preferredFocusedComponent?.let { IdeFocusManager.getInstance(project).requestFocus(it, true) }
    }

    /** The editor the action works on: the context editor, else the selected editor of the active window. */
    private fun resolveEditor(project: Project, contextEditor: FileEditor?): FileEditor? =
        contextEditor ?: FileEditorManager.getInstance(project).selectedEditor

    /** The split editor that is, or contains, [editor]. */
    private fun splitEditorOf(editor: FileEditor): TextEditorWithPreview? =
        editor as? TextEditorWithPreview ?: TextEditorWithPreview.getParentSplitEditor(editor)

    /**
     * Strategy B: when the tab holds a plain text editor and a non-text editor, returns the provider id
     * of the one that is not currently selected; null otherwise.
     */
    private fun alternateProviderId(project: Project, editor: FileEditor): String? {
        val file: VirtualFile = editor.file ?: return null
        val manager = FileEditorManagerEx.getInstanceEx(project)
        val pair = manager.getEditorsWithProviders(file)
        val editors = pair.first
        val providers = pair.second
        if (editors.size < 2 || editors.size != providers.size) return null
        val textIndex = editors.indexOfFirst { it is TextEditor }
        val previewIndex = editors.indexOfFirst { it !is TextEditor }
        if (textIndex < 0 || previewIndex < 0) return null
        val selected = manager.getSelectedEditor(file)
        val targetIndex = if (selected is TextEditor) previewIndex else textIndex
        return providers[targetIndex].editorTypeId
    }
}

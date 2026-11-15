package dev.less.middle.preview

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefApp

/** Creates the preview half of the HTML split editor; used only through [HtmlPreviewEditorProvider], not registered alone. */
class HtmlJcefPreviewProvider : FileEditorProvider, DumbAware {
    /** Accepts local HTML files when JCEF is available. */
    override fun accept(project: Project, file: VirtualFile): Boolean =
        HtmlPreviewFiles.isPreviewable(file) && JBCefApp.isSupported()

    /** Builds the JCEF preview for the file. */
    override fun createEditor(project: Project, file: VirtualFile): FileEditor = HtmlJcefPreviewEditor(file)

    /** Stable id; also part of the split editor's id. */
    override fun getEditorTypeId(): String = "less-html-preview"

    /** Irrelevant for a wrapped provider; never shown as its own tab. */
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
}

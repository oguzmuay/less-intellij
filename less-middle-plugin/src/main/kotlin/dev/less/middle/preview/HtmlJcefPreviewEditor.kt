package dev.less.middle.preview

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.Alarm
import java.beans.PropertyChangeListener
import javax.swing.JComponent

/** JCEF-based preview pane that renders an HTML document and re-renders it while it is edited. */
class HtmlJcefPreviewEditor(private val file: VirtualFile) : UserDataHolderBase(), FileEditor, DocumentListener {
    private val browser = JBCefBrowser()
    private val document: Document? = FileDocumentManager.getInstance().getDocument(file)
    private val refreshAlarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)

    init {
        Disposer.register(this, browser)
        document?.addDocumentListener(this, this)
        render()
    }

    /** Schedules a debounced re-render after every document change. */
    override fun documentChanged(event: DocumentEvent) {
        refreshAlarm.cancelAllRequests()
        refreshAlarm.addRequest({ render() }, REFRESH_DELAY_MS)
    }

    /** Loads the current (possibly unsaved) text with the file's URL as base, so relative assets resolve. */
    private fun render() {
        if (Disposer.isDisposed(this)) return
        val html = document?.text ?: VfsUtilCore.loadText(file)
        browser.loadHTML(html, file.url)
    }

    /** The browser's Swing component shown in the preview half. */
    override fun getComponent(): JComponent = browser.component

    /** Focus target when the preview is shown alone. */
    override fun getPreferredFocusedComponent(): JComponent = browser.component

    /** Name used by the split editor toolbar. */
    override fun getName(): String = "HTML Preview"

    /** The previewed file. */
    override fun getFile(): VirtualFile = file

    /** The preview keeps no navigable state. */
    override fun setState(state: FileEditorState) = Unit

    /** A preview never has unsaved changes of its own. */
    override fun isModified(): Boolean = false

    /** Valid as long as the underlying file exists. */
    override fun isValid(): Boolean = file.isValid

    /** No observable properties. */
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = Unit

    /** No observable properties. */
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = Unit

    /** Browser, alarm and document listener are children of this disposable and go with it. */
    override fun dispose() = Unit

    private companion object {
        const val REFRESH_DELAY_MS = 300
    }
}

package dev.less.middle.notes

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/** Application-wide store for the notes modal (shared by all projects), saved in less-notes.xml. */
@Service(Service.Level.APP)
@State(name = "LessNotes", storages = [Storage("less-notes.xml")])
class NotesStorage : PersistentStateComponent<NotesState> {

    private var state = NotesState()

    /** Returns the current state for persistence; the modal edits it in place. */
    override fun getState(): NotesState = state

    /** Loads persisted notes. */
    override fun loadState(state: NotesState) {
        this.state = state
    }

    companion object {
        /** Returns the application-wide notes store. */
        fun getInstance(): NotesStorage = ApplicationManager.getApplication().getService(NotesStorage::class.java)
    }
}

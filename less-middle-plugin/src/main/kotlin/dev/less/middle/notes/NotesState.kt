package dev.less.middle.notes

/** Serializable state of the notes modal: the notes in tab order and the last selected tab. */
class NotesState {
    var notes: MutableList<Note> = mutableListOf()
    var selectedIndex: Int = 0
}

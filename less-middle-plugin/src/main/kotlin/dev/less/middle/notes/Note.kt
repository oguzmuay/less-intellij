package dev.less.middle.notes

/** One note shown as a tab in the notes modal; a mutable bean so the XML serializer can persist it. */
class Note() {
    var title: String = ""
    var text: String = ""
    var items: MutableList<ChecklistItem> = mutableListOf()

    /** Creates a note with the given title and empty text. */
    constructor(title: String) : this() {
        this.title = title
    }
}

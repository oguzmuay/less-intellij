package dev.less.middle.notes

/** One checklist entry of a note; a mutable bean so the XML serializer can persist it. */
class ChecklistItem() {
    var text: String = ""
    var done: Boolean = false

    /** Creates an unchecked item with the given text. */
    constructor(text: String) : this() {
        this.text = text
    }
}

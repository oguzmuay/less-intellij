package dev.lessai.session

/** Receives change notifications from [SessionManager]; all callbacks run on the EDT. */
interface SessionListener {
    /** Called when sessions were added or removed. */
    fun sessionsChanged() {}

    /** Called when the selected session changed. */
    fun selectionChanged() {}

    /** Called when a session's name or status changed. */
    fun sessionUpdated(session: AiSession) {}
}

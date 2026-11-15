package dev.lessai.session

/** Lifecycle state of an AI session as shown to the user. */
enum class SessionStatus(val label: String) {
    /** The CLI process has been launched but has not produced output yet. */
    STARTING("starting"),

    /** The AI is currently producing output or running tools. */
    WORKING("working"),

    /** The AI finished its turn and waits for the user's next message. */
    IDLE("idle (your turn)"),

    /** The AI is blocked on a permission prompt or a question. */
    WAITING_APPROVAL("waiting for approval/input")
}

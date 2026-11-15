package dev.lessai.inbox

/** Receives notifications when an agent's inbox changed or was marked read; invoked on the EDT. */
interface MailboxListener {
    /** Called when the inbox of [agentName] received mail or its read state changed. */
    fun mailChanged(agentName: String)
}

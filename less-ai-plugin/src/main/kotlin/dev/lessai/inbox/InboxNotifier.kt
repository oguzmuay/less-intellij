package dev.lessai.inbox

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import dev.lessai.session.AiProvider
import dev.lessai.session.SessionManager
import dev.lessai.session.SessionStatus

/** Types a "new mail" hint into an idle session's terminal when mail arrives for its provider. */
class InboxNotifier(private val project: Project) : MailboxListener {

    private val notified = mutableMapOf<String, Int>()

    /** Starts listening for mail; messages already present are not announced. */
    fun start(parent: Disposable) {
        val mailbox = MailboxService.getInstance(project)
        mailbox.ensureCreated()
        AiProvider.entries.forEach { notified[it.displayName] = mailbox.messages(it.displayName).size }
        mailbox.addListener(this, parent)
    }

    /** Announces each new message once to an IDLE session of the matching provider. */
    override fun mailChanged(agentName: String) {
        val mailbox = MailboxService.getInstance(project)
        val all = mailbox.messages(agentName)
        val previous = notified[agentName] ?: 0
        notified[agentName] = all.size
        if (all.size <= previous) return
        val session = SessionManager.getInstance(project).sessions
            .firstOrNull { it.provider.displayName == agentName && it.status == SessionStatus.IDLE } ?: return
        if (agentName == AiProvider.OLLAMA.displayName) {
            all.drop(previous).forEach { session.widget.sendCommandToExecute(it.body.replace(Regex("\\s*\\R\\s*"), " ")) }
        } else {
            val dir = mailbox.dir
            session.widget.sendCommandToExecute("You have new mail in $dir/inbox/$agentName.md")
        }
    }
}

package dev.lessai.status

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Condition
import com.intellij.openapi.util.Disposer
import dev.lessai.session.AiProvider
import dev.lessai.session.AiSession
import dev.lessai.session.LaunchContext
import dev.lessai.session.SessionManager
import dev.lessai.session.SessionStatus

/**
 * Picks and runs the status sources of one session and forwards their results to [SessionManager];
 * for Claude the hook source wins and the output heuristic is ignored once a hook has fired.
 */
class StatusCoordinator(private val project: Project, private val session: AiSession) : Disposable {

    @Volatile
    private var disposed = false
    @Volatile
    private var hookSeen = false

    /** Creates the provider's sources, ties them to this coordinator and starts them. */
    fun start() {
        val heuristic = OutputHeuristicStatusSource(session.widget)
        register(heuristic, authoritative = false)
        when (session.provider) {
            AiProvider.CLAUDE -> register(ClaudeHookStatusSource(session.id), authoritative = true)
            AiProvider.CODEX -> register(CodexNotifyStatusSource(session.id), authoritative = false)
            AiProvider.GEMINI, AiProvider.OLLAMA -> Unit
        }
    }

    /** Stops all sources (they are children) and deletes the session's temp files. */
    override fun dispose() {
        disposed = true
        StatusFiles.cleanup(session.id)
    }

    /** Registers [source] as a child disposable and starts it with a sink that applies [authoritative] rules. */
    private fun register(source: StatusSource, authoritative: Boolean) {
        Disposer.register(this, source)
        source.start { status -> onStatus(status, authoritative) }
    }

    /** Applies a reported status on the EDT, dropping heuristic results once an authoritative hook was seen. */
    private fun onStatus(status: SessionStatus, authoritative: Boolean) {
        if (authoritative) hookSeen = true
        if (!authoritative && hookSeen) return
        ApplicationManager.getApplication().invokeLater({
            if (!authoritative && hookSeen) return@invokeLater
            SessionManager.getInstance(project).setStatus(session, status)
        }, ModalityState.any(), Condition<Any?> { disposed || project.isDisposed })
    }

    companion object {
        private val LOG = logger<StatusCoordinator>()

        /**
         * Prepares status-detection files for a session about to start and returns the CLI args they need.
         * Example: (id, CLAUDE) -> ["--settings", "'.../id.settings.json'"], (id, GEMINI) -> []
         */
        fun launchContextFor(sessionId: String, provider: AiProvider): LaunchContext = try {
            when (provider) {
                AiProvider.CLAUDE -> ClaudeHookStatusSource.prepareLaunch(sessionId)
                AiProvider.CODEX -> CodexNotifyStatusSource.prepareLaunch(sessionId)
                AiProvider.GEMINI, AiProvider.OLLAMA -> LaunchContext.EMPTY
            }
        } catch (e: Exception) {
            LOG.warn("Could not prepare status files for $provider session $sessionId", e)
            LaunchContext.EMPTY
        }
    }
}

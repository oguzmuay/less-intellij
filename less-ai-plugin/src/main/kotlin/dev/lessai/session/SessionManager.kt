package dev.lessai.session

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.AppExecutorUtil
import org.jetbrains.plugins.terminal.LocalTerminalDirectRunner
import org.jetbrains.plugins.terminal.ShellStartupOptions
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit

/** Owns the project's AI sessions and their terminal widgets; must be used from the EDT. */
@Service(Service.Level.PROJECT)
class SessionManager(private val project: Project) : Disposable {

    private val sessionList = mutableListOf<AiSession>()
    private val listeners = CopyOnWriteArrayList<SessionListener>()

    /** Snapshot of all open sessions in creation order. */
    val sessions: List<AiSession>
        get() = sessionList.toList()

    /** Currently selected session; setting it notifies listeners when it changes. */
    var selected: AiSession? = null
        set(value) {
            val target = value?.takeIf { it in sessionList }
            if (field === target) return
            field = target
            fire { it.selectionChanged() }
        }

    /** Starts a terminal in the project directory, runs the provider CLI in it and selects the new session. */
    fun create(request: NewSessionRequest): AiSession {
        ApplicationManager.getApplication().assertIsDispatchThread()
        val name = request.name?.trim()?.takeIf { it.isNotBlank() }
            ?: CommandBuilder.autoName(request.provider, sessionList.map { it.name })
        val command = CommandBuilder.buildCommand(request)
        val sessionDisposable = Disposer.newDisposable(this, "LessAi session $name")
        val options = ShellStartupOptions.Builder().workingDirectory(project.basePath).build()
        val widget = LocalTerminalDirectRunner.createTerminalRunner(project)
            .startShellTerminalWidget(sessionDisposable, options, true)
        widget.sendCommandToExecute(command)

        val session = AiSession(UUID.randomUUID().toString(), name, request.provider, widget, command, sessionDisposable)
        sessionList.add(session)
        fire { it.sessionsChanged() }
        selected = session
        scheduleIdle(session)
        return session
    }

    /** Disposes the session's terminal, removes it and selects a neighbouring session. */
    fun close(session: AiSession) {
        ApplicationManager.getApplication().assertIsDispatchThread()
        val index = sessionList.indexOf(session)
        if (index < 0) return
        sessionList.removeAt(index)
        Disposer.dispose(session.disposable)
        fire { it.sessionsChanged() }
        if (selected === session || selected == null) {
            selected = sessionList.getOrNull(index.coerceAtMost(sessionList.size - 1))
        }
    }

    /** Renames the session; blank names are ignored. */
    fun rename(session: AiSession, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty() || session.name == trimmed || session !in sessionList) return
        session.name = trimmed
        fire { it.sessionUpdated(session) }
    }

    /** Updates the session status and notifies listeners; hook for later status detection. */
    fun setStatus(session: AiSession, status: SessionStatus) {
        if (session.status == status || session !in sessionList) return
        session.status = status
        fire { it.sessionUpdated(session) }
    }

    /** Selects the session after the current one, wrapping around to the first. */
    fun selectNext() = step(1)

    /** Selects the session before the current one, wrapping around to the last. */
    fun selectPrevious() = step(-1)

    /** Registers a listener that is removed automatically when [parent] is disposed. */
    fun addListener(listener: SessionListener, parent: Disposable) {
        listeners.add(listener)
        Disposer.register(parent) { listeners.remove(listener) }
    }

    /** Disposes every session's terminal widget when the project closes. */
    override fun dispose() {
        sessionList.forEach { Disposer.dispose(it.disposable) }
        sessionList.clear()
        listeners.clear()
    }

    /** Moves the selection by [delta] positions with wrap-around. */
    private fun step(delta: Int) {
        if (sessionList.isEmpty()) return
        val current = selected?.let { sessionList.indexOf(it) } ?: -1
        val next = if (current < 0) 0 else Math.floorMod(current + delta, sessionList.size)
        selected = sessionList[next]
    }

    /** Placeholder status logic: flips STARTING to IDLE a few seconds after the command was sent. */
    private fun scheduleIdle(session: AiSession) {
        AppExecutorUtil.getAppScheduledExecutorService().schedule({
            ApplicationManager.getApplication().invokeLater({
                if (session.status == SessionStatus.STARTING) setStatus(session, SessionStatus.IDLE)
            }, project.disposed)
        }, IDLE_DELAY_SECONDS, TimeUnit.SECONDS)
    }

    /** Invokes [action] for every registered listener. */
    private fun fire(action: (SessionListener) -> Unit) {
        listeners.forEach(action)
    }

    companion object {
        private const val IDLE_DELAY_SECONDS = 3L

        /** Returns the project's session manager service. */
        fun getInstance(project: Project): SessionManager = project.service()
    }
}

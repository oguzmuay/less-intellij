package dev.lessai.status

import com.intellij.openapi.Disposable
import dev.lessai.session.SessionStatus

/** A detector that reports a session's status; stopped by disposing it. */
interface StatusSource : Disposable {

    /** Begins detection; [sink] may be called from any thread with each newly detected status. */
    fun start(sink: (SessionStatus) -> Unit)
}

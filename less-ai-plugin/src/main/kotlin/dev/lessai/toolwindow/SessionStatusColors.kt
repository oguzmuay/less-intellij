package dev.lessai.toolwindow

import com.intellij.ui.JBColor
import dev.lessai.session.SessionStatus
import java.awt.Color

/** Badge colours of the session statuses, shared by the session tabs. */
object SessionStatusColors {
    /**
     * Maps a status to its colour.
     * Example: STARTING -> amber, WORKING -> blue, IDLE -> green, WAITING_APPROVAL -> red
     */
    fun of(status: SessionStatus): Color = when (status) {
        SessionStatus.STARTING -> JBColor(Color(0xE0A100), Color(0xD9A400))
        SessionStatus.WORKING -> JBColor(Color(0x2F7FE0), Color(0x4A90E2))
        SessionStatus.IDLE -> JBColor(Color(0x3C9B3C), Color(0x5FB85F))
        SessionStatus.WAITING_APPROVAL -> JBColor(Color(0xD0342C), Color(0xE5534B))
    }
}

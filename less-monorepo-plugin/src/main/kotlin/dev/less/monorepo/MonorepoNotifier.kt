package dev.less.monorepo

import com.intellij.ide.util.PropertiesComponent
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

/** Shows a one-time summary balloon of the detected modules; shown again only when the set changes. */
object MonorepoNotifier {
    private const val GROUP_ID = "LESS Monorepo"
    private const val SIGNATURE_KEY = "dev.less.monorepo.lastSignature"

    /** A project with fewer modules is not a monorepo and gets no balloon. */
    private const val MIN_MODULES = 2

    /** Notifies about [modules] unless the same set was already announced for this project. */
    fun announce(project: Project, modules: Collection<DetectedModule>) {
        if (modules.size < MIN_MODULES) return
        val signature = modules.joinToString("|") { "${it.path}:${it.primary}" }.hashCode().toString()
        val props = PropertiesComponent.getInstance(project)
        if (props.getValue(SIGNATURE_KEY) == signature) return
        props.setValue(SIGNATURE_KEY, signature)

        NotificationGroupManager.getInstance().getNotificationGroup(GROUP_ID)
            .createNotification("${modules.size} modules detected", summary(modules), NotificationType.INFORMATION)
            .notify(project)
    }

    /**
     * Counts modules per primary kind, most common first.
     * Example: [Gradle, Gradle, React] -> "2 Gradle, 1 React"
     */
    private fun summary(modules: Collection<DetectedModule>): String =
        modules.groupingBy { it.primary.displayName }.eachCount()
            .entries.sortedByDescending { it.value }
            .joinToString(", ") { "${it.value} ${it.key}" }
}

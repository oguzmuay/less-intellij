package dev.less.monorepo

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent

/** Rescans the project when a marker file (pom.xml, package.json, ...) or a module directory changes. */
class MonorepoFileListener(private val project: Project) : BulkFileListener {
    /** Schedules one rescan if any event in the batch can change the detected modules. */
    override fun after(events: List<VFileEvent>) {
        if (project.isDisposed) return
        val service = project.service<MonorepoService>()
        if (events.any { affectsModules(it, service) }) service.scheduleRescan()
    }

    /** True for marker file changes inside this project and for deletes/moves/renames of module directories. */
    private fun affectsModules(event: VFileEvent, service: MonorepoService): Boolean {
        if (!service.isInsideProject(event.path)) return false
        val name = event.path.substringAfterLast('/')
        return when (event) {
            is VFilePropertyChangeEvent -> event.isRename &&
                (MarkerFiles.isMarker(event.oldValue.toString()) || MarkerFiles.isMarker(event.newValue.toString()) ||
                    service.hasModulesUnder(event.oldPath))
            is VFileDeleteEvent, is VFileMoveEvent -> MarkerFiles.isMarker(name) || service.hasModulesUnder(event.path)
            else -> MarkerFiles.isMarker(name)
        }
    }
}

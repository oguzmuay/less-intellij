package dev.less.monorepo

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Holds the modules detected in this project and rescans when marker files change. */
@Service(Service.Level.PROJECT)
class MonorepoService(private val project: Project, private val scope: CoroutineScope) {
    @Volatile
    private var modules: Map<String, DetectedModule> = emptyMap()

    @Volatile
    private var rootPaths: List<String> = emptyList()

    private var pending: Job? = null

    /** Module whose root is the directory at [path], or null. */
    fun moduleAt(path: String): DetectedModule? = modules[path]

    /** All detected modules, outermost first. */
    fun all(): Collection<DetectedModule> = modules.values

    /** Number of detected modules below [path] (not counting the one at [path]). */
    fun nestedCount(path: String): Int = modules.keys.count { it.startsWith("$path/") }

    /** True if [path] is inside one of the scanned roots. */
    fun isInsideProject(path: String): Boolean = rootPaths.any { path == it || path.startsWith("$it/") }

    /** True if a detected module lives at or below [path]; used when a directory is deleted or moved. */
    fun hasModulesUnder(path: String): Boolean = modules.keys.any { it == path || it.startsWith("$path/") }

    /** Scans the project now, publishes the result and repaints the Project view. */
    suspend fun rescan(): Collection<DetectedModule> {
        val (roots, found) = readAction {
            MonorepoScanner.roots(project).map { it.path } to MonorepoScanner.scan(project)
        }
        rootPaths = roots
        modules = found
        withContext(Dispatchers.EDT) {
            if (!project.isDisposed) ProjectView.getInstance(project).refresh()
        }
        return found.values
    }

    /** Rescans after a short quiet period so a burst of file events causes one scan. */
    fun scheduleRescan() {
        synchronized(this) {
            pending?.cancel()
            pending = scope.launch {
                delay(RESCAN_DELAY_MS)
                rescan()
            }
        }
    }

    companion object {
        private const val RESCAN_DELAY_MS = 500L
    }
}

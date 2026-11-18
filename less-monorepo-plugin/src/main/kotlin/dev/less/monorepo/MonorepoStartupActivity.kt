package dev.less.monorepo

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/** Scans the project for modules as soon as it opens, in the background. */
class MonorepoStartupActivity : ProjectActivity {
    /** Runs the first scan and announces the result. */
    override suspend fun execute(project: Project) {
        val modules = project.service<MonorepoService>().rescan()
        MonorepoNotifier.announce(project, modules)
    }
}

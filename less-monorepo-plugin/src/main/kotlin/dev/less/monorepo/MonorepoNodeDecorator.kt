package dev.less.monorepo

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.components.service

/** Replaces the folder icon of detected module directories in the Project view and adds a short label. */
class MonorepoNodeDecorator : ProjectViewNodeDecorator {
    /** Shows the module's icon, e.g. "api  Spring Boot · Gradle · Kotlin", with the full list in the tooltip. */
    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val file = node.virtualFile ?: return
        if (!file.isDirectory) return // file icons come from NamedFileIconProvider
        val dir = file
        val project = node.project?.takeIf { !it.isDisposed } ?: return
        val service = project.service<MonorepoService>()
        val module = service.moduleAt(dir.path)
        if (module == null) {
            NamedIcons.forFolder(dir.name)?.let { data.setIcon(it) }
            return
        }

        data.setIcon(module.primary.icon)
        val label = label(module, service.nestedCount(dir.path))
        val existing = data.locationString
        data.locationString = if (existing.isNullOrBlank()) label else "$existing  ·  $label"
        data.tooltip = module.labels.joinToString(", ")
    }

    /**
     * Short grey label: at most three names plus the workspace size.
     * Example: Gradle workspace with 4 modules -> "Gradle · Kotlin · workspace (4)"
     */
    private fun label(module: DetectedModule, nested: Int): String {
        val parts = module.labels.take(MAX_LABELS).toMutableList()
        if (module.workspace) parts += if (nested > 0) "workspace ($nested)" else "workspace"
        return parts.joinToString(" · ")
    }

    companion object {
        private const val MAX_LABELS = 3
    }
}

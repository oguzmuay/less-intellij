package dev.less.monorepo

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor

/** Walks the project's directories and collects every detected module. Call under a read action. */
object MonorepoScanner {
    /** How many directory levels below a root are inspected. */
    private const val MAX_DEPTH = 8

    /** Dependency, build output and tool directories that never hold the user's own modules. */
    private val SKIPPED_DIRS = setOf(
        "node_modules", "build", "target", "dist", "out", "bin", "obj", "vendor", "venv", "env",
        "__pycache__", "Pods", "DerivedData", "coverage", "tmp",
    )

    /** Bundle directories (Xcode projects, asset catalogs, frameworks) whose insides are never modules. */
    private val SKIPPED_EXTENSIONS = setOf("xcodeproj", "xcworkspace", "xcassets", "framework", "xcframework", "app", "bundle")

    /** Top-level directories that are scanned: content roots plus the project dir, without nested duplicates. */
    fun roots(project: Project): List<VirtualFile> {
        val all = (ProjectRootManager.getInstance(project).contentRoots.toList() + listOfNotNull(project.guessProjectDir()))
            .filter { it.isValid && it.isDirectory }
            .distinct()
        return all.filter { root -> all.none { other -> other != root && VfsUtilCore.isAncestor(other, root, true) } }
    }

    /** Scans all roots and returns the detected modules keyed by directory path. */
    fun scan(project: Project): Map<String, DetectedModule> {
        val index = ProjectFileIndex.getInstance(project)
        val result = LinkedHashMap<String, DetectedModule>()
        for (root in roots(project)) {
            VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Unit>(limit(MAX_DEPTH)) {
                /** Detects a module in each directory and decides whether to descend into it. */
                override fun visitFileEx(file: VirtualFile): Result {
                    if (!file.isDirectory) return CONTINUE
                    if (file != root && (isSkipped(file) || index.isExcluded(file))) return SKIP_CHILDREN
                    ModuleDetector.detect(file)
                        ?.let { withoutNestedCMake(it, file, result) }
                        ?.let { result[it.path] = it }
                    return CONTINUE
                }
            })
        }
        return result
    }

    /** True for hidden, dependency, build output and bundle directories. */
    private fun isSkipped(dir: VirtualFile): Boolean =
        dir.name.startsWith(".") || dir.name in SKIPPED_DIRS || dir.extension in SKIPPED_EXTENSIONS

    /**
     * CMake projects put a CMakeLists.txt in every source folder; only the outermost one is a module.
     * Drops the CMake kind when an ancestor is already a CMake module, and the whole module if nothing remains.
     */
    private fun withoutNestedCMake(module: DetectedModule, dir: VirtualFile, found: Map<String, DetectedModule>): DetectedModule? {
        if (ModuleKind.CMAKE !in module.kinds) return module
        val nested = generateSequence(dir.parent) { it.parent }
            .any { found[it.path]?.kinds?.contains(ModuleKind.CMAKE) == true }
        if (!nested) return module
        val kinds = module.kinds - ModuleKind.CMAKE
        return if (kinds.isEmpty()) null else module.copy(kinds = kinds)
    }
}

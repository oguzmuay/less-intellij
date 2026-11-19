package dev.less.monorepo

import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

/** Supplies the name-based file icons (Markdown, AI files, Kubernetes manifests, VERSION) everywhere: Project view, tabs, search. */
class NamedFileIconProvider : FileIconProvider {
    /** Returns the named icon for a regular file, or null to fall back to the file type's icon. */
    override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? =
        if (file.isDirectory) null else NamedIcons.forFile(file.name)
}

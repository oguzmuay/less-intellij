package dev.less.monorepo

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Icons chosen purely by folder/file name (docs, infra, docker and kubernetes (k8s, k3s) folders and the VERSION file). */
object NamedIcons {
    private fun load(name: String): Icon = IconLoader.getIcon("/icons/monorepo/$name.svg", NamedIcons::class.java)

    private val docs = load("docs")
    private val infra = load("infra")
    private val docker = load("docker")
    private val kubernetes = load("kubernetes")
    private val version = load("version")

    private val folders: Map<String, Icon> = buildMap {
        listOf("docs", "doc", "documentation", "documantation", "dokumantasyon").forEach { put(it, docs) }
        listOf("infra", "infrastructure", "altyapi").forEach { put(it, infra) }
        listOf("docker", "container", "containers").forEach { put(it, docker) }
        listOf("k8s", "k3s", "kubernetes", "kube", "helm").forEach { put(it, kubernetes) }
    }

    /**
     * Icon for a folder by its name, or null.
     * Example: "K3s" -> kubernetes icon, "src" -> null
     */
    fun forFolder(name: String): Icon? = folders[name.lowercase()]

    /**
     * Icon for a file by its name, or null.
     * Example: "VERSION" -> version icon, "version.txt" -> null
     */
    fun forFile(name: String): Icon? = if (name == "VERSION") version else null
}

package dev.less.monorepo

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/** Icons chosen purely by folder/file name: well-known folders, Markdown, AI instruction files and the VERSION file. */
object NamedIcons {
    /** Loads /icons/monorepo/<name>.svg (the _dark variant is picked up automatically). */
    fun load(name: String): Icon = IconLoader.getIcon("/icons/monorepo/$name.svg", NamedIcons::class.java)

    private val config = load("config")
    private val markdown = load("markdown")

    private val folders: Map<String, Icon> = buildMap {
        fun add(icon: String, vararg names: String) = load(icon).let { i -> names.forEach { put(it, i) } }
        add("docs", "docs", "doc", "documentation", "documantation", "dokumantasyon")
        add("infra", "infra", "infrastructure", "altyapi")
        add("docker", "docker", "container", "containers")
        add("kubernetes", "k8s", "k3s", "kubernetes", "kube", "helm")
        add("github", ".github")
        add("idea", ".idea")
        add("scripts", "script", "scripts")
        add("livekit", "livekit")
        add("whisper", "whisper")
        add("keycloak", "keycloak")
        add("seaweedfs", "seaweedfs", "seaweed")
        add("claude-dir", ".claude")
        add("codex-dir", ".codex")
        add("gemini-dir", ".gemini")
    }

    private val files: Map<String, Icon> = buildMap {
        fun add(icon: String, vararg names: String) = load(icon).let { i -> names.forEach { put(it, i) } }
        add("claude", "claude.md", "claude.local.md")
        add("agents", "agents.md", "agent.md", "agents.override.md")
        add("gemini", "gemini.md")
    }

    private val version = load("version")

    /**
     * Icon for a folder by its name, or null; any name containing "config" gets the config icon.
     * Example: "K3s" -> kubernetes icon, "app-config" -> config icon, "src" -> null
     */
    fun forFolder(name: String): Icon? {
        val lower = name.lowercase()
        return folders[lower] ?: config.takeIf { "config" in lower }
    }

    /**
     * Icon for a file by its name, or null.
     * Example: "VERSION" -> version, "CLAUDE.md" -> claude, "README.md" -> markdown, "api-svc.yaml" -> k8s service
     */
    fun forFile(name: String): Icon? {
        if (name == "VERSION") return version
        val lower = name.lowercase()
        files[lower]?.let { return it }
        if (lower.endsWith(".md")) return markdown
        return KubernetesIcons.forFile(lower)
    }
}

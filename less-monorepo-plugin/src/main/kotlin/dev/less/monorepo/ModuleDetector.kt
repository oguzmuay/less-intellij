package dev.less.monorepo

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/** Decides from a directory's marker files which build systems and frameworks it uses. Call under a read action. */
object ModuleDetector {
    /** Marker files bigger than this are not read; only their presence counts. */
    private const val MAX_READ_BYTES = 512 * 1024

    /** Node frameworks in priority order: the first one found in the dependencies wins. */
    private val NODE_FRAMEWORKS = listOf(
        "next" to ModuleKind.NEXT,
        "nuxt" to ModuleKind.VUE,
        "@angular/core" to ModuleKind.ANGULAR,
        "@nestjs/core" to ModuleKind.NEST,
        "@sveltejs/kit" to ModuleKind.SVELTE,
        "svelte" to ModuleKind.SVELTE,
        "vue" to ModuleKind.VUE,
        "react" to ModuleKind.REACT,
    )

    /** Returns the module detected in [dir], or null if it has no marker files. */
    fun detect(dir: VirtualFile): DetectedModule? {
        // Directories are included: Xcode projects (*.xcodeproj, *.xcworkspace) are bundles, not files.
        val children = dir.children.associateBy { it.name }
        if (children.keys.none(MarkerFiles::isMarker)) return null
        val acc = Accumulator()
        detectJvm(children, acc)
        detectNode(children, acc)
        detectPython(children, acc)
        detectOthers(children, acc)
        if (acc.kinds.isEmpty()) return null
        return DetectedModule(dir.path, acc.kinds.sortedBy { it.ordinal }, acc.tags.distinct(), acc.workspace)
    }

    /** Gradle, Maven and sbt, including Spring Boot and Kotlin/Android hints from the build script. */
    private fun detectJvm(files: Map<String, VirtualFile>, acc: Accumulator) {
        val gradleBuild = files["build.gradle.kts"] ?: files["build.gradle"]
        val gradleSettings = files["settings.gradle.kts"] ?: files["settings.gradle"]
        if (gradleBuild != null || gradleSettings != null) {
            acc.kinds += ModuleKind.GRADLE
            val script = text(gradleBuild)
            if ("org.springframework.boot" in script) acc.kinds += ModuleKind.SPRING_BOOT
            if ("com.android." in script) acc.kinds += ModuleKind.ANDROID
            if ("kotlin(" in script || "org.jetbrains.kotlin" in script) acc.tags += "Kotlin"
            if ("include" in text(gradleSettings)) acc.workspace = true
        }
        files["pom.xml"]?.let { pom ->
            acc.kinds += ModuleKind.MAVEN
            val xml = text(pom)
            if ("spring-boot" in xml) acc.kinds += ModuleKind.SPRING_BOOT
            if ("<modules>" in xml) acc.workspace = true
        }
        if ("build.sbt" in files) acc.kinds += ModuleKind.SBT
    }

    /** package.json: framework from the dependencies (mobile first), TypeScript, package manager and workspaces. */
    private fun detectNode(files: Map<String, VirtualFile>, acc: Accumulator) {
        val packageJson = files["package.json"] ?: return
        val json = parseJson(text(packageJson))
        val deps = listOf("dependencies", "devDependencies", "peerDependencies")
            .flatMap { key -> runCatching { json?.getAsJsonObject(key)?.keySet() }.getOrNull().orEmpty() }
            .toSet()
        val expoAppJson = "\"expo\"" in text(files["app.json"])
        val framework = when {
            "expo" in deps || expoAppJson -> ModuleKind.EXPO
            "react-native" in deps -> ModuleKind.REACT_NATIVE
            else -> NODE_FRAMEWORKS.firstOrNull { it.first in deps }?.second ?: ModuleKind.NODE
        }
        acc.kinds += framework
        if (framework == ModuleKind.EXPO && "react-native" in deps) acc.kinds += ModuleKind.REACT_NATIVE
        if ("typescript" in deps || "tsconfig.json" in files) acc.tags += "TypeScript"
        if ("vite" in deps) acc.tags += "Vite"
        when {
            "pnpm-lock.yaml" in files || "pnpm-workspace.yaml" in files -> acc.tags += "pnpm"
            "yarn.lock" in files -> acc.tags += "Yarn"
            "bun.lockb" in files || "bun.lock" in files -> acc.tags += "Bun"
        }
        val workspaceFiles = listOf("pnpm-workspace.yaml", "lerna.json", "nx.json", "turbo.json")
        if (json?.has("workspaces") == true || workspaceFiles.any { it in files }) acc.workspace = true
    }

    /** Python projects plus the common web frameworks and tool chains. */
    private fun detectPython(files: Map<String, VirtualFile>, acc: Accumulator) {
        val markers = listOf("pyproject.toml", "setup.py", "setup.cfg", "requirements.txt", "Pipfile", "manage.py")
        if (markers.none { it in files }) return
        acc.kinds += ModuleKind.PYTHON
        val deps = (text(files["pyproject.toml"]) + text(files["requirements.txt"]) + text(files["Pipfile"])).lowercase()
        when {
            "manage.py" in files || "django" in deps -> acc.tags += "Django"
            "fastapi" in deps -> acc.tags += "FastAPI"
            "flask" in deps -> acc.tags += "Flask"
        }
        when {
            "uv.lock" in files -> acc.tags += "uv"
            "poetry.lock" in files -> acc.tags += "Poetry"
        }
    }

    /** Rust, Go, .NET, iOS/Swift, Dart/Flutter, Ruby, PHP and CMake. */
    private fun detectOthers(files: Map<String, VirtualFile>, acc: Accumulator) {
        files["Cargo.toml"]?.let {
            acc.kinds += ModuleKind.RUST
            if ("[workspace]" in text(it)) acc.workspace = true
        }
        if ("go.mod" in files) acc.kinds += ModuleKind.GO
        if ("go.work" in files) {
            acc.kinds += ModuleKind.GO
            acc.workspace = true
        }
        val dotnet = files.keys.map { it.substringAfterLast('.', "") }
        if (dotnet.any { it in setOf("csproj", "fsproj", "vbproj") }) acc.kinds += ModuleKind.DOTNET
        if (dotnet.any { it == "sln" || it == "slnx" }) {
            acc.kinds += ModuleKind.DOTNET
            acc.workspace = true
        }
        val xcode = files.keys.any { it.endsWith(".xcodeproj") || it.endsWith(".xcworkspace") }
        if (xcode || "Podfile" in files) acc.kinds += ModuleKind.IOS
        if ("Podfile" in files) acc.tags += "CocoaPods"
        if ("Package.swift" in files) acc.kinds += ModuleKind.SWIFT
        files["pubspec.yaml"]?.let {
            acc.kinds += ModuleKind.DART
            if ("flutter:" in text(it)) acc.tags += "Flutter"
        }
        files["Gemfile"]?.let {
            acc.kinds += ModuleKind.RUBY
            if ("rails" in text(it)) acc.tags += "Rails"
        }
        files["composer.json"]?.let {
            acc.kinds += ModuleKind.PHP
            if ("laravel/framework" in text(it)) acc.tags += "Laravel"
        }
        if ("CMakeLists.txt" in files) acc.kinds += ModuleKind.CMAKE
    }

    /** File text, or "" for a missing, unreadable or oversized file. */
    private fun text(file: VirtualFile?): String {
        if (file == null || file.length > MAX_READ_BYTES) return ""
        return runCatching { VfsUtilCore.loadText(file) }.getOrDefault("")
    }

    /** Parses a JSON object, or null if the text is not a JSON object. */
    private fun parseJson(text: String): JsonObject? =
        runCatching { JsonParser.parseString(text).asJsonObject }.getOrNull()
}

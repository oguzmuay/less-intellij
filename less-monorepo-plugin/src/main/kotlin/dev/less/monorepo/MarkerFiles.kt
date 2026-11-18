package dev.less.monorepo

/** File names that make a directory a module; a change to any of them triggers a rescan. */
object MarkerFiles {
    private val NAMES = setOf(
        "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts",
        "pom.xml", "build.sbt",
        "package.json", "pnpm-workspace.yaml", "lerna.json", "nx.json", "turbo.json", "tsconfig.json",
        "pyproject.toml", "setup.py", "setup.cfg", "requirements.txt", "Pipfile", "manage.py",
        "Cargo.toml", "go.mod", "go.work", "Package.swift", "pubspec.yaml",
        "Gemfile", "composer.json", "CMakeLists.txt", "Podfile", "app.json",
    )
    private val EXTENSIONS = setOf("csproj", "fsproj", "vbproj", "sln", "slnx", "xcodeproj", "xcworkspace")

    /**
     * True if a file with this name can turn a directory into a module.
     * Example: "pom.xml" -> true, "Api.csproj" -> true, "App.xcodeproj" -> true, "README.md" -> false
     */
    fun isMarker(name: String): Boolean = name in NAMES || name.substringAfterLast('.', "") in EXTENSIONS
}

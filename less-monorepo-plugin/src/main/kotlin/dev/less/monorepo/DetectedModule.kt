package dev.less.monorepo

/**
 * A directory recognised as a module: its kinds (highest priority first), extra tags such as
 * "TypeScript" or "pnpm", and whether it is a workspace root that aggregates other modules.
 */
data class DetectedModule(
    val path: String,
    val kinds: List<ModuleKind>,
    val tags: List<String>,
    val workspace: Boolean,
) {
    /** Kind whose icon is shown for the directory. */
    val primary: ModuleKind get() = kinds.first()

    /** All labels in display order, e.g. ["Spring Boot", "Gradle", "Kotlin"]. */
    val labels: List<String> get() = (kinds.map { it.displayName } + tags).distinct()
}

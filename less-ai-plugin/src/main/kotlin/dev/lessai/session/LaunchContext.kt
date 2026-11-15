package dev.lessai.session

/** Extra launch inputs for a session command: environment variables and additional CLI arguments. */
data class LaunchContext(
    /** Variables prefixed to the command as `KEY='value'`; values are quoted by [CommandBuilder]. */
    val env: Map<String, String> = emptyMap(),
    /** Shell-ready argument fragments appended after the provider's own arguments (already quoted). */
    val extraArgs: List<String> = emptyList()
) {
    /** Merges two contexts; later env values win, args are concatenated in order. */
    operator fun plus(other: LaunchContext): LaunchContext =
        LaunchContext(env + other.env, extraArgs + other.extraArgs)

    companion object {
        /** A context that adds nothing to the command. */
        val EMPTY = LaunchContext()
    }
}

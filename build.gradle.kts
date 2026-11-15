plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21" apply false
    id("org.jetbrains.intellij.platform") version "2.20.1" apply false
}

/** Path of the locally installed IntelliJ IDEA the plugins are built against. */
val ideaHome: String by extra {
    val candidates = listOfNotNull(
        providers.environmentVariable("IDEA_HOME").orNull,
        "${System.getProperty("user.home")}/Applications/IntelliJ IDEA.app",
        "/Applications/IntelliJ IDEA.app",
        "${System.getProperty("user.home")}/Applications/IntelliJ IDEA Ultimate.app",
        "/Applications/IntelliJ IDEA Ultimate.app",
    )
    candidates.firstOrNull { File(it).exists() }
        ?: error("IntelliJ IDEA not found. Set IDEA_HOME to the IDE app path.")
}

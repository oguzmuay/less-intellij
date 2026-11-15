plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "dev.lessai"
version = providers.gradleProperty("pluginVersion").getOrElse("0.1.1")

val ideaHome: String? by rootProject.extra

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // Local IDE when installed, otherwise download it (CI).
        if (ideaHome != null) local(ideaHome!!) else intellijIdea(providers.gradleProperty("ideaVersion").getOrElse("2026.3"))
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

kotlin { jvmToolchain(25) }

intellijPlatform {
    pluginConfiguration {
        id = "dev.lessai.ai"
        name = "LESS AI"
        version = project.version.toString()
        ideaVersion { sinceBuild = "263" }
    }
}

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "dev.less"
version = providers.gradleProperty("pluginVersion").getOrElse("0.1.3")

val ideaHome: String? by rootProject.extra

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // Local IDE when installed, otherwise download it (CI).
        if (ideaHome != null) local(ideaHome!!) else intellijIdea(providers.gradleProperty("ideaVersion").getOrElse("2026.3"))
        // JCEF lives in a bundled plugin since 2026.x; used for the HTML preview.
        bundledPlugin("com.intellij.modules.jcef")
    }
}

kotlin { jvmToolchain(25) }

intellijPlatform {
    pluginConfiguration {
        id = "dev.less.middle"
        name = "LESS Middle"
        version = project.version.toString()
        ideaVersion { sinceBuild = "263" }
    }
}

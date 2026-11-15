plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "dev.lessai"
version = "0.1.0"

val ideaHome: String by rootProject.extra

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        local(ideaHome)
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

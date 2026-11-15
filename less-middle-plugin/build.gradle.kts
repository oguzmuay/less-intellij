plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
}

group = "dev.less"
version = "0.1.0"

val ideaHome: String by rootProject.extra

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        local(ideaHome)
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

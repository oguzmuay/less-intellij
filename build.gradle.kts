import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.4.21"
    id("org.jetbrains.intellij.platform") version "2.20.1"
}

group = "dev.lessai"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform { defaultRepositories() }
}

dependencies {
    intellijPlatform {
        // Build against the locally installed IDE instead of downloading one.
        local(providers.environmentVariable("IDEA_HOME").orElse("/Users/oguzugurdogan/Applications/IntelliJ IDEA.app"))
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

kotlin {
    jvmToolchain(25)
}

intellijPlatform {
    pluginConfiguration {
        id = "dev.lessai.ai"
        name = "LESS AI"
        version = project.version.toString()
        ideaVersion {
            sinceBuild = "263"
        }
    }
}

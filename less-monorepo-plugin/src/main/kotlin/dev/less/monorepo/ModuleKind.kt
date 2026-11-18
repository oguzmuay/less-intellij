package dev.less.monorepo

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * A build system or framework a directory can be recognised as; each has its own folder icon.
 * Declaration order is the priority used to pick the icon when one directory matches several kinds.
 */
enum class ModuleKind(val displayName: String, iconName: String) {
    SPRING_BOOT("Spring Boot", "spring"),
    ANDROID("Android", "android"),
    GRADLE("Gradle", "gradle"),
    MAVEN("Maven", "maven"),
    SBT("sbt", "scala"),
    RUST("Rust", "rust"),
    GO("Go", "go"),
    DOTNET(".NET", "dotnet"),
    IOS("iOS", "ios"),
    SWIFT("Swift", "swift"),
    DART("Dart", "dart"),
    PYTHON("Python", "python"),
    RUBY("Ruby", "ruby"),
    PHP("PHP", "php"),
    EXPO("Expo", "expo"),
    REACT_NATIVE("React Native", "react-native"),
    NEXT("Next.js", "next"),
    ANGULAR("Angular", "angular"),
    NEST("NestJS", "nest"),
    SVELTE("Svelte", "svelte"),
    VUE("Vue", "vue"),
    REACT("React", "react"),
    NODE("Node", "node"),
    CMAKE("CMake", "cmake");

    /** Folder icon for this kind; IconLoader picks the `_dark` variant on dark themes. */
    val icon: Icon = IconLoader.getIcon("/icons/monorepo/$iconName.svg", ModuleKind::class.java)
}

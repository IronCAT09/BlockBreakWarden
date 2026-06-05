pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.5"
}

stonecutter {
    // Each entry is a Minecraft version that gets its own build target.
    // Boundary versions: one per API / Java / behavior break across 1.20 -> 1.21.x.
    // 26.x is intentionally absent: Yarn mappings are not published for it yet.
    // Adding a version later = one entry here + one row in build.gradle.kts.
    create(rootProject) {
        versions(
            "1.20.1",
            "1.20.4",
            "1.20.6",
            "1.21.1",
            "1.21.4",
            "1.21.6",
            "1.21.8",
            "1.21.11"
        )
        vcsVersion = "1.21.8"
    }
}

rootProject.name = "BlockBreakWarden"

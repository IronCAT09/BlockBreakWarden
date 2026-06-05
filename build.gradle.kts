plugins {
    id("fabric-loom") version "1.16.3"
    id("maven-publish")
}

// ---------------------------------------------------------------------------
// Per-version dependency matrix.
// One row per build target declared in settings.gradle.kts. Values pulled from
// meta.fabricmc.net (Yarn) and modrinth.com (Fabric API / Mod Menu).
// ---------------------------------------------------------------------------
data class VersionDeps(
    val yarn: String,
    val fabricApi: String,
    val modmenu: String,
    /** Value for the `minecraft` entry in fabric.mod.json `depends`. */
    val minecraftDependency: String
)

val mcVersion = stonecutter.current.version

val deps: VersionDeps = when (mcVersion) {
    "1.20.1"  -> VersionDeps("1.20.1+build.10", "0.92.9+1.20.1",   "7.2.2",  ">=1.20- <1.20.2")
    "1.20.4"  -> VersionDeps("1.20.4+build.3",  "0.97.3+1.20.4",   "9.2.0",  ">=1.20.2 <1.20.5")
    "1.20.6"  -> VersionDeps("1.20.6+build.3",  "0.100.8+1.20.6",  "10.0.0", ">=1.20.5 <1.21-")
    "1.21.1"  -> VersionDeps("1.21.1+build.3",  "0.116.12+1.21.1", "11.0.4", ">=1.21- <1.21.4")
    "1.21.4"  -> VersionDeps("1.21.4+build.8",  "0.119.4+1.21.4",  "13.0.4", ">=1.21.4 <1.21.6")
    "1.21.6"  -> VersionDeps("1.21.6+build.1",  "0.128.2+1.21.6",  "15.0.2", ">=1.21.6 <1.21.8")
    "1.21.8"  -> VersionDeps("1.21.8+build.1",  "0.136.1+1.21.8",  "15.0.2", ">=1.21.8 <1.21.9")
    "1.21.11" -> VersionDeps("1.21.11+build.6", "0.141.4+1.21.11", "17.0.0", ">=1.21.9")
    else -> throw GradleException("No dependency matrix entry for Minecraft $mcVersion")
}

// 1.20.5+ requires Java 21; earlier 1.20.x runs on Java 17.
val javaVersion: Int = if (stonecutter.current.parsed >= "1.20.5") 21 else 17
val loaderVersion = "0.19.3"

version = "${property("mod_version")}+$mcVersion"
group = property("maven_group") as String

base {
    archivesName.set(property("archives_base_name") as String)
}

repositories {
    maven("https://maven.terraformersmc.com/") { name = "TerraformersMC" }
    // Patbox / pb4 (placeholder-api etc.) — transitive deps of some Mod Menu builds,
    // only needed if Mod Menu is pulled transitively (e.g. for a dev run).
    maven("https://maven.nucleoid.xyz/") { name = "Nucleoid" }
}

dependencies {
    minecraft("com.mojang:minecraft:$mcVersion")
    mappings("net.fabricmc:yarn:${deps.yarn}:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${deps.fabricApi}")

    // Mod Menu (compile-only against its API). Transitive deps are dropped so the
    // build doesn't need to resolve Mod Menu's whole dependency tree on every version;
    // the API classes we use (ModMenuApi / ConfigScreenFactory) are self-contained.
    modImplementation("com.terraformersmc:modmenu:${deps.modmenu}") { isTransitive = false }
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(javaVersion)
}

java {
    withSourcesJar()
    val jv = JavaVersion.toVersion(javaVersion)
    sourceCompatibility = jv
    targetCompatibility = jv
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks.processResources {
    val props = mapOf(
        "version" to project.version,
        "minecraft_dependency" to deps.minecraftDependency,
        "java_version" to ">=$javaVersion"
    )
    inputs.properties(props)
    filesMatching("fabric.mod.json") {
        expand(props)
    }
}

tasks.jar {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${project.base.archivesName.get()}" }
    }
}

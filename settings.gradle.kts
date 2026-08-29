import com.possible_triangle.gradle.settings.localRepository
import com.possible_triangle.gradle.settings.ResolutionStrategy

pluginManagement {
    repositories {
        mavenLocal()
        maven("https://maven.quiltmc.org/repository/release") {
            name = "Quilt"
        }
        maven("https://maven.fabricmc.net") {
            name = "Fabric"
        }
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForged"
        }
        maven("https://jitpack.io") {
            name = "Jitpack"
        }
        maven("https://maven.frozenblock.net/snapshot") { // Candlelight & Triangle
            name = "FrozenBlock Snapshot"
        }
        maven("https://maven.minecraftforge.net/") {
            name = "MinecraftForge"
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val neoforgeSnapshotMaven = settings.providers.gradleProperty("neoforge_snapshot_maven").orNull
if (!neoforgeSnapshotMaven.isNullOrBlank()) {
    pluginManagement {
        repositories {
            maven(neoforgeSnapshotMaven) { name = "NeoForge Snapshots" }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version("+")
    id("net.frozenblock.triangle.helper") version("+")
}

helper {
    versionStrategy = ResolutionStrategy.SNAPSHOT
}

rootProject.name = "Glowtone"

object Constants {
    const val FABRIC: Boolean = true
    const val NEOFORGE: Boolean = true
}

include("gt-common")
project(":gt-common").projectDir = file("common")

if (Constants.FABRIC) {
    include("gt-fabric")
    project(":gt-fabric").projectDir = file("fabric")
}

if (Constants.NEOFORGE) {
    include("gt-neoforge")
    project(":gt-neoforge").projectDir = file("neoforge")
}

localRepository("FrozenLib",
    "net.frozenblock:frozenlib",
    prefix = "flib",
    multi = true,
    enabled = true
)

localPluginRepository(
    "GradleHelper",
    enabled = true
)

fun localPluginRepository(repo: String, enabled: Boolean = true) {
    if (!enabled) return
    println("Attempting to include local plugin build $repo")

    val github = System.getenv("GITHUB_ACTIONS") == "true"

    var path = "../$repo"
    var file = File(path)

    if (github) {
        path = repo
        file = File(path)
        println("Running on GitHub")
    }

    if (file.exists()) {
        pluginManagement {
            includeBuild(path)
        }
        println("Included local plugin build $repo")
    } else {
        println("Local plugin build $repo not found")
    }
}

plugins {
    id("net.frozenblock.triangle.neoforge")
    id("org.quiltmc.gradle.licenser")
    checkstyle
}

checkstyle {
    configFile = rootProject.file("checkstyle.xml")
    toolVersion = "10.20.2"
}

val mod_id: String by project
val mod_version: String by project
val subproject_prefix: String by project
val minecraft_version: String by project
val maven_group: String by project
val archives_base_name: String by project

val frozenlib_version: String by project

val sodium_version: String by project
val run_sodium: String by project
val shouldRunSodium = run_sodium == "true"

val lambdynamiclights_version: String by project
val yumi_mc_foundation_version: String by project
val asyncparticles_version: String by project

val neoforgeSnapshotMaven = findProperty("neoforge_snapshot_maven") as String?

base {
    archivesName.set(archives_base_name)
}

group = maven_group

tasks.jar {
    archiveClassifier.set("neoforge")
}

repositories {
    maven("https://maven.neoforged.net/releases") { name = "NeoForged" }
    if (!neoforgeSnapshotMaven.isNullOrBlank()) {
        maven(neoforgeSnapshotMaven) { name = "NeoForge Snapshots" }
    }
    flatDir {
        dirs("libs")
    }
}

neoforge {
    dependOn(project(":$subproject_prefix-common"))
    accessWidener(project(":$subproject_prefix-common"))
}

neoForge {
    accessTransformers {} // Required for transitive AW to apply!
}

dependencies {
    // FrozenLib
    api("net.frozenblock:frozenlib-neoforge:$frozenlib_version")?.let {
        accessTransformers(it)
        interfaceInjectionData(it)
    }

    // LambDynamicLights
	compileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-runtime:$lambdynamiclights_version")
	compileOnly("dev.yumi.mc.core:yumi-mc-foundation:$yumi_mc_foundation_version")

    // Async Particles
	compileOnly("maven.modrinth:asyncparticles:$asyncparticles_version")

    // Sodium
    if (shouldRunSodium) {
        implementation("net.caffeinemc:sodium-neoforge-mod:$sodium_version")
        implementation("net.caffeinemc:sodium-neoforge:$sodium_version")
    } else {
        compileOnly("net.caffeinemc:sodium-neoforge-mod:$sodium_version")
        compileOnly("net.caffeinemc:sodium-neoforge:$sodium_version")
    }
}

val githubActions: Boolean = System.getenv("GITHUB_ACTIONS") == "true"
val licenseChecks: Boolean = githubActions

tasks {
    license {
        if (licenseChecks) {
            rule(rootProject.file("codeformat/HEADER"))

            include("**/*.java")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

val sourcesJar: Jar by tasks
val javadocJar: Jar by tasks

artifacts {
    archives(sourcesJar)
    archives(javadocJar)
}

val changelogText = run {
    val split = rootProject.file("CHANGELOG.md").readText().split("-----------------")
    check(split.size == 2) { "Malformed changelog" }
    split[1].trim()
}

upload {
    maven {
        name.set("$mod_id-neoforge")
    }

    forEach {
        changelog = changelogText
    }

    curseforge {
        dependencies {
            required("frozenlib")
            optional("wilder-wild")
            optional("trailier-tales")
            optional("the-copperier-age")
            optional("netherier-nether")
            optional("lambdynamiclights")
        }
    }

    modrinth {
        dependencies {
            required("frozenlib")
            optional("wilder-wild")
            optional("trailier-tales")
            optional("the-copperier-age")
            optional("netherier-nether")
            optional("lambdynamiclights")
        }
    }
}

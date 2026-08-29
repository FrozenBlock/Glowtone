plugins {
    id("net.frozenblock.triangle.fabric")
    id("org.quiltmc.gradle.licenser")
    checkstyle
}

checkstyle {
    configFile = rootProject.file("checkstyle.xml")
    toolVersion = "10.20.2"
}

val githubActions: Boolean = System.getenv("GITHUB_ACTIONS") == "true"
val licenseChecks: Boolean = githubActions

val fabric_loader_version: String by project
val min_fabric_loader_version: String by project

val mod_id: String by project
val mod_version: String by project
val minecraft_version: String by project
val maven_group: String by project
val archives_base_name: String by project

val fabric_api_version: String by project
val frozenlib_version: String by project

val sodium_version: String by project
val run_sodium: String by project
val shouldRunSodium = run_sodium == "true"

val lambdynamiclights_version: String by project
val yumi_commons_version: String by project
val yumi_mc_foundation_version: String by project
val spruceui_version: String by project
val pridelib_version: String by project
val run_lambdynamiclights: String by project
val shouldRunLambDynamicLights = run_lambdynamiclights == "true"

base {
    archivesName = archives_base_name
}

val release = findProperty("releaseType") == "stable"

version = getModVersion()
group = maven_group

tasks.jar {
    archiveClassifier.set("fabric")
}

fabric {
    dependOn(project(":gt-common"))
    accessWidener(project(":gt-common"))
    dataGen {
        owner = project(":gt-common")
        splitSourceSet("datagen")
    }
}

loom {
    enableTransitiveAccessWideners = true
    interfaceInjection {
        enableDependencyInterfaceInjection = true
    }
}

repositories {
    flatDir {
        dirs("libs")
    }
    maven {
        name = "Gegy"
        url = uri("https://maven.gegy.dev/releases/")
    }
}

val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)
val loaderVariants = setOf("apiElements", "runtimeElements", "sourcesElements", "javadocElements", "includeInternal", "modCompileClasspath")
configurations.all {
    if (name in loaderVariants) {
        attributes {
            attribute(loaderAttribute, "fabric")
        }
    }
}
sourceSets.configureEach {
    listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach { variant ->
        configurations.named(variant) {
            attributes {
                attribute(loaderAttribute, "fabric")
            }
        }
    }
}

dependencies {
    implementation("net.fabricmc:fabric-loader:${fabric_loader_version}")
    implementation("net.fabricmc.fabric-api:fabric-api:${fabric_api_version}")

    // FrozenLib
    api("net.frozenblock:frozenlib-fabric:${frozenlib_version}")

    // Sodium
    if (shouldRunSodium)
        implementation("net.caffeinemc:sodium-fabric:${sodium_version}")
    else
        compileOnly("net.caffeinemc:sodium-fabric:${sodium_version}")

    // LambDynamicLights
    if (shouldRunLambDynamicLights) {
        implementation("maven.modrinth:lambdynamiclights:${lambdynamiclights_version}")
        implementation("dev.lambdaurora.lambdynamiclights:lambdynamiclights-api:${lambdynamiclights_version}")

        implementation("dev.yumi.commons:yumi-commons-core:${yumi_commons_version}")
        implementation("dev.yumi.commons:yumi-commons-collections:${yumi_commons_version}")
        implementation("dev.yumi.commons:yumi-commons-event:${yumi_commons_version}")

        implementation("dev.yumi.mc.core:yumi-mc-foundation:${yumi_mc_foundation_version}")

        implementation("dev.lambdaurora:spruceui:${spruceui_version}")

        implementation("io.github.queerbric:pridelib:${pridelib_version}")
    } else {
        compileOnly("maven.modrinth:lambdynamiclights:${lambdynamiclights_version}")
        compileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-api:${lambdynamiclights_version}")

        compileOnly("dev.yumi.commons:yumi-commons-core:${yumi_commons_version}")
        compileOnly("dev.yumi.commons:yumi-commons-collections:${yumi_commons_version}")
        compileOnly("dev.yumi.commons:yumi-commons-event:${yumi_commons_version}")

        compileOnly("dev.yumi.mc.core:yumi-mc-foundation:${yumi_mc_foundation_version}")

        compileOnly("dev.lambdaurora:spruceui:${spruceui_version}")

        compileOnly("io.github.queerbric:pridelib:${pridelib_version}")
    }
}

tasks {
    processResources {
        val properties = mapOf(
            "mod_id" to mod_id,
            "version" to version,
            "minecraft_version" to "~26.2-",

            "fabric_loader_version" to ">=$min_fabric_loader_version",
            "fabric_api_version" to ">=$fabric_api_version",
            "frozenlib_version" to ">=${frozenlib_version.split('-').firstOrNull()}-"
        )

        properties.forEach { (a, b) -> inputs.property(a, b) }

        filesNotMatching(
            listOf(
                "**/*.java",
                "**/sounds.json",
                "**/lang/*.json",
                "**/.cache/*",
                "**/*.accesswidener",
                "**/*.classtweaker",
                "**/*.nbt",
                "**/*.png",
                "**/*.ogg",
                "**/*.mixins.json",
                "**/*.zip"
            )
        ) {
            expand(properties)
        }
    }

    license {
        if (licenseChecks) {
            rule(rootProject.file("codeformat/HEADER"))

            include("**/*.java")
        }
    }
}

val applyLicenses: Task by tasks
val test: Task by tasks
val runClient: Task by tasks

val sourcesJar: Jar by tasks
val javadocJar: Jar by tasks

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

artifacts {
    archives(sourcesJar)
    archives(javadocJar)
}

fun getModVersion(): String {
    var version = "$mod_version-mc$minecraft_version"

    if (!release) {
        version += "-unstable"
    }

    return version
}

val changelogText = run {
    val split = rootProject.file("CHANGELOG.md").readText().split("-----------------")
    check(split.size == 2) { "Malformed changelog" }
    split[1].trim()
}

upload {
    maven {
        name.set("glowtone-fabric")
    }

    forEach {
        changelog = changelogText
    }

    modrinth {
        dependencies {
            required("fabric-api")
            required("frozenlib")
            optional("wilder-wild")
            optional("trailier-tales")
            optional("the-copperier-age")
            optional("netherier-nether")
            optional("lambdynamiclights")
        }
    }
}

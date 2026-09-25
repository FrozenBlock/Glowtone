plugins {
    id("net.frozenblock.triangle.fabric")
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
val fabric_loader_version: String by project

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

val voxy_version: String by project
val run_voxy: String by project
val shouldRunVoxy = run_voxy == "true"

val nvidium_version: String by project
val run_nvidium: String by project
val shouldRunNvidium = run_nvidium == "true"

val asyncparticles_version: String by project
val run_asyncparticles: String by project
val shouldRunAsyncParticles = run_asyncparticles == "true"

base {
    archivesName = archives_base_name
}

version = getModVersion()
group = maven_group

tasks.jar {
    archiveClassifier.set("fabric")
}

fabric {
    dependOn(project(":$subproject_prefix-common"))
    accessWidener(project(":$subproject_prefix-common"))
    dataGen {
        owner = project(":$subproject_prefix-common")
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

dependencies {
    // Fabric
    implementation("net.fabricmc:fabric-loader:$fabric_loader_version")
    implementation("net.fabricmc.fabric-api:fabric-api:$fabric_api_version")

    // FrozenLib
    api("net.frozenblock:frozenlib-fabric:$frozenlib_version")

    // Sodium
    if (shouldRunSodium)
        implementation("net.caffeinemc:sodium-fabric:$sodium_version")
    else
        compileOnly("net.caffeinemc:sodium-fabric:$sodium_version")

    // LambDynamicLights
    if (shouldRunLambDynamicLights) {
        implementation("maven.modrinth:lambdynamiclights:$lambdynamiclights_version")
        implementation("dev.lambdaurora.lambdynamiclights:lambdynamiclights-api:$lambdynamiclights_version")

        implementation("dev.yumi.commons:yumi-commons-core:$yumi_commons_version")
        implementation("dev.yumi.commons:yumi-commons-collections:$yumi_commons_version")
        implementation("dev.yumi.commons:yumi-commons-event:$yumi_commons_version")

        implementation("dev.yumi.mc.core:yumi-mc-foundation:$yumi_mc_foundation_version")

        implementation("dev.lambdaurora:spruceui:$spruceui_version")

        implementation("io.github.queerbric:pridelib:$pridelib_version")
    } else {
        compileOnly("maven.modrinth:lambdynamiclights:$lambdynamiclights_version")
        compileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-api:$lambdynamiclights_version")

        compileOnly("dev.yumi.commons:yumi-commons-core:$yumi_commons_version")
        compileOnly("dev.yumi.commons:yumi-commons-collections:$yumi_commons_version")
        compileOnly("dev.yumi.commons:yumi-commons-event:$yumi_commons_version")

        compileOnly("dev.yumi.mc.core:yumi-mc-foundation:$yumi_mc_foundation_version")

        compileOnly("dev.lambdaurora:spruceui:$spruceui_version")

        compileOnly("io.github.queerbric:pridelib:$pridelib_version")
    }

    // Voxy
    if (shouldRunVoxy) implementation("maven.modrinth:voxy:$voxy_version")

    // Nvidium
    if (shouldRunNvidium) implementation("maven.modrinth:nvidium:$nvidium_version")

    // Async Particles
    if (shouldRunAsyncParticles)
        implementation("maven.modrinth:asyncparticles:$asyncparticles_version")
    else
        compileOnly("maven.modrinth:asyncparticles:$asyncparticles_version")
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

val release = findProperty("releaseType") == "stable"

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
        name.set("$mod_id-fabric")
    }

    forEach {
        changelog = changelogText
    }

    curseforge {
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

plugins {
    id("net.frozenblock.triangle.common")
    id("org.quiltmc.gradle.licenser")
    checkstyle
}

checkstyle {
    configFile = rootProject.file("checkstyle.xml")
    toolVersion = "10.20.2"
}

val mod_id: String by project
val frozenlib_version: String by project
val sodium_version: String by project
val lambdynamiclights_version: String by project
val asyncparticles_version: String by project
val yumi_mc_foundation_version: String by project

common {
    accessWidener()
}

neoForge {
    accessTransformers {} // Required for transitive AW to apply!
}

tasks {
    license {
        if (licenseChecks) {
            rule(rootProject.file("codeformat/HEADER"))

            include("**/*.java")
        }
    }
}

dependencies {
    // FrozenLib
    compileOnly("net.frozenblock:frozenlib-common:$frozenlib_version")?.let {
        accessTransformers(it)
        interfaceInjectionData(it)
    }

    // Sodium
    compileOnly("net.caffeinemc:sodium-fabric:$sodium_version")

    // LambDynamicLights
    compileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-runtime:$lambdynamiclights_version")
    compileOnly("dev.yumi.mc.core:yumi-mc-foundation:$yumi_mc_foundation_version")

    // Async Particles
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

configurations {
    create("commonJava") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
    create("commonResources") {
        isCanBeResolved = false
        isCanBeConsumed = true
    }
}

upload.maven {
    name.set("$mod_id-common")
}

plugins {
    id("net.frozenblock.triangle.common")
    id("org.quiltmc.gradle.licenser")
    checkstyle
}

checkstyle {
    configFile = rootProject.file("checkstyle.xml")
    toolVersion = "10.20.2"
}

val frozenlib_version: String by project
val sodium_version: String by project
val lambdynamiclights_version: String by project

val githubActions: Boolean = System.getenv("GITHUB_ACTIONS") == "true"
val licenseChecks: Boolean = githubActions

val applyLicenses: Task by tasks

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
    compileOnly("net.frozenblock:frozenlib-common:${frozenlib_version}")?.let {
        accessTransformers(it)
        interfaceInjectionData(it)
    }

    compileOnly("net.fabricmc:sponge-mixin:0.17.3+mixin.0.8.7")
    compileOnly("io.github.llamalad7:mixinextras-common:0.5.3")
    annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.3")

    compileOnly("net.caffeinemc:sodium-fabric:${sodium_version}")
    compileOnly("dev.lambdaurora.lambdynamiclights:lambdynamiclights-runtime:${lambdynamiclights_version}")
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
    name.set("glowtone-common")
}

import org.kohsuke.github.GHReleaseBuilder
import org.kohsuke.github.GitHub

plugins {
    id("net.frozenblock.triangle.core") version("+")
    id("net.frozenblock.triangle.common") version("+") apply(false)
    id("net.frozenblock.triangle.fabric") version("+") apply(false)
    id("net.frozenblock.triangle.neoforge") version("+") apply(false)
    id("net.mehvahdjukaar.candlelight") version("+") apply(false)

    id("org.quiltmc.gradle.licenser") version("+") apply(false)
    checkstyle
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.kohsuke:github-api:1.326")
    }
}

checkstyle {
    configFile = rootProject.file("checkstyle.xml")
    toolVersion = "10.20.2"
}

val min_fabric_loader_version: String by project

mod {
    additional.add("fabric_loader_version", ">=$min_fabric_loader_version")
    additional.add("minecraft_version", "~26.2-")
    additional.add("mod_description")
}

val changelogText = run {
    val split = file("CHANGELOG.md").readText().split("-----------------")
    check(split.size == 2) { "Malformed changelog" }
    split[1].trim()
}

fun mainJarTask(project: Project) =
    if (project.tasks.names.contains("shadowJar")) project.tasks.named("shadowJar")
    else project.tasks.named("jar")

val githubRelease by tasks.registering {
    val fabricJar = mainJarTask(project(":gt-fabric"))
    dependsOn(fabricJar)

    val token = env["GITHUB_TOKEN"]
    val repository = mod.repository.get()
    val tag = project(":gt-fabric").version.toString()
    val releaseTitle = "Glowtone $tag"
    val isPrerelease = mod.releaseType.get() != "release"
    val commitish = env["GITHUB_SHA"]

    onlyIf { !token.isNullOrEmpty() }

    doLast {
        val github = GitHub.connectUsingOAuth(token)
        val repo = github.getRepository(repository)

        repo.getReleaseByTagName(tag)?.delete()

        val releaseBuilder = GHReleaseBuilder(repo, tag)
        releaseBuilder.name(releaseTitle)
        releaseBuilder.body(changelogText)
        releaseBuilder.prerelease(isPrerelease)
        if (commitish != null) releaseBuilder.commitish(commitish)

        val release = releaseBuilder.create()
        release.uploadAsset(fabricJar.get().outputs.files.singleFile, "application/java-archive")
    }
}

val publishMod by tasks.registering {
    dependsOn(tasks.named("upload"))
    dependsOn(githubRelease)
}

subprojects {
    apply(plugin = "net.frozenblock.triangle.core")
    apply(plugin = "net.mehvahdjukaar.candlelight")

    if (project.name != "gt-common") {
        afterEvaluate {
            tasks.findByName("compileJava")?.dependsOn(":gt-common:candleLightTransform")
        }
    }

    val mavenUrl = env["MAVEN_URL"]
    val mavenUsername = env["MAVEN_USERNAME"]
    val mavenPassword = env["MAVEN_PASSWORD"]

    if (mavenUrl != null && mavenUsername != null && mavenPassword != null) {
        upload {
            maven {
                repositories {
                    maven(mavenUrl) {
                        name = "FrozenBlock"
                        credentials {
                            username = mavenUsername
                            password = mavenPassword
                        }
                    }
                }
            }
        }
    }

    tasks.withType<JavaCompile> {
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "4000"))
        options.release.set(25)
    }

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    dependencies {
        compileOnly("net.mehvahdjukaar:candlelight:+")
    }

    repositories {
        maven("https://maven.frozenblock.net/release") {
            name = "FrozenBlock"
        }
        maven("https://maven.frozenblock.net/snapshot") {
            name = "FrozenBlock Snapshot"
        }
        maven("https://maven.minecraftforge.net/") {
            name = "Forge"
        }
        maven("https://registry.somethingcatchy.net/repository/maven-releases/") { // Candlelight & Triangle
            name = "SomethingCatchy (MehVahdJukaar)"
        }
        maven("https://maven.quiltmc.org/repository/release") {
            name = "Quilt"
        }

        exclusiveContent {
            forRepository {
                maven("https://api.modrinth.com/maven") {
                    name = "Modrinth"
                }
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }
        maven("https://jitpack.io") {
            name = "Jitpack"
        }
        mavenCentral()
    }

    tasks {
        withType(JavaCompile::class) {
            options.encoding = "UTF-8"
            options.release.set(25)
            options.isFork = true
            options.isIncremental = true
        }

        withType(Test::class) {
            maxParallelForks = Runtime.getRuntime().availableProcessors().div(2)
        }
    }
}

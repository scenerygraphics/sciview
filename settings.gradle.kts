pluginManagement {
    val kotlinVersion: String by settings
    val dokkaVersion: String by settings

    plugins {
        kotlin("jvm") version kotlinVersion
        kotlin("kapt") version kotlinVersion
        id("org.jetbrains.dokka") version dokkaVersion

        id("com.github.johnrengelman.shadow") version "8.1.1"
    }

    repositories {
        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
    }
}

rootProject.name = "sciview"

gradle.rootProject {
    group = "sc.iview"
    version = "0.2.0-beta-9-SNAPSHOT"
    description = "Scenery-backed 3D visualization package for ImageJ."
}

val useLocalScenery: String? by extra
if (System.getProperty("CI").toBoolean() != true
    && System.getenv("CI").toBoolean() != true
    && useLocalScenery?.toBoolean() == true)
    if(File("../scenery/build.gradle.kts").exists()) {
        logger.warn("Including local scenery project instead of version declared in build, set -PuseLocalScenery=false to use declared version instead.")
        includeBuild("../scenery")
    }

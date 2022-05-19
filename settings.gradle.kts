
pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
    }
}

plugins {
    id("sciJava.catalogs") version "30.0.0+66"
}

rootProject.name = "sciview"

gradle.rootProject {
    group = "graphics.scenery"
    version = "0.2.0-beta-9-SNAPSHOT-test-4"
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
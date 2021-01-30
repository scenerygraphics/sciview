pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

rootProject.name = "sciview"

gradle.rootProject {
    //    group = "scenery"
    version = "0.2.0-beta-9-SNAPSHOT"
    description = "Scenery-backed 3D visualization package for ImageJ."
}

//includeBuild("sciJava")


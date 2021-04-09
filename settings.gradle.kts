rootProject.name = "sciview"

gradle.rootProject {
    group = "sc.iview"
    version = "0.2.0-beta-9-SNAPSHOT_test"
    description = "Scenery-backed 3D visualization package for ImageJ."
}

//if (System.getProperty("CI") == "false")
//    includeBuild("../scenery")
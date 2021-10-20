import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sciview.implementation
import sciview.joglNatives
import java.net.URL
import sciview.*

//System.setProperty("scijava.platform.family.long", "linux")

plugins {
    val ktVersion = "1.5.10"
    val dokkaVersion = "1.4.32"

    java
    kotlin("jvm") version ktVersion
    kotlin("kapt") version ktVersion
    sciview.publish
    sciview.sign
    id("org.jetbrains.dokka") version dokkaVersion
    jacoco
    `maven-publish`
    `java-library`
    signing
}

repositories {
    mavenCentral()
    maven("https://maven.scijava.org/content/groups/public")
    maven("https://jitpack.io")
}

dependencies {

    implementation(platform("org.scijava:pom-scijava:31.1.0"))

    // Graphics dependencies

    annotationProcessor("org.scijava:scijava-common:2.87.0")
    kapt("org.scijava:scijava-common:2.87.0") { // MANUAL version increment
        exclude("org.lwjgl")
    }

    val sceneryVersion = "9017a37"
    api("graphics.scenery:scenery:$sceneryVersion") { version { strictly(sceneryVersion) } }
    api("graphics.scenery:spirvcrossj:0.8.0-1.1.106.0", lwjglNatives)
    // check if build is triggered on https://jitpack.io/#scenerygraphics/sciview `build` tab
    // if not, uncomment this only to trigger it
    //    api("com.github.scenerygraphics:scenery:$scenery")

    // This seams to be still necessary
    implementation(platform("org.lwjgl:lwjgl-bom:3.2.3"))
    listOf("", "-glfw", "-jemalloc", "-vulkan", "-opengl", "-openvr", "-xxhash", "-remotery").forEach { lwjglProject ->
        api("org.lwjgl:lwjgl$lwjglProject:3.2.3")

        if (lwjglProject != "-vulkan") {
            lwjglNatives.forEach { native ->
                runtimeOnly("org.lwjgl:lwjgl$lwjglProject:3.2.3:$native")
            }
        }
    }
    //    listOf("", "-glfw", "-jemalloc", "-vulkan", "-opengl", "-openvr", "-xxhash", "-remotery").forEach { lwjglProject ->
    //        if (lwjglProject == "-vulkan")
    //                api("org.lwjgl:lwjgl$lwjglProject:3.2.3")
    //        else {
    //            lwjglNatives.forEach { native ->
    //                //api("org.lwjgl:lwjgl$lwjglProject:3.2.3-$native")
    //                api("org.lwjgl", "lwjgl$lwjglProject", "3.2.3", null, null, "$native.jar", null)
    //            }
    //        }
    //    }
    //implementation(jackson.bundles.all)

    implementation("com.fasterxml.jackson.core:jackson-databind:2.12.5")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.5")
    implementation("org.msgpack:jackson-dataformat-msgpack:0.8.20")

    implementation(misc.cleargl)
    implementation(misc.coreMem)
    implementation(jogamp.jogl, joglNatives)

    implementation("com.formdev:flatlaf:0.38")

    // SciJava dependencies

    implementation("org.scijava:scijava-common")
    implementation("org.scijava:ui-behaviour")
    implementation("org.scijava:script-editor")
    implementation("org.scijava:scijava-ui-swing")
    implementation("org.scijava:scijava-ui-awt")
    implementation("org.scijava:scijava-search")
    implementation("org.scijava:scripting-jython")
    implementation(migLayout.swing)

    // ImageJ dependencies

    implementation("net.imagej:imagej-common")
    implementation("net.imagej:imagej-mesh:0.8.1")
    implementation("net.imagej:imagej-mesh-io")
    implementation("net.imagej:imagej-ops")
    implementation("net.imagej:imagej-launcher")
    implementation("net.imagej:imagej-ui-swing")
    implementation("net.imagej:imagej-legacy")
    implementation("io.scif:scifio")
    implementation("io.scif:scifio-bf-compat")

    // ImgLib2 dependencies
    implementation("net.imglib2:imglib2")
    implementation("net.imglib2:imglib2-roi")

    // Math dependencies
    implementation(commons.math3)
    implementation(misc.joml)

    // Kotlin dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:1.5.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.10")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    // Test scope

    testImplementation(misc.junit4)
    implementation("net.imagej:ij")
    implementation("net.imglib2:imglib2-ij")

    implementation(n5.core)
    implementation(n5.hdf5)
    implementation(n5.imglib2)
    //    implementation("org.janelia.saalfeldlab:n5-aws-s3")
    //    implementation("org.janelia.saalfeldlab:n5-ij:2.0.1-SNAPSHOT")
    implementation("sc.fiji:spim_data")

    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test-junit"))

    implementation("sc.fiji:bigdataviewer-core")
    implementation("sc.fiji:bigdataviewer-vistools")

    // OME
    implementation("ome:formats-bsd")
    implementation("ome:formats-gpl")


}

//kapt {
//    useBuildCache = false // safe
//    arguments {
//        arg("-Werror")
//        arg("-Xopt-in", "kotlin.RequiresOptIn")
//    }
//}

tasks {
    withType<KotlinCompile>().all {
        val version = System.getProperty("java.version").substringBefore('.').toInt()
        val default = if (version == 1) "1.8" else "$version"
        kotlinOptions {
            jvmTarget = project.properties["jvmTarget"]?.toString() ?: default
            freeCompilerArgs += listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
        }
        sourceCompatibility = project.properties["sourceCompatibility"]?.toString() ?: default
    }
    test {
        finalizedBy(jacocoTestReport) // report is always generated after tests run
    }
    jar {
        archiveVersion.set(rootProject.version.toString())
    }

    withType<GenerateMavenPom>().configureEach {
        val matcher = Regex("""generatePomFileFor(\w+)Publication""").matchEntire(name)
        val publicationName = matcher?.let { it.groupValues[1] }

        pom.properties.empty()

        pom.withXml {
            // Add parent to the generated pom
            var parent = asNode().appendNode("parent")
            parent.appendNode("groupId", "org.scijava")
            parent.appendNode("artifactId", "pom-scijava")
            parent.appendNode("version", "31.1.0")
            parent.appendNode("relativePath")

            var repositories = asNode().appendNode("repositories")
            var jitpackRepo = repositories.appendNode("repository")
            jitpackRepo.appendNode("id", "jitpack.io")
            jitpackRepo.appendNode("url", "https://jitpack.io")

            var scijavaRepo = repositories.appendNode("repository")
            scijavaRepo.appendNode("id", "scijava.public")
            scijavaRepo.appendNode("url", "https://maven.scijava.org/content/groups/public")

            // Update the dependencies and properties
            var dependenciesNode = asNode().appendNode("dependencies")
            var propertiesNode = asNode().appendNode("properties")
            propertiesNode.appendNode("inceptionYear", 2016)

            // spirvcrossj natives
            lwjglNatives.forEach {
                var dependencyNode = dependenciesNode.appendNode("dependency")
                dependencyNode.appendNode("groupId", "graphics.scenery")
                dependencyNode.appendNode("artifactId", "spirvcrossj")
                dependencyNode.appendNode("version", "\${spirvcrossj.version}")
                dependencyNode.appendNode("classifier", "$it")
                dependencyNode.appendNode("scope", "runtime")
            }

            // lwjgl natives
            lwjglNatives.forEach { nativePlatform ->
                listOf(
                    "",
                    "-glfw",
                    "-jemalloc",
                    "-opengl",
                    "-openvr",
                    "-xxhash",
                    "-remotery"
                ).forEach { lwjglProject ->
                    var dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", "org.lwjgl")
                    dependencyNode.appendNode("artifactId", "lwjgl$lwjglProject")
                    dependencyNode.appendNode("version", "\${lwjgl.version}")
                    dependencyNode.appendNode("classifier", "$nativePlatform")
                    dependencyNode.appendNode("scope", "runtime")
                }
            }

            // jvrpn natives
            lwjglNatives.forEach {
                var dependencyNode = dependenciesNode.appendNode("dependency")
                dependencyNode.appendNode("groupId", "graphics.scenery")
                dependencyNode.appendNode("artifactId", "jvrpn")
                dependencyNode.appendNode("version", "\${jvrpn.version}")
                dependencyNode.appendNode("classifier", "$it")
                dependencyNode.appendNode("scope", "runtime")
            }
            // add jvrpn property because it only has runtime native deps
            propertiesNode.appendNode("jvrpn.version", "1.2.0")

            val versionedArtifacts = listOf("scenery",
                                            "flatlaf",
                                            "kotlin-stdlib-common",
                                            "kotlin-stdlib",
                                            "kotlinx-coroutines-core",
                                            "spirvcrossj",
                                            "pom-scijava",
                                            "lwjgl-bom",
                                            "jackson-module-kotlin",
                                            "jackson-dataformat-yaml",
                                            "jackson-dataformat-msgpack",
                                            "jogl-all",
                                            "kotlin-bom",
                                            "lwjgl",
                                            "lwjgl-glfw",
                                            "lwjgl-jemalloc",
                                            "lwjgl-vulkan",
                                            "lwjgl-opengl",
                                            "lwjgl-openvr",
                                            "lwjgl-xxhash",
                                            "lwjgl-remotery")

            val toSkip = listOf("pom-scijava")

            configurations.implementation.allDependencies.forEach {
                var artifactId = it.name

                if (!toSkip.contains(artifactId)) {

                    var propertyName = "$artifactId.version"

                    if (versionedArtifacts.contains(artifactId)) {
                        // add "<artifactid.version>[version]</artifactid.version>" to pom
                        propertiesNode.appendNode(propertyName, it.version)
                    }

                    var dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", artifactId)
                    dependencyNode.appendNode("version", "\${$propertyName}")

                    // Custom per artifact tweaks
                    println(artifactId)
                    if ("\\-bom".toRegex().find(artifactId) != null) {
                        dependencyNode.appendNode("type", "pom")
                    }
                    // from https://github.com/scenerygraphics/sciview/pull/399#issuecomment-904732945
                    if (artifactId == "formats-gpl") {
                        var exclusions = dependencyNode.appendNode("exclusions")
                        var jacksonCore = exclusions.appendNode("exclusion")
                        jacksonCore.appendNode("groupId", "com.fasterxml.jackson.core")
                        jacksonCore.appendNode("artifactId", "jackson-core")
                        var jacksonAnnotations = exclusions.appendNode("exclusion")
                        jacksonAnnotations.appendNode("groupId", "com.fasterxml.jackson.core")
                        jacksonAnnotations.appendNode("artifactId", "jackson-annotations")
                    }
                    //dependencyNode.appendNode("scope", it.scope)
                }
            }

            var depStartIdx = "<dependencyManagement>".toRegex().find(asString())?.range?.start
            var depEndIdx = "</dependencyManagement>".toRegex().find(asString())?.range?.last
            if (depStartIdx != null) {
                if (depEndIdx != null) {
                    asString().replace(depStartIdx, depEndIdx + 1, "")
                }
            }

            depStartIdx = "<dependencies>".toRegex().find(asString())?.range?.start
            depEndIdx = "</dependencies>".toRegex().find(asString())?.range?.last
            if (depStartIdx != null) {
                if (depEndIdx != null) {
                    asString().replace(depStartIdx, depEndIdx + 1, "")
                }
            }
        }
    }

    dokkaHtml {
        enabled = false
        dokkaSourceSets.configureEach {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/scenerygraphics/sciview/tree/master/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }

    dokkaJavadoc {
        enabled = false
    }

    jacocoTestReport {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
        dependsOn(test) // tests are required to run before generating the report
    }

    register("runMain", JavaExec::class.java) {
        classpath = sourceSets.main.get().runtimeClasspath

        main = "sc.iview.Main"

        val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("scenery.") }

        val additionalArgs = System.getenv("SCENERY_JVM_ARGS")
        allJvmArgs = if (additionalArgs != null) {
            allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") } + additionalArgs
        } else {
            allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") }
        }
    }

    register("runImageJMain", JavaExec::class.java) {
        classpath = sourceSets.main.get().runtimeClasspath

        main = "sc.iview.ImageJMain"

        val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("scenery.") }

        val additionalArgs = System.getenv("SCENERY_JVM_ARGS")
        allJvmArgs = if (additionalArgs != null) {
            allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") } + additionalArgs
        } else {
            allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") }
        }
    }

    sourceSets.main.get().allSource.files
        .filter { it.path.contains("demo") && (it.name.endsWith(".kt") || it.name.endsWith(".java")) }
        .map {
            val p = it.path
            if (p.endsWith(".kt")) {
                p.substringAfter("kotlin${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".kt")
            } else {
                p.substringAfter("java${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".java")
            }
        }
        .forEach { className ->
            println("Working on $className")
            val exampleName = className.substringAfterLast(".")
            val exampleType = className.substringBeforeLast(".").substringAfterLast(".")

            println("Registering $exampleName of $exampleType")
            register<JavaExec>(name = className.substringAfterLast(".")) {
                classpath = sourceSets.test.get().runtimeClasspath
                main = className
                group = "demos.$exampleType"

                val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("scenery.") }

                val additionalArgs = System.getenv("SCENERY_JVM_ARGS")
                allJvmArgs = if (additionalArgs != null) {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") } + additionalArgs
                } else {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") }
                }
            }
        }

    register<JavaExec>(name = "run") {
        classpath = sourceSets.main.get().runtimeClasspath
        if (project.hasProperty("target")) {
            project.property("target")?.let { target ->
                classpath = sourceSets.test.get().runtimeClasspath

                println("Target is $target")
                //                if(target.endsWith(".kt")) {
                //                    main = target.substringAfter("kotlin${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".kt")
                //                } else {
                //                    main = target.substringAfter("java${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".java")
                //                }

                main = "$target"
                val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("scenery.") }

                val additionalArgs = System.getenv("SCENERY_JVM_ARGS")
                allJvmArgs = if (additionalArgs != null) {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") } + additionalArgs
                } else {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") }
                }

                println("Will run target $target with classpath $classpath, main=$main")
                println("JVM arguments passed to target: $allJvmArgs")
            }
        }
    }
}

val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
    dependsOn(tasks.dokkaJavadoc)
    from(tasks.dokkaJavadoc.get().outputDirectory.get())
    archiveClassifier.set("javadoc")
}

val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
    dependsOn(tasks.dokkaHtml)
    from(tasks.dokkaHtml.get().outputDirectory.get())
    archiveClassifier.set("html-doc")
}

jacoco {
    toolVersion = "0.8.7"
}

artifacts {
    archives(dokkaJavadocJar)
    archives(dokkaHtmlJar)
}

java.withSourcesJar()


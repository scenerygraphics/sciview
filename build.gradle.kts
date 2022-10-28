import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sciview.implementation
import sciview.joglNatives
import java.net.URL
import sciview.*

plugins {
    val ktVersion = "1.6.10"
    val dokkaVersion = "1.6.0"

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
    val ktVersion = "1.6.10"
    implementation(platform("org.scijava:pom-scijava:31.1.0"))

    // Graphics dependencies

    annotationProcessor("org.scijava:scijava-common:2.87.1")
    kapt("org.scijava:scijava-common:2.87.1") { // MANUAL version increment
        exclude("org.lwjgl")
    }


    val sceneryVersion = "4055b8eb32"
    //val sceneryVersion = "ce77dda497"

    api("graphics.scenery:scenery:$sceneryVersion")
    // check if build is triggered on https://jitpack.io/#scenerygraphics/sciview `build` tab
    // if not, uncomment this only to trigger it
//    api("com.github.scenerygraphics:scenery:$sceneryVersion")

    api("org.apache.logging.log4j:log4j-api:2.19.0")
    api("org.apache.logging.log4j:log4j-core:2.19.0")
    api("org.apache.logging.log4j:log4j-1.2-api:2.19.0")

    api("org.slf4j:slf4j-api:1.7.36")
    api("org.slf4j:slf4j-simple:1.7.36")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.1")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.1")
    implementation("org.msgpack:jackson-dataformat-msgpack:0.9.0")

    implementation(misc.cleargl)
    implementation(misc.coreMem)
    implementation(jogamp.jogl, joglNatives)

    implementation("com.formdev:flatlaf:1.6.5")

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
    api("net.imagej:imagej-mesh:0.8.1")
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$ktVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$ktVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")

    // Test scope

    testImplementation(misc.junit4)
    implementation("net.imagej:ij")
    implementation("net.imglib2:imglib2-ij")

    implementation(n5.core)
    implementation(n5.hdf5)
    implementation(n5.imglib2)
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
            val parent = asNode().appendNode("parent")
            parent.appendNode("groupId", "org.scijava")
            parent.appendNode("artifactId", "pom-scijava")
            parent.appendNode("version", "31.1.0")
            parent.appendNode("relativePath")

            val repositories = asNode().appendNode("repositories")
            val jitpackRepo = repositories.appendNode("repository")
            jitpackRepo.appendNode("id", "jitpack.io")
            jitpackRepo.appendNode("url", "https://jitpack.io")

            val scijavaRepo = repositories.appendNode("repository")
            scijavaRepo.appendNode("id", "scijava.public")
            scijavaRepo.appendNode("url", "https://maven.scijava.org/content/groups/public")

            // Update the dependencies and properties
            val dependenciesNode = asNode().appendNode("dependencies")
            val propertiesNode = asNode().appendNode("properties")
            propertiesNode.appendNode("inceptionYear", 2016)

            // lwjgl natives
            lwjglNatives.forEach { nativePlatform ->
                listOf(
                    "",
                    "-glfw",
                    "-jemalloc",
                    "-opengl",
                    "-vulkan",
                    "-openvr",
                    "-xxhash",
                    "-remotery",
                    "-spvc",
                    "-shaderc",
                ).forEach project@ { lwjglProject ->
                    // Vulkan natives only exist for macOS
                    if(lwjglProject.endsWith("vulkan") && nativePlatform != "macos") {
                        return@project
                    }

                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", "org.lwjgl")
                    dependencyNode.appendNode("artifactId", "lwjgl$lwjglProject")
                    dependencyNode.appendNode("version", "\${lwjgl.version}")
                    dependencyNode.appendNode("classifier", nativePlatform)
                    dependencyNode.appendNode("scope", "runtime")
                }
            }

            // jvrpn natives
            lwjglNatives.forEach { classifier ->
                val dependencyNode = dependenciesNode.appendNode("dependency")
                dependencyNode.appendNode("groupId", "graphics.scenery")
                dependencyNode.appendNode("artifactId", "jvrpn")
                dependencyNode.appendNode("version", "\${jvrpn.version}")
                dependencyNode.appendNode("classifier", classifier)
                dependencyNode.appendNode("scope", "runtime")
            }
            // add lwjgl version explicitly
            propertiesNode.appendNode("lwjgl.version", "3.3.1")
            // add jvrpn property because it only has runtime native deps
            propertiesNode.appendNode("jvrpn.version", "1.2.0")

            val versionedArtifacts = listOf("scenery",
                                            "flatlaf",
                                            "kotlin-stdlib-common",
                                            "kotlin-stdlib",
                                            "kotlinx-coroutines-core",
                                            "pom-scijava",
                                            "lwjgl-bom",
                                            "imagej-mesh",
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
                                            "lwjgl-remotery",
                                            "lwjgl-spvc",
                                            "lwjgl-shaderc")

            val toSkip = listOf("pom-scijava")

            configurations.implementation.allDependencies.forEach {
                val artifactId = it.name

                if (!toSkip.contains(artifactId)) {

                    val propertyName = "$artifactId.version"

                    if (versionedArtifacts.contains(artifactId)) {
                        // add "<artifactid.version>[version]</artifactid.version>" to pom
                        propertiesNode.appendNode(propertyName, it.version)
                    }

                    val dependencyNode = dependenciesNode.appendNode("dependency")
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
                        val exclusions = dependencyNode.appendNode("exclusions")
                        val jacksonCore = exclusions.appendNode("exclusion")
                        jacksonCore.appendNode("groupId", "com.fasterxml.jackson.core")
                        jacksonCore.appendNode("artifactId", "jackson-core")
                        val jacksonAnnotations = exclusions.appendNode("exclusion")
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


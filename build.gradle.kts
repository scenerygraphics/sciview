import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import sciview.*

// Read versions from properties file.
val versions: Map<String, String> = project.properties.
    filter { (k, _) -> k.endsWith(".version") }.
    map { (k, v) -> k.removeSuffix(".version") to v as String }.
    toMap()
fun v(k: String): String {
  return versions[k] ?: throw RuntimeException("No version for " + k)
}

plugins {
    java
    // Kotlin/Dokka versions are managed in gradle.properties
    kotlin("jvm")
    kotlin("kapt")
    sciview.publish
    sciview.sign
    id("org.jetbrains.dokka")
    jacoco
    `maven-publish`
    `java-library`
    signing
    id("ca.cutterslade.analyze")
}

java {
  sourceCompatibility = JavaVersion.VERSION_11
  targetCompatibility = JavaVersion.VERSION_11
}

repositories {
    if(project.properties["useMavenLocal"] == "true") {
        logger.warn("Using local Maven repository as source")
        mavenLocal()
    }
    mavenCentral()
    maven("https://maven.scijava.org/content/groups/public")
}

dependencies {
    // Bills of Materials
    implementation(platform("org.scijava:pom-scijava:${v("pom-scijava")}"))
    implementation(platform(kotlin("bom")))

    // Annotation processing
    annotationProcessor("org.scijava:scijava-common:${v("scijava-common")}")
    kapt("org.scijava:scijava-common:${v("scijava-common")}") {
        exclude("org.lwjgl")
    }

    // Kotlin dependencies
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    // Graphics dependencies
    api("graphics.scenery:scenery") {
        version { strictly(v("scenery")) }
    }

    // SciJava dependencies
    implementation("org.scijava:scijava-common")
    implementation("org.scijava:ui-behaviour")
    implementation("org.scijava:script-editor")
    implementation("org.scijava:scijava-ui-swing")
    implementation("org.scijava:scijava-ui-awt")
    implementation("org.scijava:scijava-search")
    implementation("org.scijava:scripting-jython")

    // ImageJ dependencies
    api("net.imagej:imagej-mesh")
    implementation("net.imagej:imagej-common")
    implementation("net.imagej:imagej-launcher")
    implementation("net.imagej:imagej-mesh-io")
    implementation("net.imagej:imagej-ops")
    implementation("net.imagej:imagej-ui-swing")

    // SCIFIO dependencies
    implementation("io.scif:scifio")
    implementation("io.scif:scifio-bf-compat")

    // ImgLib2 dependencies
    implementation("net.imglib2:imglib2")
    implementation("net.imglib2:imglib2-algorithm")
    implementation("net.imglib2:imglib2-realtransform")
    implementation("net.imglib2:imglib2-roi")

    // BigDataViewer dependencies
    implementation("sc.fiji:bigdataviewer-core")
    implementation("sc.fiji:bigdataviewer-vistools")
    implementation("sc.fiji:spim_data")

    // N5 dependencies
    implementation("org.janelia.saalfeldlab:n5")
    implementation("org.janelia.saalfeldlab:n5-hdf5")
    implementation("org.janelia.saalfeldlab:n5-imglib2")

    // Third party dependencies
    implementation("com.formdev:flatlaf")
    implementation("dev.dirs:directories") // XDG support
    implementation("net.java.dev.jna:jna")
    implementation("org.apache.logging.log4j:log4j-1.2-api:${v("log4j-1.2-api")}")
    implementation("org.apache.logging.log4j:log4j-api")
    implementation("org.slf4j:slf4j-simple")
    implementation("org.yaml:snakeyaml") {
        version { strictly(v("snakeyaml")) }
    }

    // Runtime dependencies
    runtimeOnly("io.scif:scifio-bf-compat")
    runtimeOnly("net.imagej:imagej-launcher")
    runtimeOnly("net.java.dev.jna:jna-platform")
    runtimeOnly("ome:formats-bsd")
    runtimeOnly("org.scijava:scripting-jython")

    // Test dependencies
    testImplementation("net.clearvolume:cleargl")
    testImplementation("org.slf4j:slf4j-simple")
    testImplementation(kotlin("test-junit"))
    testImplementation(kotlin("test-junit"))
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
        val default = if (version == 1) "11" else "$version"
        kotlinOptions {
            jvmTarget = project.properties["jvmTarget"]?.toString() ?: default
            freeCompilerArgs += listOf("-Xinline-classes", "-opt-in=kotlin.RequiresOptIn")
        }
//        sourceCompatibility = project.properties["sourceCompatibility"]?.toString() ?: default
    }
    test {
        finalizedBy(jacocoTestReport) // report is always generated after tests run
    }
    jar {
        archiveVersion.set(rootProject.version.toString())
    }
    analyzeClassesDependencies {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
    }
    analyzeTestClassesDependencies {
        warnUsedUndeclared = true
        warnUnusedDeclared = true
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
            parent.appendNode("version", v("pom-scijava"))
            parent.appendNode("relativePath")

            val repositories = asNode().appendNode("repositories")

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
            // add jvrpn property because it only has runtime native deps
            propertiesNode.appendNode("jvrpn.version", v("jvrpn"))

            // add correct lwjgl version
            propertiesNode.appendNode("lwjgl.version", v("lwjgl"))

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
                                            "jna-platform",
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

            configurations.implementation.get().allDependencies.forEach {
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
                remoteUrl.set(URL("https://github.com/scenerygraphics/sciview/tree/main/src/main/kotlin"))
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

        mainClass.set("sc.iview.Main")

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

        mainClass.set("sc.iview.ImageJMain")

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
                mainClass.set(className)
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

                mainClass.set("$target")
                val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("scenery.") }

                val additionalArgs = System.getenv("SCENERY_JVM_ARGS")
                allJvmArgs = if (additionalArgs != null) {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") } + additionalArgs
                } else {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") }
                }

                println("Will run target $target with classpath $classpath, main=${mainClass.get()}")
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
    toolVersion = v("jacoco")
}

artifacts {
    archives(dokkaJavadocJar)
    archives(dokkaHtmlJar)
}



java.withSourcesJar()

import org.gradle.kotlin.dsl.implementation
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL
import sciview.*

plugins {
    java
    // Kotlin/Dokka versions are managed in gradle.properties
    kotlin("jvm")
    kotlin("kapt")
    sciview.publish
    sciview.sign
    sciview.fiji
    id("org.jetbrains.dokka")
    jacoco
    `maven-publish`
    `java-library`
    signing
}

java {
  sourceCompatibility = JavaVersion.VERSION_21
  targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    if(project.properties["useMavenLocal"] == "true") {
        logger.warn("Using local Maven repository as source")
        mavenLocal()
    }
    maven("https://maven.scijava.org/content/groups/public")
}

dependencies {
    val scijavaParentPomVersion = project.properties["scijavaParentPOMVersion"]
    val ktVersion = project.properties["kotlinVersion"]
    implementation(platform("org.scijava:pom-scijava:$scijavaParentPomVersion"))

    // Graphics dependencies

    // Attention! Manual version increment necessary here!
    val scijavaCommonVersion = "2.98.0"
    annotationProcessor("org.scijava:scijava-common:$scijavaCommonVersion")
    kapt("org.scijava:scijava-common:$scijavaCommonVersion") {
        exclude("org.lwjgl")
    }

    val sceneryVersion = "0.11.2"
    api("graphics.scenery:scenery:$sceneryVersion") {
        version { strictly(sceneryVersion) }
        exclude("org.biojava.thirdparty", "forester")
        exclude("null", "unspecified")

        // from biojava artifacts; clashes with jakarta-activation-api
        exclude("javax.xml.bind", "jaxb-api")
        exclude("org.glassfish.jaxb", "jaxb-runtime")
        exclude("org.jogamp.jogl","jogl-all")
    }

    implementation("net.java.dev.jna:jna-platform:5.14.0")
    implementation("org.janelia.saalfeldlab:n5")
    implementation("org.janelia.saalfeldlab:n5-imglib2")
    implementation("org.apache.logging.log4j:log4j-api:2.23.1")
    implementation("org.apache.logging.log4j:log4j-1.2-api:2.20.0")

    implementation("com.formdev:flatlaf:3.4.1")

    // SciJava dependencies

    implementation("org.yaml:snakeyaml") {
        version { strictly("1.33") }
    }
    implementation("org.scijava:scijava-common")
    implementation("org.scijava:ui-behaviour")
    implementation("org.scijava:script-editor")
    implementation("org.scijava:scijava-ui-swing")
    implementation("org.scijava:scijava-ui-awt")
    implementation("org.scijava:scijava-search")
    implementation("org.scijava:scripting-jython")
//    implementation(migLayout.swing)

    // ImageJ dependencies

    implementation("net.imagej:imagej-common")
    api("net.imagej:imagej-mesh:0.8.1")
    implementation("net.imagej:imagej-mesh-io")
    implementation("net.imagej:imagej-ops")
//    implementation("net.imagej:imagej-launcher")
    implementation("net.imagej:imagej-ui-swing")
    implementation("io.scif:scifio")
    implementation("io.scif:scifio-bf-compat")

    // ImgLib2 dependencies
    implementation("net.imglib2:imglib2")
    implementation("net.imglib2:imglib2-roi")

    // XDG support
    implementation("dev.dirs:directories:26")

    // Math dependencies
//    implementation(commons.math3)
//    implementation(misc.joml)

    // Kotlin dependencies
    implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$ktVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$ktVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    // Test scope

//    testImplementation(misc.junit4)
//    implementation("net.imagej:ij")
    implementation("net.imglib2:imglib2-ij")

//    implementation(n5.core)
//    implementation(n5.hdf5)
//    implementation(n5.imglib2)
    implementation("org.janelia.saalfeldlab:n5")
    implementation("org.janelia.saalfeldlab:n5-hdf5")
    implementation("sc.fiji:spim_data")
    implementation("org.slf4j:slf4j-simple")

    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.slf4j:slf4j-simple")

    implementation("sc.fiji:bigdataviewer-core")
    implementation("sc.fiji:bigdataviewer-vistools")
    implementation("sc.fiji:bigvolumeviewer:0.3.3") {
        exclude("org.jogamp.jogl","jogl-all")
        exclude("org.jogamp.gluegen", "gluegen-rt")
    }

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

val isRelease: Boolean
    get() = System.getProperty("release") == "true"

tasks {
    withType<KotlinCompile>().all {
        val version = System.getProperty("java.version").substringBefore('.').toInt()
        val default = if (version == 1) "21" else "$version"
        kotlinOptions {
            jvmTarget = project.properties["jvmTarget"]?.toString() ?: default
            freeCompilerArgs += listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
        }
//        sourceCompatibility = project.properties["sourceCompatibility"]?.toString() ?: default
    }
    test {
        finalizedBy(jacocoTestReport) // report is always generated after tests run
    }
    jar {
        archiveVersion.set(rootProject.version.toString())

        manifest.attributes["Implementation-Build"] = run { // retrieve the git commit hash
            val gitFolder = "$projectDir/.git/"
            val digit = 7
            /*  '.git/HEAD' contains either
             *      in case of detached head: the currently checked out commit hash
             *      otherwise: a reference to a file containing the current commit hash     */
            val head = file(gitFolder + "HEAD").readText().split(":") // .git/HEAD
            val isCommit = head.size == 1 // e5a7c79edabbf7dd39888442df081b1c9d8e88fd
            // def isRef = head.length > 1     // ref: refs/heads/main
            when {
                isCommit -> head[0] // e5a7c79edabb
                else -> file(gitFolder + head[1].trim()) // .git/refs/heads/main
                    .readText()
            }.trim().take(digit)
        }
        manifest.attributes["Implementation-Version"] = project.version
    }

    withType<GenerateMavenPom>().configureEach {
        val scijavaParentPomVersion = project.properties["scijavaParentPOMVersion"]
        val matcher = Regex("""generatePomFileFor(\w+)Publication""").matchEntire(name)
        val publicationName = matcher?.let { it.groupValues[1] }

        pom.properties.empty()

        pom.withXml {
            // Add parent to the generated pom
            val parent = asNode().appendNode("parent")
            parent.appendNode("groupId", "org.scijava")
            parent.appendNode("artifactId", "pom-scijava")
            parent.appendNode("version", "$scijavaParentPomVersion")
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
            propertiesNode.appendNode("jvrpn.version", "1.2.0")

            // add correct lwjgl version
            propertiesNode.appendNode("lwjgl.version", "3.3.3")

            // add bigvolumeviewer version
            propertiesNode.appendNode("bigvolumeviewer.version", "0.3.3")

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
                                            "lwjgl-shaderc",
                                            "lwjgl-jawt",
                                            "log4j-1.2-api")

            val toSkip = listOf("pom-scijava")

            logger.quiet("Adding pom-scijava-managed dependencies for Maven publication:")

            configurations.implementation.get().allDependencies.forEach {
                val artifactId = it.name

                if (!toSkip.contains(artifactId)) {

                    val propertyName = "$artifactId.version"

                    // we only add our own property if the version is not null,
                    // as that indicates something managed by the parent POM, where
                    // we did not specify an explicit version.
                    if (versionedArtifacts.contains(artifactId) && it.version != null) {
                        // add "<artifactid.version>[version]</artifactid.version>" to pom
                        propertiesNode.appendNode(propertyName, it.version)
                    }

                    val dependencyNode = dependenciesNode.appendNode("dependency")
                    dependencyNode.appendNode("groupId", it.group)
                    dependencyNode.appendNode("artifactId", artifactId)
                    dependencyNode.appendNode("version", "\${$propertyName}")

                    // Custom per artifact tweaks
                    logger.quiet("* ${it.group}:$artifactId with version property \$$propertyName")
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
            val exampleName = className.substringAfterLast(".")
            val exampleType = className.substringBeforeLast(".").substringAfterLast(".")

            logger.info("Registering $exampleName of $exampleType from $className")
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

    dokkaHtml {
        enabled = isRelease
        dokkaSourceSets.configureEach {
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/scenerygraphics/sciview/tree/main/src/main/kotlin"))
                remoteLineSuffix.set("#L")
            }
        }
    }

    dokkaJavadoc {
        enabled = isRelease
    }

    if(project.properties["buildFatJAR"] == true) {
        apply(plugin = "com.github.johnrengelman.shadow")
        jar {
            isZip64 = true
        }
    }

    withType<GenerateModuleMetadata> {
        enabled = false
    }
}

jacoco {
    toolVersion = "0.8.11"
}

task("copyDependencies", Copy::class) {
    from(configurations.runtimeClasspath).into("$buildDir/dependencies")
}

java.withSourcesJar()

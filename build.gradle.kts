import org.gradle.kotlin.dsl.implementation
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sciJava.*
import sciview.implementation
import java.net.URL

plugins {
    val ktVersion = "1.4.20"
    java
    kotlin("jvm") version ktVersion
    kotlin("kapt") version ktVersion
    sciview.publish
    sciview.sign
    id("com.github.elect86.sciJava") version "0.0.4"
    id("org.jetbrains.dokka") version ktVersion
    jacoco
    id("sciJava.platform") version "30.0.0+14"
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://maven.scijava.org/content/groups/public")
    maven("https://raw.githubusercontent.com/kotlin-graphics/mary/master")
    maven("https://jitpack.io")
}

// here we set some versions
//"ui-behaviour"("2.0.3")
//"imagej-mesh"("0.8.1")
//"bigdataviewer-vistools"("1.0.0-beta-21")
//"bigvolumeviewer"("0.1.8") // added from Gradle conversion

dependencies {

    //    implementation(platform("sciJava:platform:30.0.0+6"))

    // Graphics dependencies

    val sciJavaCommon = "org.scijava:scijava-common:${versions["scijava-common"]}"
    annotationProcessor(sciJavaCommon)
    kapt(sciJavaCommon)

    //    api("graphics.scenery:scenery:861b4bc")
    api("com.github.scenerygraphics:scenery:937ba10")

    implementation(misc.cleargl)
    implementation(misc.coreMem)
    implementation(jogamp.jogl, native = joglNative)

    implementation("com.formdev:flatlaf:0.38")

    // SciJava dependencies

    implementation(sciJava.common) // { version { strictly("2.83.3") } } CHECKME
    implementation(sciJava.uiBehaviour)
    implementation(sciJava.scriptEditor)
    implementation(sciJava.uiSwing)
    implementation(sciJava.uiAwt)
    implementation(sciJava.search)
    implementation(sciJava.scriptingJython)
    implementation(migLayout.swing)

    // ImageJ dependencies

    implementation(imagej.core)
    //    sciJava("net.imagej") {
    //        exclude("org.scijava", "scripting-renjin")
    //        exclude("org.scijava", "scripting-jruby")
    //    }
    implementation(imagej.common)
    implementation(imagej.mesh) { version { strictly("0.8.1") } } // FIXME
    implementation(imagej.meshIo)
    implementation(imagej.ops)
    implementation(imagej.launcher)
    implementation(imagej.uiSwing)
    implementation(imagej.legacy)
    implementation(scifio.core)
    implementation(scifio.bfCompat)

    // ImgLib2 dependencies
    implementation(imgLib2.core)
    implementation(imgLib2.roi)

    // Math dependencies
    implementation(commons.math3)
    implementation(misc.joml)

    // Kotlin dependencies

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")

    // Optional dependencies - for sc.iview.Main only! -->
    //		<dependency>
    //			<groupId>org.scijava</groupId>
    //			<artifactId>scijava-plugins-commands</artifactId>
    //			<scope>runtime</scope>
    //			<optional>true</optional>
    //		</dependency>
    //		<dependency>
    //			<groupId>net.imagej</groupId>
    //			<artifactId>imagej-plugins-commands</artifactId>
    //			<scope>runtime</scope>
    //			<optional>true</optional>
    //		</dependency>
    //		<dependency>
    //			<groupId>ch.qos.logback</groupId>
    //			<artifactId>logback-classic</artifactId>
    //		</dependency>

    // Test scope

    testImplementation(misc.junit4)
    implementation(imagej.ij)
    implementation(imgLib2.ij)

    implementation(n5.core)
    implementation(n5.hdf5)
    implementation(n5.imglib2)
    implementation(bigDataViewer.spimData)

    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(kotlin("test-junit"))

    implementation(bigDataViewer.core)
    implementation(bigDataViewer.visTools)
    implementation("com.github.skalarproduktraum:jogl-minimal:1c86442")

    // this apparently is still necessary
    implementation(platform("org.lwjgl:lwjgl-bom:3.2.3"))
    val os = getCurrentOperatingSystem()
    val lwjglNatives = "natives-" + when {
        os.isWindows -> "windows"
        os.isLinux -> "linux"
        os.isMacOsX -> "macos"
        else -> error("invalid")
    }
    listOf("", "-glfw", "-jemalloc", "-vulkan", "-opengl", "-openvr", "-xxhash", "-remotery").forEach {
        implementation("org.lwjgl:lwjgl$it")
        if (it != "-vulkan")
            runtimeOnly("org.lwjgl", "lwjgl$it", classifier = lwjglNatives)
    }

    sciJava("graphics.scenery:spirvcrossj:0.7.0-1.1.106.0")
    runtimeOnly("graphics.scenery", "spirvcrossj", "0.7.0-1.1.106.0", classifier = lwjglNatives)

    sciJava("net.java.jinput:jinput:2.0.9", native = "natives-all")
}

kapt {
    useBuildCache = false // safe
    arguments {
        arg("-Werror")
        arg("-Xopt-in", "kotlin.RequiresOptIn")
    }
}

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
    // https://docs.gradle.org/current/userguide/java_testing.html#test_filtering
    test {
        finalizedBy(jacocoTestReport) // report is always generated after tests run
    }
    jar {
        archiveVersion.set(rootProject.version.toString())
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
            xml.isEnabled = true
            html.apply {
                isEnabled = false
                //                destination = file("$buildDir/jacocoHtml")
            }
        }
        dependsOn(test) // tests are required to run before generating the report
    }
    register("runApp", JavaExec::class.java) {
        classpath = sourceSets.main.get().runtimeClasspath

        main = "sc.iview.commands.demo.basic.MeshDemo"

        // arguments to pass to the application
        //    args 'appArg1'
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


artifacts {
    archives(dokkaJavadocJar)
    archives(dokkaHtmlJar)
}

java.withSourcesJar()

import org.gradle.kotlin.dsl.implementation
import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import sciJava.*
import java.net.URL

plugins {
    val ktVersion = "1.4.20"
    java
    kotlin("jvm") version ktVersion
    sciview.publish
    sciview.sign
    id("com.github.elect86.sciJava") version "0.0.4"
    id("org.jetbrains.dokka") version ktVersion
    jacoco
    idea
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
    maven("https://maven.scijava.org/content/groups/public")
}

val sceneryVersion = "886a7492"
"scijava-common"("2.84.0")
"ui-behaviour"("2.0.3")
"imagej-mesh"("0.8.1")
"bigdataviewer-vistools"("1.0.0-beta-21")
"bigvolumeviewer"("0.1.8") // added from Gradle conversion

"kotlin"("1.4.20")
"kotlinx-coroutines-core"("1.3.9")

dependencies {

    // Graphics dependencies

    api("com.github.scenerygraphics:scenery:$sceneryVersion") {
        for (mod in listOf("glfw", "jemalloc", "vulkan", "opengl", "openvr", "xxhash", "remotery"))
            exclude("org.lwjgl", "lwjgl-$mod")
    }

    sciJava("net.clearvolume:cleargl")
    sciJava("net.clearcontrol:coremem")
    sciJava("org.jogamp.jogl:jogl-all", native = joglNative)

    implementation("com.formdev:flatlaf:0.38")

    // SciJava dependencies

    sciJava("org.scijava"["scijava-common", "ui-behaviour", "script-editor", "scijava-ui-swing",
            "scijava-ui-awt", "scijava-search", "scripting-jython"])
    sciJava("com.miglayout:miglayout-swing")

    // ImageJ dependencies

    sciJava("net.imagej") {
        exclude("org.scijava", "scripting-renjin")
        exclude("org.scijava", "scripting-jruby")
    }

    sciJava("net.imagej:imagej-"["common", "mesh", "mesh-io", "ops", "launcher", "ui-swing", "legacy"])
    sciJava("io.scif:scifio"["", "-bf-compat"])

    // ImgLib2 dependencies

    sciJava("net.imglib2:imglib2"["-roi", ""])

    // Math dependencies

    sciJava("org.apache.commons:commons-math3")
    sciJava("org.joml")

    // Kotlin dependencies

    sciJava("org.jetbrains.kotlinx:kotlinx-coroutines-core")

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

    testSciJava("junit")
    sciJava("net.imagej:ij", test = false)
    sciJava("net.imglib2:imglib2-ij", test = false)


    sciJava("org.janelia.saalfeldlab:n5"["", "-hdf5", "-imglib2"])
    sciJava("sc.fiji:spim_data")

    implementation(kotlin("stdlib-jdk8"))
    sciJava("org.jetbrains.kotlin:kotlin-test-junit:\$kotlin")

    sciJava("sc.fiji:bigdataviewer-core")
    sciJava("sc.fiji:bigdataviewer-vistools")
    sciJava("sc.fiji:bigvolumeviewer")

    implementation(platform("org.lwjgl:lwjgl-bom:3.2.3"))
    val os = getCurrentOperatingSystem()
    val lwjglNatives = "natives-" + when {
        os.isWindows -> "windows"
        os.isLinux -> "linux"
        os.isMacOsX -> "macos"
        else -> error("invalid")
    }
    implementation("org.lwjgl:lwjgl")
    runtimeOnly("org.lwjgl", "lwjgl", classifier = lwjglNatives)
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
}

//val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
//    dependsOn(tasks.dokkaJavadoc)
//    from(tasks.dokkaJavadoc.get().outputDirectory.get())
//    archiveClassifier.set("javadoc")
//}
//
//val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
//    dependsOn(tasks.dokkaHtml)
//    from(tasks.dokkaHtml.get().outputDirectory.get())
//    archiveClassifier.set("html-doc")
//}

val sourceJar = task("sourceJar", Jar::class) {
    dependsOn(tasks.classes)
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

artifacts {
    //    archives(dokkaJavadocJar)
    //    archives(dokkaHtmlJar)
    archives(sourceJar)
}

java {
//    withJavadocJar()
    withSourcesJar()
}

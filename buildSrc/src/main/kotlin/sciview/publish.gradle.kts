package sciview

import java.net.URI

// configuration of the Maven artifacts
plugins {
    `maven-publish`
    `java-library`
    id("org.jetbrains.dokka")
}

val sciviewUrl = "https://github.com/scenerygraphics/sciview"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "sc.iview"
            artifactId = rootProject.name
            val customVersion = project.properties["customVersion"]
            val v = if(customVersion != null) {
                if(customVersion == "git") {
                    val gitCommand = Runtime.getRuntime().exec("git rev-parse --verify --short HEAD")
                    val result = gitCommand.waitFor()

                    if(result == 0) {
                        gitCommand.inputStream.bufferedReader().use { it.readText() }.trim().substring(0, 7)
                    } else {
                        logger.error("Could not execute git to get commit hash (exit code $result). Is git installed?")
                        logger.error("Will fall back to default project version.")
                        rootProject.version
                    }
                } else {
                    customVersion
                }
            } else {
                rootProject.version
            }

            version = v.toString()

            logger.quiet("Creating Maven publication $groupId:$artifactId:$version ...")

            from(components["java"])

            val dokkaJavadocJar by tasks.register<Jar>("dokkaJavadocJar") {
                dependsOn(tasks.dokkaJavadoc)
                from(tasks.dokkaJavadoc.flatMap { it.outputDirectory })
                archiveClassifier.set("javadoc")
            }

            val dokkaHtmlJar by tasks.register<Jar>("dokkaHtmlJar") {
                dependsOn(tasks.dokkaHtml)
                from(tasks.dokkaHtml.flatMap { it.outputDirectory })
                archiveClassifier.set("html-doc")
            }


            artifact(dokkaJavadocJar)
            artifact(dokkaHtmlJar)

            // TODO, resolved dependencies versions? https://docs.gradle.org/current/userguide/publishing_maven.html#publishing_maven:resolved_dependencies

            pom {
                name.set(rootProject.name)
                description.set(rootProject.description)
                url.set(sciviewUrl)
                properties.set(mapOf("inceptionYear" to "2016"))
                organization {
                    name.set(rootProject.name)
                    url.set("http://scenery.graphics")
                }
                licenses {
                    license {
                        name.set("Simplified BSD License")
                        //                        url.set("https://www.gnu.org/licenses/lgpl.html") TODO?
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("kephale")
                        name.set("Kyle Harrington")
                        url.set("https://kyleharrington.com")
                        roles.addAll("founder", "lead", "developer", "debugger", "reviewer", "support", "maintainer")
                    }
                    developer {
                        id.set("skalarproduktraum")
                        name.set("Ulrik Günther")
                        url.set("https://imagej.net/User:Skalarproduktraum")
                        roles.addAll("founder", "lead", "developer", "debugger", "reviewer", "support", "maintainer")
                    }
                }
                contributors {
                    contributor {
                        name.set("Robert Haase")
                        url.set("https://imagej.net/User:Haesleinhuepf")
                        properties.set(mapOf("id" to "haesleinhuepf"))
                    }
                    contributor {
                        name.set("Curtis Rueden")
                        url.set("https://imagej.net/User:Rueden")
                        properties.set(mapOf("id" to "ctrueden"))
                    }
                    contributor {
                        name.set("Aryaman Gupta")
                        properties.set(mapOf("id" to "aryaman-gupta"))
                    }
                }
                mailingLists {
                    mailingList {
                        name.set("Image.sc Forum")
                        archive.set("https://forum.image.sc/tags/sciview")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/scenerygraphics/sciview")
                    developerConnection.set("scm:git:git@github.com:scenerygraphics/sciview")
                    tag.set("sciview-0.2.0-beta-9") // TODO differs from version
                    url.set(sciviewUrl)
                }
                issueManagement {
                    system.set("GitHub Issues")
                    url.set("https://github.com/scenerygraphics/sciview/issues")
                }
                ciManagement {
                    system.set("GitHub Actions")
                    url.set("https://github.com/scenerygraphics/sciview/actions")
                }
                distributionManagement {
                    // https://stackoverflow.com/a/21760035/1047713
                    //                    <snapshotRepository>
                    //                        <id>ossrh</id>
                    //                        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
                    //                    </snapshotRepository>
                    //                    <repository>
                    //                        <id>ossrh</id>
                    //                        <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
                    //                    </repository>
                }
                //                artifact("${rootProject.name}-${rootProject.version}-sources.jar")
                //                artifact("${rootProject.name}-${rootProject.version}-javadoc.jar")
            }
        }
    }

    repositories {
        maven {
            name = "scijava"
            credentials(PasswordCredentials::class)

            val releaseRepo = "https://maven.scijava.org/content/repositories/releases/"
            val snapshotRepo = "https://maven.scijava.org/content/repositories/snapshots/"

            val snapshot = rootProject.version.toString().endsWith("SNAPSHOT")
            url = URI(if (snapshot) snapshotRepo else releaseRepo)
        }
    }
}

// TODO?

//<properties>
//<main-class>sc.iview.Main</main-class>
//<package-name>sciview</package-name>
//
//<license.licenseName>bsd_2</license.licenseName>
//<license.copyrightOwners>sciview developers.</license.copyrightOwners>

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
//
//val sourceJar = task("sourceJar", Jar::class) {
//    dependsOn(tasks.classes)
//    archiveClassifier.set("sources")
//    from(sourceSets.main.get().allSource)
//}
//
//artifacts {
//    archives(dokkaJavadocJar)
//    archives(dokkaHtmlJar)
//    archives(sourceJar)
//}

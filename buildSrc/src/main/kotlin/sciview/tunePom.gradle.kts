package sciview

import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

tasks {

    withType<GenerateMavenPom>().configureEach {
        val matcher = Regex("""generatePomFileFor(\w+)Publication""").matchEntire(name)
        //        val publicationName = matcher?.let { it.groupValues[1] }

        pom.properties.empty()

        pom.withXml {
            val node = asNode()
            // Add parent to the generated pom
            node.appendNode("parent")
                .appendNode("groupId", "org.scijava")
                .appendNode("artifactId", "pom-scijava")
                .appendNode("version", "30.0.0")
                .appendNode("relativePath")

            // Update the dependencies and properties
            val dependenciesNode = node.appendNode("dependencies")
            val propertiesNode = node.appendNode("properties")
            propertiesNode.appendNode("inceptionYear", 2016)

            listOf("graphics.scenery:scenery",
                   "com.formdev:flatlaf",
                   "org.jetbrains.kotlin:kotlin-stdlib-common",
                   "org.jetbrains.kotlin:kotlin-stdlib",
                   "org.jetbrains.kotlinx:kotlinx-coroutines-core",
                   "net.imagej:imagej-mesh").forEach { ga ->

                val (group, artifactId) = ga.split(':')
                val version = configurations.named<Configuration>("implementation").get().allDependencies
                    .first { it.group == group && it.name == name }.version!!
                val propertyName = "$artifactId.version"
                propertiesNode.appendNode(propertyName, version)

                val dependencyNode = dependenciesNode.appendNode("dependency")
                    .appendNode("groupId", group)
                    .appendNode("artifactId", artifactId)
                    .appendNode("version", "\${$propertyName}")

                // Custom per artifact tweaks
                //                println(artifactId)
                if (Regex("-bom").find(artifactId) != null)
                    dependencyNode.appendNode("type", "pom")
                // from https://github.com/scenerygraphics/sciview/pull/399#issuecomment-904732945
                if (artifactId == "formats-gpl")
                    dependencyNode.appendNode("exclusions")
                        .appendNode("exclusion")
                        .appendNode("groupId", "com.fasterxml.jackson.core")
                        .appendNode("artifactId", "jackson-core")
                        .appendNode("exclusion")
                        .appendNode("groupId", "com.fasterxml.jackson.core")
                        .appendNode("artifactId", "jackson-annotations")
                //dependencyNode.appendNode("scope", it.scope)
            }

            fun String.delete() = Regex("<$this>").find(asString())?.range?.start?.let { start ->
                Regex("</$this>").find(asString())?.range?.last?.let {
                    asString().replace(start, it + 1, "")
                }
            }

            "dependencyManagement".delete()
            "dependencies>".delete()
        }
    }
}
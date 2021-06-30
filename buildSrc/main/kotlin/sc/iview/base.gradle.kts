package sc.iview

plugins {
    jacoco
}

tasks {

    sourceSets.test.get().allSource.files
        .map { it.path.substringAfter("kotlin${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".kt") }
        .forEach { className ->
            val exampleName = className.substringAfterLast(".")
            val exampleType = className.substringBeforeLast(".").substringAfterLast(".")

            register<JavaExec>(name = className.substringAfterLast(".")) {
                classpath = sourceSets.test.get().runtimeClasspath
                main = className
                group = "examples.$exampleType"

                val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("scenery.") }

                val additionalArgs = System.getenv("SCENERY_JVM_ARGS")
                allJvmArgs = if (additionalArgs != null) {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") } + additionalArgs
                } else {
                    allJvmArgs + props.flatMap { (k, v) -> listOf("-D$k=$v") }
                }
            }
        }

    register<JavaExec>("run") {
        classpath = sourceSets.test.get().runtimeClasspath
        if (project.hasProperty("target")) {
            project.property("target")?.let { target ->
                val file = sourceSets.test.get().allSource.files.first { "class $target" in it.readText() }
                main = file.path.substringAfter("kotlin${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".kt")
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

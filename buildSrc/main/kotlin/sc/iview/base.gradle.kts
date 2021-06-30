package sc.iview

plugins {
    jacoco
}

tasks {

    register<JavaExec>("run") {
        classpath = sourceSets.test.get().runtimeClasspath
        if (project.hasProperty("target")) {
            project.property("target")?.let { target ->
                val file = sourceSets.test.get().allSource.files.first { "class $target" in it.readText() }
                main = file.path.substringAfter("kotlin${File.separatorChar}").replace(File.separatorChar, '.').substringBefore(".kt")
                val props = System.getProperties().filter { (k, _) -> k.toString().startsWith("sc.iview.") }

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

val TaskContainer.jacocoTestReport: TaskProvider<JacocoReport>
    get() = named<JacocoReport>("jacocoTestReport")

val TaskContainer.test: TaskProvider<Test>
    get() = named<Test>("test")

val Project.sourceSets: SourceSetContainer
    get() = (this as ExtensionAware).extensions.getByName("sourceSets") as SourceSetContainer

val SourceSetContainer.test: NamedDomainObjectProvider<SourceSet>
    get() = named<SourceSet>("test")


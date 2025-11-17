package sciview

import de.undercouch.gradle.tasks.download.Download
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

plugins {
    id("de.undercouch.download")
}

// Discern the path to the Fiji folder we are working with.
val fijiDir: File = properties["fiji.dir"]?.toString()?.let(::File)
    ?: layout.buildDirectory.dir("fiji/Fiji").get().asFile

// For really shaky connections, we retry a few times by default.
val maxRetries = 16

// Properties for the update site. Can be set in the local gradle.properties,
// in the one in ~/.gradle, or on the command line via -P
val updateSite = project.properties["fijiUpdateSite"] as String
val updateSiteURL = project.properties["fijiUpdateSiteURL"] as String
val user = project.properties["fijiUpdateSiteUsername"]
val pass = project.properties["fijiUpdateSitePassword"]

val fijiTestClass = project.properties["fijiTestClass"] as String
val fijiTestClassExpectedOutput = project.properties["fijiTestClassExpectedOutput"] as String

val fijiDownloadURL = "https://downloads.imagej.net/fiji/latest/fiji-latest-portable-nojava.zip"
val fijiZipFile = layout.buildDirectory.file("fiji-latest-portable-nojava.zip")
val fijiChecksumDisable = (project.properties["fijiChecksumDisable"] as? String)?.toBoolean() ?: false

tasks {
    val fijiDownload by tasks.creating(Download::class) {
        src(fijiDownloadURL)
        dest(fijiZipFile)
        retries(maxRetries)
        overwrite(false)
        useETag(true)

        finalizedBy("fijiChecksum")
    }

    register("fijiChecksum") {
        onlyIf { !fijiChecksumDisable }
        notCompatibleWithConfigurationCache("Uses project references in doLast block")
        doLast { checksum() }
    }

    register<Copy>("fijiUnpack") {
        group = "Fiji"
        description = "Unpacks the fiji zip archive into $fijiDir, downloading it as needed."
        notCompatibleWithConfigurationCache("Uses project references in doFirst block")
        onlyIf {
            fijiDir.listFiles()?.isEmpty() == true || !fijiDir.exists()
        }
        doFirst {
            logger.lifecycle("Unpacking Fiji into $fijiDir")
            fijiDir.mkdirs()
        }
        dependsOn(fijiDownload)
        from(zipTree(fijiDownload.dest))
        into(fijiDir.parentFile)
    }

    register("fijiUpdate") {
        group = "Fiji"
        description = "Updates the Fiji installation at $fijiDir."
        dependsOn("fijiUnpack")
        notCompatibleWithConfigurationCache("Uses project references in doLast block")
        doLast { update() }
    }

    register("fijiPopulate") {
        group = "Fiji"
        description = "Installs $updateSite into the Fiji installation at $fijiDir."
        dependsOn("jar", "fijiUnpack")
        mustRunAfter("jar")
        notCompatibleWithConfigurationCache("Uses project references in doLast block")
        doLast { populate() }
    }

    register("fijiUpload") {
        group = "Fiji"
        description = "Uploads $updateSite + dependencies from $fijiDir to the $updateSite update site."
        dependsOn("fijiPopulate")
        notCompatibleWithConfigurationCache("Uses project references in doLast block")
        doLast { upload() }
    }

    register("fijiClean") {
        group = "Fiji"
        description = "Deletes the Fiji installation at $fijiDir."
        doLast {
            logger.lifecycle("Removing Fiji directory $fijiDir")
            fijiDir.deleteRecursively()
        }
    }

    register("fijiDistClean") {
        group = "Fiji"
        description = "Deletes the Fiji installation at $fijiDir, as well as the downloaded fiji zip archive."
        doLast {
            logger.lifecycle("Removing Fiji directory")
            fijiDir.deleteRecursively()
            logger.lifecycle("Removing fiji-latest-portable-nojava.zip")
            fijiDir.parentFile.resolve("fiji-latest-portable-nojava.zip").delete()
        }
    }

}

private fun checksum() {
    val checksumFile = fijiZipFile.get().asFile.resolveSibling("fiji-latest-portable-nojava.zip.sha256")

    logger.lifecycle("Checking SHA256 checksum of fiji-latest-portable-nojava.zip")
    download.run {
        src("$fijiDownloadURL.sha256")
        dest(checksumFile)
        overwrite(true)
        retries(maxRetries)
        useETag(true)
    }

    verifyChecksum.run {
        src(fijiZipFile)
        algorithm("SHA256")
        checksum(checksumFile.readText().trim())
    }
}

private fun update() {
    validateFijiDir()

    val skip = project.properties["fijiUpdateSkip"]?.toString()?.toBoolean() ?: false
    if (skip) {
        logger.lifecycle("Skipping Fiji update due to fijiUpdateSkip=true")
        return
    }

    try {
        runUpdater("add-update-site", updateSite, updateSiteURL)
        runUpdater("update")
    }
    catch (_: Exception) {
        error("Failed to update Fiji")
    }
}

private fun populate() {
    validateFijiDir()
    logger.lifecycle("Populating $fijiDir...")

    // Parse relevant update site databases. This information is useful
    // for deciding which JAR files to copy, and which ones to leave alone.
    val info = Info(
        db("Fiji-Latest", "https://sites.imagej.net/Fiji"),
        db(updateSite, updateSiteURL),
        mutableMapOf()
    )

    // Gather details of existing JAR files in the Fiji installation.
    val jarsDir = fijiDir.resolve("jars")
    project.fileTree(jarsDir).files.forEach {
        if (it.extension == "jar") {
            val relPath = it.path.substring(fijiDir.path.length)
            info.localFiles.getOrPut(relPath.davce.acKey) { mutableListOf() } += it
        }
    }

    logger.info("--> Copying files into Fiji installation")

    // Copy main artifact.
    val mainJar = project.tasks.getByName("jar").outputs.files.singleFile
    copy {
        from(mainJar)
        into(jarsDir)
        eachFile { prepareToCopyFile(file, jarsDir, "jars", info) ?: exclude() }
    }

    // Copy platform-independent JAR files.
    copy {
        from(configurations.named("runtimeClasspath"))
        into(jarsDir)
        eachFile { prepareToCopyFile(file, jarsDir, "jars", info) ?: exclude() }
    }

    // Copy platform-specific JAR files.
    for (platform in listOf("linux-arm64", "linux64", "macos-arm64", "macos64", "win-arm64", "win64")) {
        val platformDir = jarsDir.resolve(platform)
        copy {
            from(configurations.named("runtimeClasspath"))
            into(platformDir)
            eachFile { prepareToCopyFile(file, platformDir, "jars/$platform", info) ?: exclude() }
        }
    }

    // HACK: Fix the naming of Fiji's renamed artifacts.
    // This map is the inverse of the one called "renamedArtifacts" elsewhere.
    val artifactsToFix = mapOf(
        "jocl" to "org.jocl.jocl"
    )
    for (file in project.fileTree(jarsDir).files) {
        if (file.extension != "jar") continue
        val relPath = file.path.substring(fijiDir.path.length)
        val davce = relPath.davce
        if (davce.classifier.isNotEmpty()) continue // DOUBLE HACK OMG
        val badArtifactId = davce.artifactId
        val goodArtifactId = artifactsToFix[badArtifactId]
        if (goodArtifactId != null) {
            val goodFile = File(file.path.replaceFirst(badArtifactId, goodArtifactId))
            logger.info("Renaming: $file -> $goodFile")
            file.renameTo(goodFile)
        }
    }

    // Now that we populated Fiji, let's verify that it works.
    // We launch the class given in fijiTestClass to check whether the update site was correctly installed.
    // Finally, the strings given in fijiTestClassExpectedOutput are tested against this command's output.
    logger.info("--> Testing installation")
    val stdout = runFiji("--run", fijiTestClass)
    logger.info(stdout)
    val gitHash: String = project.properties["gitHash"] as? String ?: "git rev-parse HEAD".runCommand(File("."))?.trim()?.substring(0, 7) ?: ""
    val expectedContents = fijiTestClassExpectedOutput
        .replace("[GIT_HASH]", gitHash)
        .replace("[VERSION_NUMBER]", project.version.toString())
        .split("|")

    expectedContents.forEach {
        logger.lifecycle("Output is expected to contain $it (${stdout.contains(it)})")
    }

    val success = expectedContents.map { stdout.contains(it) }.all { it == true }
    if (!success) {
        throw GradleException("Testing $updateSite command $fijiTestClass failed.")
    }
    logger.lifecycle("Testing $updateSite $fijiTestClass command successful.")
}

private fun upload() {
    if (user == null || pass == null) {
        error("""
            No credentials available to upload to the update site.
            Please configure the fijiUpdateSiteUsername and fijiUpdateSitePassword
            properties in your ~/.gradle/gradle.properties file.
        """.trimIndent())
    }
    val dryRun = project.properties["fijiUploadDryRun"]?.toString()?.toBoolean() ?: false

    logger.lifecycle("Uploading to Fiji update site $updateSite at $updateSiteURL ${if(dryRun) { " (dry run)" } else { "" }}")
    runUpdater("edit-update-site", updateSite, updateSiteURL, "webdav:$user:$pass", ".")

    if(dryRun) {
        runUpdater("upload-complete-site", "--force", "--force-shadow", "--simulate", updateSite)
    } else {
        runUpdater("upload-complete-site", "--force", "--force-shadow", updateSite)
    }
}

private fun validateFijiDir() {
    if (!fijiDir.isDirectory) {
        error("No such directory: ${fijiDir.absolutePath}")
    }
}

/** Runs Fiji with the given arguments. */
private fun runFiji(vararg args: String): String {
    return runLauncher(emptyList(), emptyList(), "net.imagej.Main", args.toList())
}

/** Runs the ImageJ Updater with the given arguments. */
private fun runUpdater(vararg args: String): String {
    val jvmArgs = listOf("-Dpatch.ij1=false")
    val launcherArgs = listOf("-classpath", ".")
    return runLauncher(jvmArgs, launcherArgs, "net.imagej.updater.CommandLine", args.toList())
}

private fun String.runCommand(workingDir: File): String? {
    try {
        val parts = this.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}

/**
 * Runs a Java program via the ImageJ Launcher's ClassLauncher, with the given arguments.
 *
 * Avoids using the ImageJ Launcher native binary, because it does not support Java 9+ well.
 */
private fun runLauncher(jvmArgs: List<String>, launcherArgs: List<String>, mainClass: String, mainArgs: List<String>): String {
    // Find the imagej-launcher-x.y.z.jar, needed on the classpath.
    val launcherJar = fijiDir.resolve("jars").listFiles()
        ?.firstOrNull { f -> f.name.startsWith("imagej-launcher-") }
        ?: error("Where is your $fijiDir/jars/imagej-launcher.jar?!")

    val jvmArgsForJava = listOf(
        "-Dpython.cachedir.skip=true",
        "-Dplugins.dir=$fijiDir",
        //"-Xmx...m",
        "-Djava.awt.headless=true",
        "-Dapple.awt.UIElement=true",
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "-Djava.class.path=$launcherJar",
        "-Dimagej.dir=$fijiDir",
        "-Dij.dir=$fijiDir",
        "-Dfiji.dir=$fijiDir",
        //"-Dfiji.defaultLibPath=lib/amd64/server/libjvm.so",
        //"-Dfiji.executable=./ImageJ-linux64",
        //"-Dij.executable=./ImageJ-linux64",
        //"-Djava.library.path=$fijiDir/lib/$platform:$fijiDir/mm/$platform",
    ) + jvmArgs

    val mainClassForJava = "net.imagej.launcher.ClassLauncher"

    // Yes: the original main class is passed to the ClassLauncher as a main argument.
    // It's confusing. But thanks to the magic of ClassLoaders, we can and we will.
    val mainArgsForJava = launcherArgs + listOf(
        "-ijjarpath", "jars",
        "-ijjarpath", "plugins",
    ) + mainClass + mainArgs

    return runJava(jvmArgsForJava, mainClassForJava, mainArgsForJava)
}

private fun runJava(jvmArgs: List<String>, mainClass: String, mainArgs: List<String>): String {
    // Find the Java binary. We use the same Java installation as the Gradle build.
    val javaHome = File(System.getProperty("java.home"))
    val binJava = listOf("java", "java.exe")
        .map { javaHome.resolve("bin").resolve(it) }
        .firstOrNull { it.exists() }
        ?: "No Java executable found! O_O"

    val stdoutStream = ByteArrayOutputStream()
    try {
        exec {
            workingDir = fijiDir
            commandLine = listOf(binJava.toString()) + jvmArgs + mainClass + mainArgs
            standardOutput = stdoutStream
        }
    }
    catch (exc: Exception) {
        logger.error(exc.toString())
    }
    return stdoutStream.toString().trim()
}

val String.dirname: String
    get() {
        val slash = lastIndexOf("/")
        return if (slash < 0) "" else substring(0, slash)
    }

val String.extension: String
    get() {
        val dot = lastIndexOf(".")
        return if (dot < 0) "" else substring(dot)
    }

/** Fixes backwards Windows path separators to use forward slashes instead. */
val String.normalized: String
    get() {
        return replace("\\", "/")
    }

/**
 * Parses a file path into five parts: containing directory, artifactId
 * prefix, version string, classifier (if any), and file extension.
 */
val String.davce: DAVCE
    get() {
        val e = extension
        val noExt = substring(0, length - e.length)
        val d = noExt.normalized.dirname
        val noDir = if (d.isEmpty()) noExt else noExt.substring(d.length + 1)
        val cMatch = Regex(".*?-((native|windows|mac|linux).*)").matchEntire(noDir)
        val c = cMatch?.groups?.get(1)?.value ?: ""
        val noClass = if (c.isEmpty()) noDir else noDir.substring(0, noDir.length - c.length - 1)
        val vMatch = Regex(".*?-([a-f0-9]{6}[a-f0-9]*|[0-9].*)").matchEntire(noClass)
        val v = vMatch?.groups?.get(1)?.value ?: ""
        val a = if (v.isEmpty()) noClass else noClass.substring(0, noClass.length - v.length - 1)

        // Do a sanity check.
        var expected = if (d.isEmpty()) a else "$d/$a"
        if (v.isNotEmpty()) expected += "-$v"
        if (c.isNotEmpty()) expected += "-$c"
        expected += e
        if (expected != normalized) error("Failed to parse file path '$this' correctly (d=$d a=$a v=$v c=$c e=$e expected=$expected)")

        return DAVCE(d, a, v, c, e)
    }

infix fun String.compareVersions(version: String): Int {
    val tokens1 = split(".")
    val tokens2 = version.split(".")
    for ((a, b) in tokens1.zip(tokens2)) {
        try {
            // Try to compare integers.
            val aa = a.toLong()
            val bb = b.toLong()
            if (aa < bb) return -1
            if (aa > bb) return 1
        }
        catch (e: NumberFormatException) {
            // Fall back to comparing strings.
            if (a < b) return -1
            if (a > b) return 1
        }
    }
    return tokens1.size.compareTo(tokens2.size)
}

val Element.filename: String
    get() {
        return getAttribute("filename") ?: error("Plugin entry has no filename attribute")
    }

val Element.isObsolete: Boolean
    get() {
        for (j in 0 until childNodes.length) {
            val child = childNodes.item(j)
            if (child.nodeName == "version") return false
        }
        return true
    }

fun warn(message: String) { logger.warn("[WARNING] $message") }

fun db(siteName: String, siteURL: String): Map<AC, Element> {
    val dbCacheDir = layout.buildDirectory.dir("tmp").get().asFile
    Files.createDirectories(dbCacheDir.toPath())
    val dbXml = dbCacheDir.resolve("$siteName.xml.gz")
    val url = "$siteURL/db.xml.gz"
    if (!dbXml.exists()) {
        download.run {
            src(url)
            dest(dbXml)
            overwrite(true)
            retries(maxRetries)
        }
    }
    if (!dbXml.exists()) {
        error("Download failed: $url")
    }

    logger.info("--> Parsing ${dbXml.name}")
    val db = parseGzippedXml(dbXml.readBytes())
    val xpath = XPathFactory.newInstance().newXPath()
    val plugins = xpath.evaluate("//pluginRecords/plugin", db, XPathConstants.NODESET) as NodeList
    val pluginMap = mutableMapOf<AC, Element>()
    for (i in 0 until plugins.length) {
        val plugin = plugins.item(i) as Element
        if (plugin.isObsolete) continue
        if (!plugin.filename.endsWith(".jar")) continue
        val ac = plugin.filename.davce.acKey

        val clashingPlugin = pluginMap[ac]
        if (clashingPlugin != null) {
            error("Clashing plugin entries: ${clashingPlugin.filename} vs. ${plugin.filename} " +
                "(${clashingPlugin.filename.davce} vs. ${plugin.filename.davce})")
        }
        pluginMap[ac] = plugin
    }
    return pluginMap
}

fun parseGzippedXml(gzippedXml: ByteArray): Document {
    GZIPInputStream(ByteArrayInputStream(gzippedXml)).use { gzis ->
        InputStreamReader(gzis, StandardCharsets.UTF_8).use { reader ->
            val documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            return documentBuilder.parse(InputSource(reader))
        }
    }
}

data class AC(val artifactId: String, val classifier: String) {
    val pathGuess: String get() {
        val isWin = classifier.contains("win")
        val isMac = classifier.contains("mac")
        val isLinux = classifier.contains("linux")
        val isArm64 = classifier.contains("arm64") || classifier.contains("aarch64")
        val subdir = when {
            isLinux && isArm64 -> "linux-arm64"
            isLinux -> "linux64"
            isMac && isArm64 -> "macos-arm64"
            isMac -> "macos64"
            isWin && isArm64 -> "win-arm64"
            isWin -> "win64"
            else -> return "jars"
        }
        return "jars/$subdir"
    }
}
data class DAVCE(
    val dirname: String,
    val artifactId: String,
    val version: String,
    val classifier: String,
    val extension: String
) {
    val acKey: AC get() {
        // HACK: Fiji has at least one artifact it renames. Bummer!
        // Let's correct for that here, to ensure proper processing.
        // This map is the inverse of the one called "artifactsToFix" elsewhere.
        val renamedArtifacts = mapOf(
            "org.jocl.jocl" to "jocl"
        )
        return AC(renamedArtifacts[artifactId] ?: artifactId, classifier)
    }
}
data class Info(
    val fiji: Map<AC, Element>,
    val updateSite: Map<AC, Element>,
    val localFiles: MutableMap<AC, MutableList<File>>
)

/**
 * Determines whether the given file belongs in the specified destination directory, and if so,
 * does the necessary work to make preparations (e.g. delete other versions of the file).
 *
 * Returns true if the file is an appropriate fit for that directory and
 * should be copied, or null if not. SMELL THE DELICIOUS CODE.
 */
fun prepareToCopyFile(file: File, destDir: File, pathPrefix: String, info: Info): Boolean? {
    val davce = file.name.davce
    val ac = davce.acKey
    val corePlugin = info.fiji[ac]
    val updateSiteContents = info.updateSite[ac]
    val plugin = updateSiteContents ?: corePlugin
    val pluginPath = plugin?.filename?.dirname ?: ac.pathGuess
    if (pluginPath != pathPrefix) return null

    // FOUR CASES:
    // * core only -- skip but do sanity checks
    // * update site only -- overwrite
    // * core AND update site -- overwrite but warn
    // * neither -- overwrite but warn

    if (corePlugin != null && updateSiteContents == null) {
        // This file is provided by the core update site, not the current update site.
        // We don't want to shadow any core files (which we aren't already shadowing).
        logger.info("Skipping core file: ${file.name}")
        // But if the version on the core update site is older, let's point that out.
        val desiredVersion = davce.version
        val coreVersion = corePlugin.filename.davce.version
        if (desiredVersion compareVersions coreVersion > 0) {
            warn("Ignoring file ${corePlugin.filename} from the core update site which " +
                "is older than the desired dependency version ($desiredVersion > $coreVersion)")
        }
        return null
    }
    else {
        // Delete existing versions of this file already in the Fiji installation.
        for (localFile in info.localFiles[ac] ?: emptyList()) {
            logger.info("Deleting: $localFile")
            localFile.delete()
        }

        // Log the impending copy operation, and warn of any weird situations.
        logger.info("Copying: ${file.name} -> ${destDir.resolve(file.name)}")
        if (corePlugin != null) warn("${file.name} SHADOWS the core update site!")
        if (updateSiteContents == null) warn("${file.name} is NEW to the $updateSite update site!")
    }
    return true
}

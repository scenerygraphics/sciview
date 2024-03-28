package sciview

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList
import org.xml.sax.InputSource
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
import java.net.URL
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.GZIPInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

tasks {
    register("populateFiji") {
        dependsOn("jar")
        doLast {
            // Discern the path to the Fiji.app folder we are going to populate.
            val fijiPath = System.getProperty("fiji.dir") ?: "Fiji.app"
            logger.lifecycle("Populating ${fijiPath}...")
            val fijiDir = File(fijiPath)
            if (!fijiDir.isDirectory()) {
                error("No such directory: ${fijiDir.absolutePath}")
            }

            // Parse relevant update site databases. This information is useful
            // for deciding which JAR files to copy, and which ones to leave alone.
            val info = Info(
                db("ImageJ", "https://update.imagej.net"),
                db("Fiji", "https://update.fiji.sc"),
                db("Java-8", "https://sites.imagej.net/Java-8"),
                db("sciview", "https://sites.imagej.net/sciview"),
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

            // Copy sciview main artifact.
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
            for (platform in listOf("linux64", "macosx-arm64", "macosx", "win64")) {
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
            logger.info("--> Testing installation")
            val osName = System.getProperty("os.name")
            val osArch = System.getProperty("os.arch")
            val bits = if (osArch.contains("64")) "64" else "32"
            val launcher = when {
                osName == "Linux" -> "ImageJ-linux$bits"
                osName == "Darwin" -> "Contents/MacOS/ImageJ-macosx"
                osName.startsWith("Windows") -> "ImageJ-win$bits.exe"
                else -> null
            }
            if (launcher == null) warn("Skipping test for unknown platform: $osName-$osArch")
            else {
                logger.info("Launcher executable = $launcher")

                val baos = ByteArrayOutputStream()
                exec {
                    commandLine = listOf(
                        "$fijiDir/$launcher",
                        "--add-opens=java.base/java.lang=ALL-UNNAMED",
                        "--add-opens=java.base/java.util=ALL-UNNAMED",
                        "--",
                        "--headless",
                        "--run",
                        "sc.iview.commands.help.About"
                    )
                    standardOutput = baos
                }
                val stdout = baos.toString().trim()
                logger.info(stdout)
                val success = stdout.startsWith("[INFO] SciView was created by Kyle Harrington")
                if (!success) error("Test failed.")
                logger.info("Test passed.")
            }
        }
    }
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
        val cMatch = Regex(".*?-((native|windows|macos|linux).*)").matchEntire(noDir)
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
    val dbCacheDir = project.buildDir.resolve("tmp")
    Files.createDirectories(dbCacheDir.toPath())
    val dbXml = dbCacheDir.resolve("$siteName.xml.gz")
    val url = "$siteURL/db.xml.gz"
    if (!dbXml.exists()) download(url, dbXml)
    if (!dbXml.exists()) error("Download failed: $url")

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

fun download(url: String, dest: File) {
    logger.info("--> Downloading $url to $dest")
    URL(url).openStream().use { inputStream ->
        Files.copy(inputStream, dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }
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
        val isArm64 = classifier.contains("arm64")
        val subdir = when {
            isLinux -> "linux64"
            isMac && isArm64 -> "macosx-arm64"
            isMac -> "macosx"
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
    val imagej: Map<AC, Element>,
    val fiji: Map<AC, Element>,
    val java8: Map<AC, Element>,
    val sciview: Map<AC, Element>,
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
    val corePlugin = info.java8[ac] ?: info.fiji[ac] ?: info.imagej[ac]
    val sciviewPlugin = info.sciview[ac]
    val plugin = sciviewPlugin ?: corePlugin
    val pluginPath = plugin?.filename?.dirname ?: ac.pathGuess
    if (pluginPath != pathPrefix) return null

    // FOUR CASES:
    // * core only -- skip but do sanity checks
    // * sciview only -- overwrite
    // * core AND sciview -- overwrite but warn
    // * neither -- overwrite but warn

    if (corePlugin != null && sciviewPlugin == null) {
        // This file is provided by the core update site, not the sciview update site.
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
        // Delete existing versions of this file already in the Fiji.app installation.
        for (localFile in info.localFiles[ac] ?: emptyList()) {
            logger.info("Deleting: $localFile")
            localFile.delete()
        }

        // Log the impending copy operation, and warn of any weird situations.
        logger.info("Copying: ${file.name} -> ${destDir.resolve(file.name)}")
        if (corePlugin != null) warn("${file.name} SHADOWS the core update site!")
        if (sciviewPlugin == null) warn("${file.name} is NEW to the sciview update site!")
    }
    return true
}

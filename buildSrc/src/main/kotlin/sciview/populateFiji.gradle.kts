package sciview

import java.io.ByteArrayOutputStream

tasks {
    register("populateFiji") {

        group = "sciview"

        doLast {

            val os = "uname -s".read()
            val launcher = when {
                os == "Linux" -> when ("uname -m".read()) {
                    "x86_64" -> "ImageJ-linux64"
                    else -> "ImageJ-linux32"
                }
                os == "Darwin" -> "Contents/MacOS/ImageJ-macosx"
                os.startsWith("MING") -> "ImageJ-win32.exe"
                os.startsWith("MSYS_NT") -> "ImageJ-win32.exe"
                else -> error("Unknown platform")
            }

            // Check if we have a path given, in that case we do not download a new Fiji, but use the path given --
            //            if [ -z "$1" ]
            //            then
            println("--> Installing into pristine Fiji installation")
            //            echo "--> If you want to install into a pre-existing Fiji installation, run as"
            //            echo "     $0 path/to/Fiji.app"
            //            # -- Determine correct ImageJ launcher executable --

            // Roll out a fresh Fiji

            if (!file("fiji-nojre.zip").exists()) {
                println("--> Downloading Fiji")
                "curl -L -O https://downloads.imagej.net/fiji/latest/fiji-nojre.zip".ex()
            }

            println("--> Unpacking Fiji")
            "rm -rf Fiji.app".ex()
            "unzip fiji-nojre.zip".ex()

            println("--> Updating Fiji")
            //            "Fiji.app/$launcher --update update-force-pristine".ex()
            val fijiDirectory = "Fiji.app"

            println("--> Copying dependencies into Fiji installation")
            copy {
                //                from(configurations.named<Configuration>("default")).into("$fijiDirectory/dependencies")
                from(configurations.named<Configuration>("default")).into("$fijiDirectory/jars")
            }

            // Now that we populated fiji, let's double check that it works --

            println("--> Testing installation with command: sc.iview.commands.help.About")
            val about = "$fijiDirectory//$launcher --headless --run sc.iview.commands.help.About".read()
            println("Test " + when {
                about.isEmpty() -> "failed"
                else -> "passed"
            })
        }
    }
}

fun String.ex() = exec { commandLine = split(' ') }

fun String.read(): String {
    val res = ByteArrayOutputStream()
    exec {
        commandLine = split(' ')
        standardOutput = res
    }
    return res.toString().trim()
}
package sc.iview.commands.view

import com.google.common.io.Files
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SAVE_CAMERA_CONFIGURATION
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Save the current camera configuration to file.
 *
 * @author Kyle Harrington
 */
@Suppress("UnstableApiUsage")
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Save Camera Configuration", weight = VIEW_SAVE_CAMERA_CONFIGURATION)])
class SaveCameraConfiguration : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var saveFile: File

    override fun run() {
        try {
            val cam = sciView.camera ?: return

            val fw = FileWriter(saveFile)
            val bw = BufferedWriter(fw)
            if (!Files.getFileExtension(saveFile.absolutePath).equals("clj", ignoreCase = true)) throw IOException("File must be Clojure (extension = .clj)")
            val pos = cam.position
            val rot = cam.rotation
            var scriptContents = "; @SciView sciView\n\n"
            scriptContents += """(.setPosition (.getCamera sciView) (cleargl.GLVector. (float-array [${pos.x()} ${pos.y()} ${pos.z()}])))
"""
            scriptContents += """(.setRotation (.getCamera sciView) (com.jogamp.opengl.math.Quaternion. ${rot.x()} ${rot.y()} ${rot.z()} ${rot.w()}))
"""
            bw.write(scriptContents)
            bw.close()
            fw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
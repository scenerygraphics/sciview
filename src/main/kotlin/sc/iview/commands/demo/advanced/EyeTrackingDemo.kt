package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackedDeviceType
import graphics.scenery.controls.eyetracking.PupilEyeTracker
import graphics.scenery.textures.Texture
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import graphics.scenery.volumes.Volume
import net.imglib2.type.numeric.integer.UnsignedByteType
import org.joml.*
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.awt.image.DataBufferByte
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashMap
import javax.imageio.ImageIO
import kotlin.concurrent.thread
import kotlin.math.PI
import graphics.scenery.attribute.material.Material
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard

@Plugin(type = Command::class,
        menuRoot = "SciView",
        menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
            Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
            Menu(label = "Utilize Eye Tracker for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
/**
 * Command class that forwards to the [EyeTracking] class to perform the actual tracking and analysis.
 */
class EyeTrackingDemo: Command {

    @Parameter
    var mastodonCallbackLinkCreate: ((HedgehogAnalysis.SpineGraphVertex) -> Unit)? = null

    @Parameter
    var mastodonUpdateGraph: (() -> Unit)? = null

    @Parameter
    private lateinit var sv: SciView

    override fun run() {
        // the actual eye tracking logic happens in here
        val eyeTracking = EyeTracking(mastodonCallbackLinkCreate, mastodonUpdateGraph, sv)
        eyeTracking.run()
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            argmap["sv"] = sv
            command.run(EyeTrackingDemo::class.java, true, argmap)
        }
    }
}
package sc.iview.commands.demo.advanced

import graphics.scenery.*
import graphics.scenery.utils.SystemHelpers
import graphics.scenery.utils.extensions.minus
import graphics.scenery.utils.extensions.xyz
import graphics.scenery.utils.extensions.xyzw
import graphics.scenery.volumes.Volume
import org.joml.*
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Plugin
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.nio.file.Files
import java.nio.file.Paths
import java.util.HashMap
import kotlin.concurrent.thread
import graphics.scenery.attribute.material.Material
import graphics.scenery.controls.*
import graphics.scenery.primitives.Cylinder
import graphics.scenery.primitives.TextBoard
import org.scijava.plugin.Parameter

@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
        Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
        Menu(label = "Utilize VR Controller for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)]
)
class VRControllerTrackingCommand : Command {

    @Parameter
    private lateinit var sv: SciView

    override fun run() {
        val tracking = VRControllerTracking(sv)
        tracking.run()
    }

    companion object {
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            argmap["sv"] = sv
            command.run(VRControllerTrackingCommand::class.java, true, argmap)
        }
    }
}
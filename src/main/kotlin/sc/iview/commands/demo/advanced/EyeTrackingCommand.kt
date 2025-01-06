package sc.iview.commands.demo.advanced

import graphics.scenery.*
import org.joml.*
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.demo.advanced.HedgehogAnalysis.SpineGraphVertex
import java.util.HashMap

@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
        Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
        Menu(label = "Utilize Eye Tracker for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)]
)
/**
 * Command class that forwards to the [EyeTracking] class to perform the actual tracking and analysis.
 */
class EyeTrackingCommand : Command {

    @Parameter
    var mastodonCallbackLinkCreate: ((List<Pair<Vector3f, SpineGraphVertex>>) -> Unit)? = null

    @Parameter
    var mastodonUpdateGraph: (() -> Unit)? = null

    @Parameter
    var mastodonAddSpot: ((Int, Vector3f) -> Unit)? = null

    @Parameter
    var mastodonSelectSpot: ((Vector3f, Int) -> Unit)? = null

    @Parameter
    private lateinit var sv: SciView

    override fun run() {
        // the actual eye tracking logic happens in here
        val eyeTracking = EyeTracking(mastodonCallbackLinkCreate, mastodonUpdateGraph, mastodonAddSpot, mastodonSelectSpot, sv)
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
            command.run(EyeTrackingCommand::class.java, true, argmap)
        }
    }
}
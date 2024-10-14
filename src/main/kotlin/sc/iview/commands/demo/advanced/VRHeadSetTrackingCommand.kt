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
import java.io.File
import java.util.HashMap
import graphics.scenery.controls.behaviours.*
import graphics.scenery.utils.extensions.*
import org.scijava.event.EventService
import sc.iview.commands.file.OpenDirofTif

@Plugin(
    type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
        Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
        Menu(label = "Utilize VR Headset for Cell Tracking", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)]
)
class VRHeadSetTrackingCommand : Command {

    @Parameter
    private lateinit var eventService: EventService

    @Parameter
    private lateinit var sv: SciView

    override fun run() {
        val tracking = VRHeadsetTracking(sv, eventService)
        tracking.run()
    }

    companion object {
        //run function from here, it will automatically choose the volume for rendering, please give the correct location of volume
        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            // TODO this should probably open a file open dialog instead of hardcoding a path?
            command.run(OpenDirofTif::class.java, true,
                hashMapOf<String,Any>(
                    "file" to File("E:\\dataset\\Pdu_H2BeGFP_CAAXmCherry_0123_20130312_192018.corrected-histone"),
                    "onlyFirst" to 10
                ))
                .get()

            val argmap = HashMap<String, Any>()
            argmap["sv"] = sv
            command.run(VRHeadSetTrackingCommand::class.java, true, argmap)
                .get()
        }
    }
}
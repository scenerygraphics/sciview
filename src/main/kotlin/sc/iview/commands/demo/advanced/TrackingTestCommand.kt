package sc.iview.commands.demo.advanced

import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights

@Plugin(type = Command::class,
    menuRoot = "SciView",
    menu = [Menu(label = "Demo", weight = MenuWeights.DEMO),
        Menu(label = "Advanced", weight = MenuWeights.DEMO_ADVANCED),
        Menu(label = "Test without VR and Eye Tracker", weight = MenuWeights.DEMO_ADVANCED_EYETRACKING)])
class TrackingTestCommand: Command {

    @Parameter
    private lateinit var sv: SciView

    override fun run() {
        val test = TrackingTest(sv)
        test.run()
    }

    companion object {

        @Throws(Exception::class)
        @JvmStatic
        fun main(args: Array<String>) {
            val sv = SciView.create()
            val command = sv.scijavaContext!!.getService(CommandService::class.java)
            val argmap = HashMap<String, Any>()
            argmap["sv"] = sv
            command.run(TrackingTestCommand::class.java, true, argmap)
        }
    }

}
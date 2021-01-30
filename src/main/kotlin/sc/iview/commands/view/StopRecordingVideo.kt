package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_STOP_RECORDING_VIDEO

/**
 * Command to stop recording the current video
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Stop recording video", weight = VIEW_STOP_RECORDING_VIDEO)])
class StopRecordingVideo : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.toggleRecordVideo()
    }
}
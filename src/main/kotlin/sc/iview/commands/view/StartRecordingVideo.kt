package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_START_RECORDING_VIDEO
import kotlin.math.max

/**
 * Command to start recording a video. Currently this will record to ~/Desktop
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Start recording video", weight = VIEW_START_RECORDING_VIDEO)])
class StartRecordingVideo : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private var bitrate = 10000000 // 10 MBit

    @Parameter(choices = ["VeryLow", "Low", "Medium", "High", "Ultra", "Insane"], style = "listBox")
    private lateinit var videoEncodingQuality // listed as an enum here, cant access from java https://github.com/scenerygraphics/scenery/blob/1a451c2864e5a48e47622d9313fe1681e47d7958/src/main/kotlin/graphics/scenery/utils/H264Encoder.kt#L65
            : String

    override fun run() {
        bitrate = max(0, bitrate)
        sciView.getScenerySettings().set("VideoEncoder.Bitrate", bitrate)
        sciView.getScenerySettings().set("VideoEncoder.Quality", videoEncodingQuality)
        sciView.toggleRecordVideo()
    }
}
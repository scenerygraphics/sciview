package sc.iview.commands.view

import net.imagej.ImgPlus
import org.scijava.ItemIO
import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SCREENSHOT

/**
 * Command to take a screenshot. The screenshot is opened in ImageJ.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Screenshot", weight = VIEW_SCREENSHOT)])
class Screenshot : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var uiService: UIService

    @Parameter(type = ItemIO.OUTPUT)
    private var img: ImgPlus<*>? = null

    override fun run() {
        val screenshot = sciView.aRGBScreenshot
        img = ImgPlus(screenshot)
        uiService.show(img)
    }
}
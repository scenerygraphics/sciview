package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_RENDER_TO_OPENVR

/**
 * Activates rendering to an OpenVR headset
 *
 * @author Ulrik Guenther
 */
@Plugin(type = Command::class, initializer = "initValues", menuRoot = "SciView", selectable = true, menu = [Menu(label = "View", weight = VIEW), Menu(label = "Render to OpenVR Headset", weight = VIEW_RENDER_TO_OPENVR)])
class RenderToOpenVRHMD : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.toggleVRRendering()
    }
}
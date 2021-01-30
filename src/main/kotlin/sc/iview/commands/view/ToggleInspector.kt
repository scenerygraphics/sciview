package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_TOGGLE_INSPECTOR

/**
 * Command that displays a [SwingNodePropertyEditor] window.
 *
 * @author Curtis Rueden
 */
@Plugin(type = Command::class, initializer = "initValues", menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Toggle Inspector", weight = VIEW_TOGGLE_INSPECTOR)])
class ToggleInspector : Command {
    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.toggleInspectorWindow()
    }
}
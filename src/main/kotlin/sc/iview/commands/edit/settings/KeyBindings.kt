package sc.iview.commands.edit.settings

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights

/**
 * A command for interactively editing input controls.
 * @author Vladimir Ulman
 * @author Ulrik Guenther
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Edit", weight = MenuWeights.EDIT), Menu(label = "Settings", weight = MenuWeights.EDIT_SETTINGS), Menu(label = "Key Bindings", weight = MenuWeights.EDIT_SETTINGS_BINDINGS)])
class KeyBindings : Command {

    @Parameter
    private lateinit var sciView: SciView

    override fun run() {
        sciView.publicGetInputHandler().openKeybindingsGuiEditor("SciView's Key bindings editor", ".sciview.keybindings.yaml", "all")
    }
}
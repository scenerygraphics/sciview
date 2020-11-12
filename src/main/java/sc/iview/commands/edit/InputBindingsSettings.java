package sc.iview.commands.edit;

import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.*;

/**
 * A command for interactively editing input controls.
 * @author Vladimir Ulman
 */
@Plugin(type = Command.class, menuRoot = "SciView",
        menu = { @Menu(label = "Edit", weight = EDIT), @Menu(label = "Inputs Bindings", weight = EDIT_SCIVIEW_SETTINGS+2) })
public class InputBindingsSettings implements Command
{
    @Parameter
    private SciView sciView;

    @Override
    public void run() {
        sciView.publicGetInputHandler().openKeybindingsGuiEditor( "SciView's Key bindings editor" );
    }
}

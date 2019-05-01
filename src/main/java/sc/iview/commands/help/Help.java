package sc.iview.commands.help;


import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import static sc.iview.commands.MenuWeights.HELP;
import static sc.iview.commands.MenuWeights.HELP_HELP;
import static sc.iview.commands.view.NodePropertyEditor.USAGE_TEXT;

@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Help", weight = HELP), //
                 @Menu(label = "Help", weight = HELP_HELP) })
public class Help implements Command {

    @Parameter
    private UIService uiService;

    @Override
    public void run() {
        uiService.showDialog( "<html>" + USAGE_TEXT + "</html>", "SciView Usage");
    }
}

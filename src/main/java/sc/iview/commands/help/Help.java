package sc.iview.commands.help;


import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.ui.behaviour.InputTrigger;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.HELP;
import static sc.iview.commands.MenuWeights.HELP_HELP;
import static sc.iview.commands.view.NodePropertyEditor.USAGE_TEXT;

@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Help", weight = HELP), //
                 @Menu(label = "Help", weight = HELP_HELP) })
public class Help implements Command {

    @Parameter
    private SciView sciView;

    @Parameter
    private UIService uiService;

    public String getKeybinds() {
        StringBuilder helpString = new StringBuilder();
        for( InputTrigger trigger : sciView.getSceneryInputHandler().getAllBindings().keySet() ) {
            helpString.append(trigger).append("\t-\t").append(sciView.getSceneryInputHandler().getAllBindings().get(trigger)).append("\n");
        }
        return helpString.toString();
    }

    @Override
    public void run() {
        uiService.showDialog( "<html>" + USAGE_TEXT + "<br><br>" + getKeybinds() + "", "SciView Usage");
    }
}

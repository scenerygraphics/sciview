package sc.iview.commands.help;


import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

import static sc.iview.commands.MenuWeights.*;

/**
 * Command to show information about the authors of SciView
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Help", weight = HELP), //
                 @Menu(label = "About", weight = HELP_ABOUT) })
public class About implements Command {

    @Parameter
    private UIService uiService;

    private String ABOUT_TEXT = "SciView was created by Kyle Harrington and Ulrik G&uuml;nther.<br>" +
            "Other key contributors include: Curtis Rueden, Aryaman Gupta, Tobias Pietzsch, Robert Haase, Jan Eglinger, and Stephan Saalfeld.<br>" +
            "Resources files were contributed by: Robert Wiese, and Kyle Harrington.<br>" +
            "Citation information will be available in the near future.";

    @Override
    public void run() {
        uiService.showDialog( "<html>" + ABOUT_TEXT + "</html>", "About SciView");
    }
}

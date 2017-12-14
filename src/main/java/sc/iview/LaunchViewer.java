package sc.iview;

import org.scijava.command.Command;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Created by kharrington on 6/20/17.
 */
@Plugin(type = Command.class,
        menuPath = "SciView>Launch")
public class LaunchViewer implements Command {
    
    @Parameter
    private DisplayService displayService;

    @Parameter
    SciView sciView;

    @Override
    public void run() {

    }

}


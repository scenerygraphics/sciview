package sc.iview;

import org.scijava.command.Command;
import org.scijava.display.DisplayService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Created by kharrington on 6/20/17.
 */
@Plugin(type = Command.class,
        menuPath = "Scenery>Launch")
public class LaunchViewer implements Command {
    
    @Parameter
    private DisplayService displayService;

    @Override
    public void run() {
    	SciView sv = new SciView();
		sv.main();
		displayService.createDisplay(sv);
    }

}


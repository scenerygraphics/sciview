package graphics.scenery.viewer;

import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Created by kharrington on 6/20/17.
 */
@Plugin(type = Command.class,
        menuPath = "Scenery>Launch")
public class LaunchViewer implements Command {

    @Parameter
    SceneryService scenery;

    @Override
    public void run() {
        scenery.createSceneryViewer();
    }

}


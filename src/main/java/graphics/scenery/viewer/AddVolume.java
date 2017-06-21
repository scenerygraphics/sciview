package graphics.scenery.viewer;

import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Created by kharrington on 6/21/17.
 */
@Plugin(type = Command.class,
        menuPath = "Scenery>Add>Volume")
public class AddVolume implements Command {

    @Parameter
    private Dataset image;

    @Parameter
    private SceneryService sceneryService;

    @Parameter
    private float voxelWidth;

    @Parameter
    private float voxelHeight;

    @Parameter
    private float voxelDepth;

    @Override
    public void run() {
        sceneryService.getActiveSceneryViewer().addVolume(image,new float[]{voxelWidth,voxelHeight,voxelDepth});
    }

}

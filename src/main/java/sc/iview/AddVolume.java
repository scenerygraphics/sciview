package sc.iview;

import net.imagej.Dataset;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

/**
 * Created by kharrington on 6/21/17.
 */
@Plugin(type = Command.class,
        menuPath = "SciView>Add>Volume")
public class AddVolume implements Command {

    @Parameter
    private Dataset image;

    @Parameter
    private float voxelWidth;

    @Parameter
    private float voxelHeight;

    @Parameter
    private float voxelDepth;

    @Parameter
    SciView sciView;

    @Parameter
    private LogService logService;

    @Override
    public void run() {
        sciView.addVolume(image,new float[]{voxelWidth,voxelHeight,voxelDepth},logService);
    }

}

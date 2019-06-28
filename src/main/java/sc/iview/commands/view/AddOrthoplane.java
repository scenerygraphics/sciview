package sc.iview.commands.view;

import graphics.scenery.volumes.Volume;
import io.scif.services.DatasetIOService;
import net.imagej.Dataset;
import net.imagej.ImgPlus;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;
import sc.iview.commands.demo.ResourceLoader;
import sc.iview.shape.OrthoSlice;

import java.io.File;
import java.io.IOException;

import static sc.iview.commands.MenuWeights.VIEW;
import static sc.iview.commands.MenuWeights.VIEW_SET_SUPERSAMPLING_FACTOR;

@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = {@Menu(label = "View", weight = VIEW), //
                @Menu(label = "Add Orthoplane", weight = VIEW_SET_SUPERSAMPLING_FACTOR)})
public class AddOrthoplane implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    @Parameter
    private DatasetIOService datasetIO;

    @Override
    public void run() {

        final Dataset cube;
        try {
            File cubeFile = ResourceLoader.createFile( getClass(), "/cored_cube_var2_8bit.tif" );

            cube = datasetIO.open( cubeFile.getAbsolutePath() );
        }
        catch (IOException exc) {
            logService.error( exc );
            return;
        }

        Volume v = (Volume) sciView.addVolume( cube, new float[] { 1, 1, 1 } );
        v.setPixelToWorldRatio(0.1f);
        v.setName( "Volume Render Demo" );
        v.setDirty(true);
        v.setNeedsUpdate(true);

        sciView.setActiveNode(v);
        sciView.centerOnNode( sciView.getActiveNode() );

        OrthoSlice os = new OrthoSlice(sciView, cube);
        os.paint();

//        if( sciView.getActiveNode() instanceof Volume) {
//            Volume v = ((Volume) sciView.getActiveNode());
//
//
//
//        }

    }

}

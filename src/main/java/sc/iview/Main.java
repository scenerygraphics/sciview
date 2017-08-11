package sc.iview;

import net.imagej.Dataset;
import net.imagej.ImageJ;

import java.io.IOException;

/**
 * Created by kharrington on 6/20/17.
 */
public class Main {
    public static void main(String... args) throws IOException {
        ImageJ ij = new ImageJ();

        //ij.launch(args);
        if( !ij.ui().isVisible() )
            ij.ui().showUI();

//        // Volume render test
//        SciView sciView = ((SciViewService) ij.getContext().getService( "sc.iview.SciViewService" )).getOrCreateActiveSciView();
//        Dataset testImg = (Dataset) ij.io().open( "/Users/kharrington/git/SciView/resources/cored_cube.tif" );
//        sciView.addVolume( testImg, new float[]{1,1,1} );

        //SceneryService sceneryService = ij.getContext().getService(SceneryService.class);

        //sceneryService.createSceneryViewer();

    }
}

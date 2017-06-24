package sc.iview;

import net.imagej.ImageJ;

/**
 * Created by kharrington on 6/20/17.
 */
public class Main {
    public static void main(String... args)
    {
        ImageJ ij = new ImageJ();

        //ij.launch(args);
        if( !ij.ui().isVisible() )
            ij.ui().showUI();

        //SceneryService sceneryService = ij.getContext().getService(SceneryService.class);

        //sceneryService.createSceneryViewer();

    }
}

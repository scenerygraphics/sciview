package sc.fiji.threed;

import net.imagej.Position;
import net.imagej.display.DataView;
import net.imglib2.RealLocalizable;

/**
 * Created by kharrington on 6/16/17.
 */
public interface SceneryView extends DataView {
    public RealLocalizable getCameraPosition();

    public Scenery getScenery();
}

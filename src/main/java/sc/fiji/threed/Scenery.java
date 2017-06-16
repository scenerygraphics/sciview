package sc.fiji.threed;

import graphics.scenery.Scene;
import net.imagej.Data;

/**
 * A Scenery object the primary data structure for encapsulating a 3D visualization environment. It packages together
 * a number of renderable nodes, including volumes and meshes, through {@link Scene}.
 *
 * @author Kyle Harrington (University of Idaho, Moscow)
 */
public interface Scenery extends Data {

    /** Return the SceneryViewer for this object */
    SceneryViewer getViewer();

}

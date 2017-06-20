package graphics.scenery.viewer;

import net.imagej.AbstractData;
import net.imglib2.RealInterval;
import org.scijava.Context;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;

/**
 * Created by kharrington on 6/16/17.
 */
public class DefaultScenery extends AbstractData implements Scenery {

    @Parameter
    private LogService log;

    private SceneryViewer viewer;

    private String name = "SceneryViewer";

    public DefaultScenery(final Context context,
                          RealInterval interval,
                          final SceneryViewer viewer)
    {
        super(context,interval);
        this.viewer = viewer;
    }

    @Override
    public SceneryViewer getViewer() {
        return viewer;
    }

    @Override
    public void update() {
        /* Scenery is continuously rendered, but we could force an update here. */
    }

    @Override
    public void rebuild() {
        /* Scenery is continuously rendered, but we could force a rebuild here. */
    }
}

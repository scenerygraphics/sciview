package sc.fiji.threed;

import net.imagej.display.ImageDisplayService;
import org.scijava.display.DisplayService;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;


/**
 * Default service for rendering inside Scenery.
 *
 * @author Kyle Harrington (University of Idaho, Moscow)
 */
@Plugin(type = Service.class)
public class DefaultSceneryService extends AbstractService
    implements SceneryService
{
    @Parameter
    private EventService eventService;

    @Parameter
    private PluginService pluginService;

    @Parameter
    private DisplayService displayService;


}

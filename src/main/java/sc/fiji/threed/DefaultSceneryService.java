package sc.fiji.threed;

import net.imagej.Data;
import net.imagej.Position;
import net.imagej.display.DataView;
import net.imagej.display.ImageDisplayService;
import org.scijava.display.DisplayService;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import java.util.List;


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

    @Parameter
    private ScriptService scriptService;

    @Override
    public EventService getEventService() {
        return eventService;
    }

    @Override
    public DisplayService getDisplayService() {
        return displayService;
    }

    @Override
    public DataView createDataView(Data data) {
        for (final DataView dataView : getDataViews()) {
            if (dataView.isCompatible(data)) {
                dataView.initialize(data);
                return dataView;
            }
        }
        throw new IllegalArgumentException("No data view found for data: " + data);
    }

    @Override
    public List<? extends DataView> getDataViews() {
        return pluginService.createInstancesOfType(DataView.class);
    }

    @Override
    public SceneryDisplay getActiveSceneryDisplay() {
        return displayService.getActiveDisplay(SceneryDisplay.class);
    }

    @Override
    public Scenery getActiveScenery() {
        return getActiveScenery(getActiveSceneryDisplay());
    }

    @Override
    public SceneryView getActiveSceneryView() {
        return getActiveSceneryView(getActiveSceneryDisplay());
    }

    @Override
    public Position getActivePosition() {
        return getActivePosition(getActiveSceneryDisplay());
    }

    @Override
    public Scenery getActiveScenery(SceneryDisplay display) {
        final SceneryView activeSceneryView = getActiveSceneryView(display);
        return activeSceneryView == null ? null : activeSceneryView.getScenery();
    }

    @Override
    public SceneryView getActiveSceneryView(SceneryDisplay display) {
        if (display == null) return null;
        final DataView activeView = display.getActiveView();
        if (activeView instanceof SceneryView) {
            return (SceneryView) activeView;
        }
        return null;
    }

    @Override
    public List<SceneryDisplay> getSceneryDisplay() {
        return displayService.getDisplaysOfType(SceneryDisplay.class);
    }

    @Override
    public Position getActivePosition(SceneryDisplay display) {
        if (display == null) return null;
        final SceneryView activeSceneryView = this.getActiveSceneryView(display);
        if (activeSceneryView == null) return null;
        //return activeSceneryView.getCameraPosition();
        /* TODO */
        return null;
    }

    @Override
    public void initialize() {
        scriptService.addAlias(SceneryDisplay.class);
        scriptService.addAlias(SceneryView.class);
        scriptService.addAlias(DataView.class);
    }
}

package graphics.scenery.viewer;

import net.imagej.Data;
import net.imagej.Position;
import net.imagej.display.DataView;
import org.scijava.display.DisplayService;
import org.scijava.display.event.window.WinClosedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

import java.util.LinkedList;
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

    /* Parameters */
    

    /* Instance variables */

    private List<SceneryViewer> sceneryViewers =
            new LinkedList<>();

    /* Methods */

    public SceneryViewer getActiveSceneryViewer() {
        if ( sceneryViewers.size() > 0 )
            return sceneryViewers.get(0);
        return null;
    }

    public SceneryViewer getSceneryViewer(String name) {
        for( final SceneryViewer sceneryViewer : sceneryViewers ) {
            if( name.equalsIgnoreCase(sceneryViewer.getName())) {
                return sceneryViewer;
            }
        }
        return null;
    }

    public void createSceneryViewer() {
        SceneryViewer v = new SceneryViewer();

        // Maybe should use thread service instead
        Thread viewerThread = new Thread(){
            public void run() {
                v.main();
            }
        };
        viewerThread.start();

        sceneryViewers.add(v);
    }

    @Override
    public int numSceneryViewers() {
        return sceneryViewers.size();
    }
}

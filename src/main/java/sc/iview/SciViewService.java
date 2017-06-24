package sc.iview;

import net.imagej.Data;
import net.imagej.ImageJService;
import net.imagej.Position;
import net.imagej.display.DataView;
import org.scijava.display.DisplayService;
import org.scijava.event.EventService;

import java.util.List;

/**
 * Interface for services that work with Scenery.
 *
 * @author Kyle Harrington (University of Idaho, Moscow)
 */
public interface SciViewService extends ImageJService {

    public SciView getActiveSceneryViewer();
    
    public SciView getOrCreateActiveSciView();

    public SciView getSceneryViewer(String name);

    public void createSceneryViewer();

    public int numSceneryViewers();
}

package sc.iview;

import net.imagej.ImageJService;

/**
 * Interface for services that work with Scenery.
 *
 * @author Kyle Harrington (University of Idaho, Moscow)
 */
public interface SciViewService extends ImageJService {

    public SciView getActiveSciView();
    
    public SciView getOrCreateActiveSciView();

    public SciView getSciView(String name);

    public void createSciView();

    public int numSciView();
}

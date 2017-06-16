package sc.fiji.threed;

import net.imagej.ImageJService;

/**
 * Interface for services that work with Scenery.
 *
 * @author Kyle Harrington (University of Idaho, Moscow)
 */
public interface SceneryService extends ImageJService {

    EventService getEventService();

    DisplayService getDisplayService();

    /** Creates a new {@link DataView} wrapping the given data object. */
    DataView createDataView(Data data);

    /**
     * Gets the list of available DataViews. See ImageDisplayService's explanation for more details
     */
    List<? extends DataView> getDataViews();

    /** Gets the currently active {@link SceneryDisplay}. */
    SceneryDisplay getActiveSceneryDisplay();

    /**
     * Gets the active {@link Scenery}, if any, of the currently active
     * {@link SceneryDisplay}.
     */
    Scenery getActiveScenery();

    /**
     * Gets the active {@link SceneryView}, if any, of the currently active
     * {@link SceneryDisplay}.
     */
    SceneryView getActiveSceneryView();

    /**
     * Gets the active {@link Position}, if any, of the currently active
     * {@link SceneryDisplay}.
     */
    Position getActivePosition();

    /**
     * Get the active {@link Scenery}, if any, of the given {@link SceneryDisplay}.
     */
    Scenery getActiveScenery(SceneryDisplay scenery);

    /**
     * Gets the active {@link SceneryView}
     */
    SceneryView getActiveSceneryView(SceneryDisplay display);

    /** Gets a list of all available {@link SceneryDisplay}s. */
    List<SceneryDisplay> getSceneryDisplay();

    /**
     * Gets the active {@link Position}, if any, of the active {@link SceneryView} in the given {@link SceneryDisplay}.
     */
    Position getActivePosition(SceneryDisplay display);
}

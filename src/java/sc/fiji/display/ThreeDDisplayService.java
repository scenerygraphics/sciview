package sc.fiji.display;

import java.util.List;

import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.Position;
import net.imagej.display.DataView;
import net.imagej.display.DatasetView;

import org.scijava.display.DisplayService;
import org.scijava.event.EventService;
import org.scijava.plugin.PluginService;

public interface ThreeDDisplayService extends ImageJService {

	EventService getEventService();

	PluginService getPluginService();

	DisplayService getDisplayService();

	/** Creates a new {@link DataView} wrapping the given data object. */
	DataView createDataView(Data data);

	/**
	 * Gets the list of available {@link DataView}s. The list will contain one
	 * uninitialized instance of each {@link DataView} implementation known to the
	 * {@link PluginService}.
	 */
	List<? extends DataView> getDataViews();

	/** Gets the currently active {@link ImageDisplay}. */
	ThreeDDisplay getActiveThreeDDisplay();

	/**
	 * Gets the active {@link Dataset}, if any, of the currently active
	 * {@link ImageDisplay}.
	 */
	Dataset getActiveDataset();

	/**
	 * Gets the active {@link DatasetView}, if any, of the currently active
	 * {@link ImageDisplay}.
	 */
	DatasetView getActiveDatasetView();
	
	/** 
	 * Gets the active {@link Position}, if any, of the currently active
	 * {@link ImageDisplay}.
	 */
	Position getActivePosition();

	/**
	 * Gets the active {@link Dataset}, if any, of the given {@link ImageDisplay}.
	 */
	Dataset getActiveDataset(ThreeDDisplay display);

	/**
	 * Gets the active {@link DatasetView}, if any, of the given
	 * {@link ImageDisplay}.
	 */
	DatasetView getActiveDatasetView(ThreeDDisplay display);

	/** Gets a list of all available {@link ImageDisplay}s. */
	List<ThreeDDisplay> getThreeDDisplays();
	
	/** 
	 * Gets the active {@link Position}, if any, of the active
	 * {@link DatasetView} in the given {@link ImageDisplay}.
	 */
	Position getActivePosition(ThreeDDisplay display);

}

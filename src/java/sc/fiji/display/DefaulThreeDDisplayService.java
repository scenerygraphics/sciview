package sc.fiji.display;

import java.util.List;

import net.imagej.Data;
import net.imagej.Dataset;
import net.imagej.Position;
import net.imagej.display.DataView;
import net.imagej.display.DatasetView;
import net.imagej.display.OverlayView;
import net.imagej.overlay.Overlay;

import org.scijava.display.DisplayService;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;

@Plugin(type = Service.class)
public final class DefaulThreeDDisplayService extends AbstractService
	implements ThreeDDisplayService
{

	@Parameter
	private EventService eventService;

	@Parameter
	private PluginService pluginService;

	@Parameter
	private DisplayService displayService;

	@Parameter
	private ScriptService scriptService;

	// -- ImageDisplayService methods --

	@Override
	public EventService getEventService() {
		return eventService;
	}

	@Override
	public PluginService getPluginService() {
		return pluginService;
	}

	@Override
	public DisplayService getDisplayService() {
		return displayService;
	}

	@Override
	public DataView createDataView(final Data data) {
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
	public ThreeDDisplay getActiveThreeDDisplay() {
		return displayService.getActiveDisplay(ThreeDDisplay.class);
	}

	@Override
	public Dataset getActiveDataset() {
		return getActiveDataset(getActiveThreeDDisplay());
	}

	@Override
	public DatasetView getActiveDatasetView() {
		return getActiveDatasetView(getActiveThreeDDisplay());
	}
	
	@Override
	public Position getActivePosition() {
		return getActivePosition(getActiveThreeDDisplay());
	}

	@Override
	public Dataset getActiveDataset(final ThreeDDisplay display) {
		final DatasetView activeDatasetView = getActiveDatasetView(display);
		return activeDatasetView == null ? null : activeDatasetView.getData();
	}

	@Override
	public DatasetView getActiveDatasetView(final ThreeDDisplay display) {
		if (display == null) return null;
		final DataView activeView = display.getActiveView();
		if (activeView instanceof DatasetView) {
			return (DatasetView) activeView;
		}
		return null;
	}
	
	@Override
	public Position getActivePosition(final ThreeDDisplay display) {
		if (display == null) return null;
		final DatasetView activeDatasetView = this.getActiveDatasetView(display);
		if(activeDatasetView == null) return null;
		return activeDatasetView.getPlanePosition();
	}

	@Override
	public List<ThreeDDisplay> getThreeDDisplays() {
		return displayService.getDisplaysOfType(ThreeDDisplay.class);
	}

	@Override
	public void initialize() {
		scriptService.addAlias(ThreeDDisplay.class);
		scriptService.addAlias(DatasetView.class);
		scriptService.addAlias(DataView.class);
		scriptService.addAlias(OverlayView.class);
		scriptService.addAlias(Overlay.class);
	}
}

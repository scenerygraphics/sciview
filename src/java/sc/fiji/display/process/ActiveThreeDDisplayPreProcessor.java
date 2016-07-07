package sc.fiji.display.process;

import net.imagej.display.process.SingleInputPreprocessor;
import sc.fiji.display.ThreeDDisplay;
import sc.fiji.display.ThreeDDisplayService;

import org.scijava.Priority;
import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;


@Plugin(type = PreprocessorPlugin.class, priority = Priority.VERY_HIGH_PRIORITY)
public class ActiveThreeDDisplayPreProcessor extends
	SingleInputPreprocessor<ThreeDDisplay>
{

	@Parameter(required = false)
	private ThreeDDisplayService threeDDisplayService;

	public ActiveThreeDDisplayPreProcessor() {
		super(ThreeDDisplay.class);
	}

	// -- SingleInputProcessor methods --

	@Override
	public ThreeDDisplay getValue() {
		if (threeDDisplayService == null) return null;
		return threeDDisplayService.getActiveThreeDDisplay();
	}

}

package sc.iview;

import org.scijava.module.process.PreprocessorPlugin;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.imagej.display.process.SingleInputPreprocessor;

/**
 * Fills single, unresolved module inputs with the active active
 * {@link SciView}. Hence, rather than a dialog prompting the user to - *
 * manually select an input, the active {@link SciView} is used automatically. -
 * *
 * <p>
 * - * In the case of more than one compatible parameter, the active - *
 * {@link SciView} is not used and instead the user must select. This behavior -
 * * is consistent with ImageJ v1.x. - *
 * </p>
 * 
 * @author Curtis Rueden
 * @author Mark Hiner hinerm at gmail.com
 */
@Plugin(type = PreprocessorPlugin.class)
public class ActiveSciViewPreprocessor extends SingleInputPreprocessor<SciView>
{

	@Parameter(required = false)
	private SciViewService sceneryService;

	public ActiveSciViewPreprocessor() {
		super(SciView.class);
	}

	// -- SingleInputProcessor methods --

	@Override
	public SciView getValue() {
		if (sceneryService == null) return null;
		return sceneryService.getOrCreateActiveSciView();
	}

}

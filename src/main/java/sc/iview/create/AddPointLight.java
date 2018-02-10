package sc.iview.create;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.command.Command;
import sc.iview.SciViewService;

@Plugin(type = Command.class, 
		menuPath = "SciView>Add>Point Light")
public class AddPointLight implements Command {

	@Parameter
	private SciViewService sceneryService;

	@Override
	public void run() {
		sceneryService.getActiveSciView().addPointLight();
	}

}

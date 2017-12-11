package sc.iview.viewing;

import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciView;
import sc.iview.SciViewService;

import org.scijava.command.Command;

@Plugin(type = Command.class, 
		menuPath = "SciView>View>Screenshot")
public class Screenshot implements Command {

	@Parameter
	private SciViewService sceneryService;

	@Parameter
	private LogService logService;
	
	@Override
	public void run() {
		sceneryService.getActiveSciView().takeScreenshot(logService);
	}

}


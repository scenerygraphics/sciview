package sc.iview.create;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.command.Command;
import org.scijava.display.DisplayService;
import sc.iview.SciView;

@Plugin(type = Command.class, 
		menuPath = "SciView>Add>Box")
public class AddBox  implements Command {

	@Parameter
	DisplayService displayService;
	
	@Parameter
    SciView sciView;
	
	//SceneryService sceneryService;
		
	@Override
	public void run() {
		sciView.addBox();
	}

}

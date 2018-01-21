package sc.iview;

import cleargl.GLVector;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, 
		menuPath = "SciView>Add>Line")
public class AddLine implements Command {

	@Parameter
	private DVec3 start;

	@Parameter
	private DVec3 stop;

	// Thickness

	@Parameter
	private SciViewService sceneryService;

	@Parameter
	private SciView sciView;
	
	@Override
	public void run() {
		sciView.addLine( start, stop );
	}

}

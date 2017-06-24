package sc.iview.viewing;

import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import graphics.scenery.Node;
import sc.iview.SciView;
import sc.iview.SciViewService;

@Plugin(type = Command.class, 
		menuPath = "Scenery>View>Rotate")
public class RotateView  implements Command {

	@Parameter
	private SciViewService sceneryService;

	@Override
	public void run() {
		Thread rotator = sceneryService.getActiveSceneryViewer().getAnimationThread();
		if( rotator != null && ( 
				rotator.getState() == Thread.State.RUNNABLE ||
				rotator.getState() == Thread.State.WAITING ) ) {
			rotator = null;
		}
		
		rotator = new Thread(){
		    public void run() {
		        while (true) {
		        	for( Node node : sceneryService.getActiveSceneryViewer().getSceneNodes() ) {
			        	
			            node.getRotation().rotateByAngleY(0.01f);
			            node.setNeedsUpdate(true);
			            
		        	}

		            try {
		                Thread.sleep(20);
		            } catch (InterruptedException e) {
		                e.printStackTrace();
		            }
		        }
		    }
		};        
		rotator.start();

		sceneryService.getActiveSceneryViewer().setAnimationThread( rotator );
	}

}


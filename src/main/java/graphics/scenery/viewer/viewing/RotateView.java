package graphics.scenery.viewer.viewing;

import graphics.scenery.viewer.SceneryService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

import graphics.scenery.Node;

@Plugin(type = Command.class, 
		menuPath = "Scenery>View>Rotate")
public class RotateView  implements Command {

	@Parameter
	private SceneryService sceneryService;

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


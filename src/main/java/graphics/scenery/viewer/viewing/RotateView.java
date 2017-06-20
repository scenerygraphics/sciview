package graphics.scenery.viewer.viewing;

import org.scijava.plugin.Plugin;

import graphics.scenery.viewer.SceneryViewer;

import org.scijava.command.Command;

import graphics.scenery.Node;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Rotate")
public class RotateView  implements Command {
		
	@Override
	public void run() {
		Thread rotator = SceneryViewer.getAnimationThread();
		if( rotator != null && ( 
				rotator.getState() == Thread.State.RUNNABLE ||
				rotator.getState() == Thread.State.WAITING ) ) {
			rotator = null;
		}
		
		rotator = new Thread(){
		    public void run() {
		        while (true) {
		        	for( Node node : SceneryViewer.getSceneNodes() ) {
			        	
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

		SceneryViewer.setAnimationThread( rotator );
	}

}


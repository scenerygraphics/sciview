package sc.fiji.threed.viewing;

import org.scijava.plugin.Plugin;

import sc.fiji.threed.ThreeDViewer;

import org.scijava.command.Command;

import scenery.Node;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Rotate")
public class RotateView  implements Command {
		
	@Override
	public void run() {
		Thread rotator = ThreeDViewer.getAnimationThread();
		if( rotator != null && ( 
				rotator.getState() == Thread.State.RUNNABLE ||
				rotator.getState() == Thread.State.WAITING ) ) {
			rotator = null;
		}
		
		rotator = new Thread(){
		    public void run() {
		        while (true) {
		        	for( Node node : ThreeDViewer.getSceneNodes() ) {
			        	
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
		
		ThreeDViewer.setAnimationThread( rotator );
	}

}


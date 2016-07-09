package sc.fiji.threed.viewing;

import org.scijava.plugin.Plugin;
import org.scijava.command.Command;

import sc.fiji.ThreeDViewer;
import scenery.Node;

@Plugin(type = Command.class, 
		menuPath = "ThreeDViewer>View>Rotate")
public class RotateView  implements Command {
		
	@Override
	public void run() {
		Thread rotator = new Thread(){
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
	}

}


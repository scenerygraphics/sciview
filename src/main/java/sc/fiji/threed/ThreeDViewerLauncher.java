package sc.fiji.threed;

import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Plugin;

@Plugin(type = Command.class, 
		menu = {@Menu(label = "ThreeDViewer"),
				@Menu(label = "Launch", weight = 3) })
public class ThreeDViewerLauncher implements Command {
	@Override
	public void run() {	
		String extraPath;
				
		/* Temporarily disable jinput
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("windows")){
        	//extraPath = ":" + Thread.currentThread().getContextClassLoader().getResource( "jinput-raw.dll" ).getPath().split("!")[0];
        	extraPath = "";
        	System.out.println( "jinput is disabled for Windows. You will not be able to use game controllers." );
        }else if (os.contains("linux") || os.contains("freebsd") || os.contains("sunos")){
        	extraPath = ":" + Thread.currentThread().getContextClassLoader().getResource( "libjinput-linux.so" ).getPath().split("!")[0];
        }else if (os.contains("mac os x")){
        	extraPath = ":" + Thread.currentThread().getContextClassLoader().getResource( "libjinput-osx.jnilib" ).getPath().split("!")[0];
        }else{
            throw new UnsupportedOperationException("The specified platform: "+os+" is not supported.");
        }

		System.setProperty("java.class.path", System.getProperty("java.class.path") + extraPath ); */ 
		
		ThreeDViewer.viewer = new ThreeDViewer( "ThreeDViewer", 800, 600 );
		
		ThreeDViewer.viewer.main();	
	}
}

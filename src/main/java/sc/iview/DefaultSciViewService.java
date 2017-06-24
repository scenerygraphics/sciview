package sc.iview;

import net.imagej.Data;
import net.imagej.Position;
import net.imagej.display.DataView;
import sc.iview.swing.SciViewDisplay;

import org.scijava.display.DisplayService;
import org.scijava.display.event.window.WinActivatedEvent;
import org.scijava.display.event.window.WinClosedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.menu.MenuService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginService;
import org.scijava.script.ScriptService;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.thread.ThreadService;

import java.util.LinkedList;
import java.util.List;


/**
 * Default service for rendering inside Scenery.
 *
 * @author Kyle Harrington (University of Idaho, Moscow)
 */
@Plugin(type = Service.class)
public class DefaultSciViewService extends AbstractService
    implements SciViewService
{

    /* Parameters */

	@Parameter
	private DisplayService displayService;
	
    @Parameter
    private EventService eventService;
    
    @Parameter
    private ThreadService threadService;

    /* Instance variables */

    private List<SciView> sceneryViewers =
            new LinkedList<>();

    /* Methods */

    public SciView getActiveSceneryViewer() {
        if ( sceneryViewers.size() > 0 )
            return sceneryViewers.get(0);
        return null;
    }

    public SciView getSceneryViewer(String name) {
        for( final SciView sceneryViewer : sceneryViewers ) {
            if( name.equalsIgnoreCase(sceneryViewer.getName())) {
                return sceneryViewer;
            }
        }
        return null;
    }

    public void createSceneryViewer() {
        SciView v = new SciView();

        // Maybe should use thread service instead
        Thread viewerThread = new Thread(){
            public void run() {
                v.main();
            }
        };
        viewerThread.start();

        sceneryViewers.add(v);
    }

    @Override
    public int numSceneryViewers() {
        return sceneryViewers.size();
    }

    /* Event Handlers */


	@Override
	public SciView getOrCreateActiveSciView() {
		SciViewDisplay d = displayService.getActiveDisplay(SciViewDisplay.class);
		System.out.println("GetOrCreateActiveSceneryViewer " + d + " " + displayService.getActiveDisplay());
		System.out.println(displayService.getDisplays().size());
		if( d != null ) {
			// We also have to check if the Viewer has been initialized
			//   and we're doing it the bad way. Replace if you want
			SciView sv = d.get(0);
			while( !sv.isInitialized() ) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			return sv;
		}
			
		if( !sceneryViewers.isEmpty() )
			System.out.println("SciViews exist but no SciViewDisplay found");
		// Make one
		SciView sv = new SciView();
		System.out.println("Starting viewer");
		
		threadService.run(new Runnable() {
			@Override
			public void run() {
				sv.main();
			}
		});
		while( !sv.isInitialized() ) {
			try {
				Thread.sleep(20);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println("Viewer started. " + sv);
		displayService.createDisplay(sv);
		System.out.println("Created display");
		//sceneryViewers.add(sv);		
		
		return sv;
		
		// Might need to change to return a SciViewDisplay instead, if downstream code needs it
	}

}

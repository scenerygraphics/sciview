package sc.iview.swing;

import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.display.event.DisplayDeletedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import net.imagej.display.DataView;
import net.imagej.display.DefaultImageDisplay;
import net.imagej.event.DataRestructuredEvent;
import net.imagej.event.DataUpdatedEvent;
import sc.iview.SciView;

/**
 * Created by kharrington on 6/24/17.
 */
@Plugin( type = Display.class )
public class SciViewDisplay extends AbstractDisplay<SciView> {

	@Parameter
	private ThreadService threadService;

	@Parameter(required = false)
	private EventService eventService;	
	
	public SciViewDisplay() {
		super(SciView.class);
	}
	
    public SciViewDisplay(Class<SciView> type) {
        super(type);
    }
    
    @EventHandler
	protected void onEvent(final DataRestructuredEvent event) {
		
	}

	// FIXME - displays should not listen for Data events. Views should listen for
	// data events, adjust themselves, and generate view events. The display
	// classes should listen for view events and refresh themselves as necessary.

	@EventHandler
	protected void onEvent(final DataUpdatedEvent event) {
		System.out.println("DIsplay updated");
	}

	@EventHandler
	protected void onEvent(final DisplayDeletedEvent event) {
		
	}
}

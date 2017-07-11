package sc.iview.swing;

import org.scijava.display.Display;
import org.scijava.display.event.DisplayActivatedEvent;
import org.scijava.display.event.DisplayDeletedEvent;
import org.scijava.display.event.DisplayUpdatedEvent;
import org.scijava.display.event.DisplayUpdatedEvent.DisplayUpdateLevel;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.viewer.AbstractDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;

import net.imagej.event.DataRestructuredEvent;
import net.imagej.event.DataUpdatedEvent;
import sc.iview.SciView;

/**
 * Created by kharrington on 6/24/17.
 */
@Plugin( type = DisplayViewer.class )
public class SciViewDisplayViewer extends AbstractDisplayViewer<SciView>
{

	@Parameter
	private EventService eventService;
	
    @Override
    public boolean isCompatible(UserInterface ui) {
        return ui instanceof SwingUI;
    }

    @Override
    public boolean canView(Display<?> d) {
        return d instanceof SciViewDisplay;
    }
    
    //@Override
    public DisplayWindow createWindow( Display<?> d ) {
    	Object data = d.get(0);
    	if( !( data instanceof SciView ) )
    		throw new IllegalArgumentException("Must be SciView");
    	return new SciViewDisplayWindow((SciView)data);
    }
    
    @EventHandler
  	protected void onEvent(final DataRestructuredEvent event) {
  		
  	}

  	// FIXME - displays should not listen for Data events. Views should listen for
  	// data events, adjust themselves, and generate view events. The display
  	// classes should listen for view events and refresh themselves as necessary.

  	@EventHandler
  	protected void onEvent(final DataUpdatedEvent event) {
  		System.out.println("Display updated");
  	}

  	@EventHandler
  	protected void onEvent(final DisplayDeletedEvent event) {
  		
  	}
  	
	/** Synchronizes the user interface appearance with the display model. */
	public void onDisplayUpdatedEvent(final DisplayUpdatedEvent e) {
		
	}
	
	public void onDisplayActivatedEvent(final DisplayActivatedEvent e) {
		// do nothing because no panel
	}
}

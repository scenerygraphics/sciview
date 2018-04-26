/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview;

import java.util.LinkedList;
import java.util.List;

import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.event.EventService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.service.AbstractService;
import org.scijava.service.Service;
import org.scijava.thread.ThreadService;

import sc.iview.swing.SciViewDisplay;


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

    @Parameter
	private LogService logService;

    /* Instance variables */

    private List<SciView> sceneryViewers =
            new LinkedList<>();

    /* Methods */

    public SciView getActiveSciView() {
		SciViewDisplay d = displayService.getActiveDisplay(SciViewDisplay.class);
		if( d != null ) {
			// We also have to check if the Viewer has been initialized
			//   and we're doing it the bad way by polling. Replace if you want
			SciView sv = d.get(0);
			while( !sv.isInitialized() ) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					logService.trace(e);
				}
			}
			return sv;
		} else {
			logService.error("No SciJava display available. Use getOrCreateActiveSciView() to automatically create a display if one does not exist.");
			return null;
		}
    }

    public SciView getSciView(String name) {
        for( final SciView sceneryViewer : sceneryViewers ) {
            if( name.equalsIgnoreCase(sceneryViewer.getName())) {
                return sceneryViewer;
            }
        }
		logService.error("No SciJava display available. Use getOrCreateActiveSciView() to automatically create a display if one does not exist.");
        return null;
    }

    public void createSciView() {
        SciView v = new SciView(getContext());

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
    public int numSciView() {
        return sceneryViewers.size();
    }

    /* Event Handlers */


	@Override
	public SciView getOrCreateActiveSciView() {
		SciViewDisplay d = displayService.getActiveDisplay(SciViewDisplay.class);
		if( d != null ) {
			// We also have to check if the Viewer has been initialized
			//   and we're doing it the bad way by polling. Replace if you want
			SciView sv = d.get(0);
			while( !sv.isInitialized() ) {
				try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					logService.trace(e);
				}
			}
			return sv;
		}
			
		// Make one
		SciView sv = new SciView(getContext());
		
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
				logService.trace(e);
			}
		}

		Display<?> display = displayService.createDisplay(sv);
		displayService.setActiveDisplay(display);

		//sceneryViewers.add(sv);		
		
		return sv;
		
		// Might need to change to return a SciViewDisplay instead, if downstream code needs it
	}

}

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
package sc.iview.display;

import net.imagej.event.DataRestructuredEvent;
import net.imagej.event.DataUpdatedEvent;

import org.scijava.display.AbstractDisplay;
import org.scijava.display.Display;
import org.scijava.display.event.DisplayDeletedEvent;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.thread.ThreadService;

import sc.iview.SciView;

/**
 * Created by kharrington on 6/24/17.
 */
@Plugin(type = Display.class)
public class SciViewDisplay extends AbstractDisplay<SciView> {

    @Parameter
    private ThreadService threadService;

    @Parameter(required = false)
    private EventService eventService;

    @Parameter
    private LogService logService;

    public SciViewDisplay() {
        super( SciView.class );
    }

    public SciViewDisplay( Class<SciView> type ) {
        super( type );
    }

    @EventHandler
    protected void onEvent( final DataRestructuredEvent event ) {

    }

    // FIXME - displays should not listen for Data events. Views should listen for
    // data events, adjust themselves, and generate view events. The display
    // classes should listen for view events and refresh themselves as necessary.

    @EventHandler
    protected void onEvent( final DataUpdatedEvent event ) {
        // NB: Placeholder in case it's needed in future.
    }

    @EventHandler
    protected void onEvent( final DisplayDeletedEvent event ) {
        // NB: Placeholder in case it's needed in future.
    }
}

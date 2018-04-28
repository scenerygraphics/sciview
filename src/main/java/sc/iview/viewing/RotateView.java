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
package sc.iview.viewing;

import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import sc.iview.SciViewService;

import graphics.scenery.Node;

@Plugin(type = Command.class, menuPath = "SciView>View>Rotate")
public class RotateView implements Command {

    @Parameter
    private SciViewService sceneryService;

    @Parameter
    private LogService logService;

    @Override
    public void run() {
        Thread rotator = sceneryService.getActiveSciView().getAnimationThread();
        if( rotator != null && ( rotator.getState() == Thread.State.RUNNABLE ||
                                 rotator.getState() == Thread.State.WAITING ) ) {
            rotator = null;
        }

        rotator = new Thread() {
            public void run() {
                while( true ) {
                    for( Node node : sceneryService.getActiveSciView().getSceneNodes() ) {

                        node.getRotation().rotateByAngleY( 0.01f );
                        node.setNeedsUpdate( true );

                    }

                    try {
                        Thread.sleep( 20 );
                    } catch( InterruptedException e ) {
                        logService.trace( e );
                    }
                }
            }
        };
        rotator.start();

        sceneryService.getActiveSciView().setAnimationThread( rotator );
    }

}

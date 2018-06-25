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
package sc.iview.commands.view;

import cleargl.GLVector;
import com.jogamp.opengl.math.Quaternion;
import graphics.scenery.Mesh;
import graphics.scenery.Node;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.VIEW;
import static sc.iview.commands.MenuWeights.VIEW_CENTER_ON_ACTIVE_NODE;

@Plugin(type = Command.class, menuRoot = "SciView", //
menu = {@Menu(label = "View", weight = VIEW), //
        @Menu(label = "Center On Active Node", weight = VIEW_CENTER_ON_ACTIVE_NODE)})
public class CenterOnActiveNode implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    @Override
    public void run() {
        if( sciView.getActiveNode() instanceof Mesh ) {
            Node currentNode = sciView.getActiveNode();

            Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();

            sciView.getCamera().setTarget( currentNode.getPosition() );
            sciView.getCamera().setTargeted( true );

            // Set forward direction to point from camera at active node
            sciView.getCamera().setForward( bb.getBoundingSphere().getOrigin().minus( sciView.getCamera().getPosition() ).normalize().times( -1 ) );

            float distance = (float) (bb.getBoundingSphere().getRadius() / Math.tan( sciView.getCamera().getFov() / 360 * java.lang.Math.PI ));

            // Solve for the proper rotation
            Quaternion rotation = new Quaternion().setLookAt( sciView.getCamera().getForward().toFloatArray(),
                                                        new GLVector(0,1,0).toFloatArray(),
                                                        new GLVector(1,0,0).toFloatArray(),
                                                        new GLVector( 0,1,0).toFloatArray(),
                                                        new GLVector( 0, 0, 1).toFloatArray() );

            sciView.getCamera().setRotation( rotation.normalize() );
            sciView.getCamera().setPosition( bb.getBoundingSphere().getOrigin().plus( sciView.getCamera().getForward().times( distance * -1 ) ) );

            sciView.getCamera().setDirty(true);
            sciView.getCamera().setNeedsUpdate(true);

        }

    }

}

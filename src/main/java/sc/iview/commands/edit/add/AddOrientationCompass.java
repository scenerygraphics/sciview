/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
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
package sc.iview.commands.edit.add;

import graphics.scenery.*;
import graphics.scenery.attribute.material.Material;
import graphics.scenery.primitives.Cylinder;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.util.HashMap;

import static sc.iview.commands.MenuWeights.*;

/**
 * Command to orientation compass (R,G,B cylinders oriented along X,Y,Z axes, respectively) to the scene
 *
 * @author Vladimir Ulman
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Edit", weight = EDIT), //
                 @Menu(label = "Add", weight = EDIT_ADD), //
                 @Menu(label = "Compass", weight = EDIT_ADD_COMPASS) })
public class AddOrientationCompass implements Command {

    @Parameter
    private SciView sciView;

    @Parameter
    private float axisLength = 0.1f;

    @Parameter
    private float AXESBARRADIUS = 0.001f;

    @Parameter(required = false)
    private Vector3f xColor = new Vector3f(1f,0f,0f);

    @Parameter(required = false)
    private Vector3f yColor = new Vector3f(0f,1f,0f);

    @Parameter(required = false)
    private Vector3f zColor = new Vector3f(0f,0f,1f);

    private Node makeAxis( float axisLength, float angleX, float angleY, float angleZ, Vector3f color ) {
        Cylinder axisNode = new Cylinder(AXESBARRADIUS, axisLength,4);
        axisNode.setName("compass axis: X");
        axisNode.spatial().setRotation( new Quaternionf().rotateXYZ( angleX, angleY, angleZ ) );
        axisNode.ifMaterial( material -> {
            material.getDiffuse().set(color);
            material.setDepthTest(Material.DepthTest.Always);
            material.getBlending().setTransparent(true);
            return null;
        });

        Icosphere axisCap = new Icosphere(AXESBARRADIUS, 2, false);
        axisCap.ifSpatial(spatial -> {
            spatial.setPosition(new Vector3f(0, axisLength, 0));
            return null;
        });
        axisCap.material().getDiffuse().set(color);
        axisCap.material().setDepthTest(Material.DepthTest.Always);
        axisCap.material().getBlending().setTransparent(true);

        axisNode.addChild(axisCap);
        return axisNode;
    }

    @Override
    public void run() {
        final Node root = new Mesh("Scene orientation compass");

        //NB: RGB colors ~ XYZ axes
        //x axis:
        Node axisNode = makeAxis( axisLength, 0,0,(float)(-0.5*Math.PI), xColor );
        axisNode.setName("compass axis: X");
        root.addChild( axisNode );

        //y axis:
        axisNode = makeAxis( axisLength, 0,0, 0, yColor );

        axisNode.setName("compass axis: Y");
        root.addChild( axisNode );

        //z axis:
        axisNode = makeAxis( axisLength, (float)(0.5*Math.PI),0,0, zColor );
        axisNode.setName("compass axis: Z");
        root.addChild( axisNode );

        root.setName("Compass");

        sciView.getCamera().addChild(root);
        sciView.publishNode(root);

        root.getUpdate().add(() -> {
            final Camera cam = sciView.getCamera();
            root.setPosition(cam.viewportToView(new Vector2f(-0.9f, 0.7f)));
            root.setRotation(new Quaternionf(sciView.getCamera().getRotation()).conjugate().normalize());
            return null;
        });
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(AddOrientationCompass.class, true, argmap);
    }
}

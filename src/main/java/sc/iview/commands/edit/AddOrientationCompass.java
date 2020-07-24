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
package sc.iview.commands.edit;

import graphics.scenery.Cylinder;
import graphics.scenery.Node;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.util.HashMap;

import static sc.iview.commands.MenuWeights.EDIT;
import static sc.iview.commands.MenuWeights.EDIT_ADD_COMPASS;

/**
 * Command to orientation compass (R,G,B cylinders oriented along X,Y,Z axes, respectively) to the scene
 *
 * @author Vladimir Ulman
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Edit", weight = EDIT), //
                 @Menu(label = "Add Compass", weight = EDIT_ADD_COMPASS) })
public class AddOrientationCompass implements Command {

    @Parameter
    private SciView sciView;

    @Parameter
    private float xAxisLength = 30.0f;
    @Parameter
    private float yAxisLength = 30.0f;
    @Parameter
    private float zAxisLength = 30.0f;

    static final float AXESBARRADIUS = 1.0f;

    @Override
    public void run() {
        final Node root = new Node("Scene orientation compass");

        //NB: RGB colors ~ XYZ axes
        //x axis:
        Cylinder axisNode = new Cylinder(AXESBARRADIUS,xAxisLength,4);
        axisNode.setName("compass axis: X");
        axisNode.getMaterial().getDiffuse().set(1f,0f,0f);
        axisNode.setRotation( new Quaternionf().rotateXYZ(0,0,(float)(-0.5*Math.PI)) );
        root.addChild( axisNode );

        //y axis:
        axisNode = new Cylinder(AXESBARRADIUS,yAxisLength,4);
        axisNode.setName("compass axis: Y");
        axisNode.getMaterial().getDiffuse().set(0f,1f,0f);
        root.addChild( axisNode );

        //z axis:
        axisNode = new Cylinder(AXESBARRADIUS,zAxisLength,4);
        axisNode.setName("compass axis: Z");
        axisNode.getMaterial().getDiffuse().set(0f,0f,1f);
        axisNode.setRotation( new Quaternionf().rotateXYZ((float)(0.5*Math.PI),0,0) );
        root.addChild( axisNode );

        sciView.addNode( root );

        sciView.getCamera().addChild(root);
        //root.setPosition( new Vector3f(-25, 0, -100));

        root.getUpdate().add(() -> {
            root.setWantsComposeModel(false);
            root.getModel().identity();

            root.getModel().translate(
                    sciView.getCamera().getPosition().add(
                            root.getPosition() ) );
            root.getModel().mul( new Quaternionf().get(new Matrix4f()) );

//                logger.info("Updating pose of $controller, ${node.model}")

            root.setNeedsUpdate(false);
            root.setNeedsUpdateWorld(false);
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

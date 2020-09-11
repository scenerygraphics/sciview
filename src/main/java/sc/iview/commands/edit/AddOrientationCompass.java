/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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

import graphics.scenery.*;
import org.joml.Vector3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.lang.Math;

import static sc.iview.commands.MenuWeights.EDIT;
import static sc.iview.commands.MenuWeights.EDIT_ADD_COMPASS;

/**
 * Command to orientation compass (R,G,B cylinders oriented along X,Y,Z axes, respectively) to the scene
 *
 * @author Vladimir Ulman
 * @author Kyle Harrington
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Edit", weight = EDIT), //
                 @Menu(label = "Add Compass", weight = EDIT_ADD_COMPASS) })
public class AddOrientationCompass implements Command {

    @Parameter
    private SciView sciView;

    @Parameter(label = "Length of each bar:", stepSize = "0.1", min = "0")
    private float axisLength = 0.1f;

    @Parameter(label = "Thickness of each bar:", stepSize = "0.001", min = "0")
    private float axisBarRadius = 0.001f;

    //@Parameter -- waits until scijava can offer color-picker dialog
    private Vector3f xColor = new Vector3f(1f,0f,0f);
    //NB: RGB colors ~ XYZ axes

    //@Parameter
    private Vector3f yColor = new Vector3f(0f,1f,0f);

    //@Parameter
    private Vector3f zColor = new Vector3f(0f,0f,1f);

    @Parameter(label = "Show in overlay in top-left corner:")
    boolean attachToCam = true;

    @Parameter(label = "Show as controllable node in the scene:")
    boolean showInTheScene = false;


    private Node makeAxis( float axisLength, float angleX, float angleY, float angleZ, Vector3f color ) {
        Cylinder axisNode = new Cylinder(axisBarRadius, axisLength,4);
        axisNode.setName("compass bar");
        axisNode.setRotation( new Quaternionf().rotateXYZ( angleX, angleY, angleZ ) );
        axisNode.getMaterial().getDiffuse().set(color);
        axisNode.getMaterial().setDepthTest(Material.DepthTest.Always);
        axisNode.getMaterial().getBlending().setTransparent(true);

        Icosphere axisCap = new Icosphere(axisBarRadius, 2);
        axisCap.setName("cap of the bar");
        axisCap.setPosition(new Vector3f(0, axisLength, 0));
        axisCap.getMaterial().getDiffuse().set(color);
        axisCap.getMaterial().setDepthTest(Material.DepthTest.Always);
        axisCap.getMaterial().getBlending().setTransparent(true);

        axisNode.addChild(axisCap);
        return axisNode;
    }

    private Node createCompass() {
        final Node root = new Node("Scene orientation compass");

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
        return root;
    }

    private Node createBox() {
        final Node root = new Node("Scene representing box");

        // X - red edges
        final float halfPI = (float)(0.5*Math.PI);
        final float edgeLen = 0.05f;
        final float edgeRadius = 0.0008f;
        for (int i = 0; i < 4; ++i)
        {
            final Node n = new Cylinder(edgeRadius,edgeLen,4);
            n.getPosition().set( -0.5f * edgeLen );
            n.getPosition().add( new Vector3f(0, i%2 *edgeLen, i < 2 ? 0 : edgeLen) );
            n.setRotation( new Quaternionf().rotateXYZ(0,0,-halfPI).normalize() );
            n.setName("box edge: X");
            n.getMaterial().getDiffuse().set( new Vector3f(1,0,0) );
            n.getMaterial().setDepthTest(Material.DepthTest.Always);
            //n.getMaterial().getBlending().setTransparent(true);
            root.addChild( n );
        }

        // Y - green edges
        for (int i = 0; i < 4; ++i)
        {
            final Node n = new Cylinder(edgeRadius,edgeLen,4);
            n.getPosition().set( -0.5f * edgeLen );
            n.getPosition().add( new Vector3f(i%2 *edgeLen, 0, i < 2 ? 0 : edgeLen) );
            n.setName("box edge: Y");
            n.getMaterial().getDiffuse().set( new Vector3f(0,1,0) );
            n.getMaterial().setDepthTest(Material.DepthTest.Always);
            //n.getMaterial().getBlending().setTransparent(true);
            root.addChild( n );
        }

        // Z - blue edges
        for (int i = 0; i < 4; ++i)
        {
            final Node n = new Cylinder(edgeRadius,edgeLen,4);
            n.getPosition().set( -0.5f * edgeLen );
            n.getPosition().add( new Vector3f(i%2 *edgeLen, i < 2 ? 0 : edgeLen, 0) );
            n.setRotation( new Quaternionf().rotateXYZ(halfPI,0,0).normalize() );
            n.setName("box edge: Z");
            n.getMaterial().getDiffuse().set( new Vector3f(0,0,1) );
            n.getMaterial().setDepthTest(Material.DepthTest.Always);
            //n.getMaterial().getBlending().setTransparent(true);
            root.addChild( n );
        }

        return root;
    }

    @Override
    public void run() {
        if (showInTheScene) sciView.addNode( createCompass() );
        if (attachToCam) {
            final Node compass = createBox();
            sciView.getCamera().addChild( compass );
            compass.setWantsComposeModel(false);

            final float xStretch = sciView.getCamera().getProjection().m00();
            final float yStretch = sciView.getCamera().getProjection().m11();
            //
            final Matrix4f positionDaBox = new Matrix4f();
            positionDaBox.set(1,0,0,0,  //1st col
                              0,1,0,0,  //2nd col
                              0,0,1,0,
                          -0.9f/xStretch,+0.85f/yStretch,-1.0f,1);

            compass.getUpdate().add(() -> {
                sciView.getCamera().getRotation().get( compass.getModel() );
                compass.getModel().mulLocal( positionDaBox );
                return null;
            });
        }
    }
}

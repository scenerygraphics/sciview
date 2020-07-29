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
package sc.iview.process;

import graphics.scenery.Material;
import graphics.scenery.Node;
import graphics.scenery.Sphere;
import org.scijava.ui.behaviour.Behaviour;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.ScrollBehaviour;
import org.scijava.util.ColorRGB;
import sc.iview.SciView;
import sc.iview.Utils;
import sc.iview.vector.JOMLVector3;
import sc.iview.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * ControlPoints developed for interactive mesh creation
 *
 * @author Kyle Harrington
 */
public class ControlPoints {
    static public float DEFAULT_RADIUS = 0.5f;
    static public int DEFAULT_SEGMENTS = 18;
    static public ColorRGB DEFAULT_COLOR = ColorRGB.fromHSVColor(0.5,0.5,0.25);
    static public ColorRGB TARGET_COLOR = ColorRGB.fromHSVColor(0.5,0.75,0.75);

    protected List<Node> nodes;
    private Node targetPoint;
    private float controlPointDistance;

    public ControlPoints() {
        nodes = new ArrayList<>();
    }

    public void clearPoints() {
        nodes.clear();
    }

    public List<Vector3> getVertices() {
        List<Vector3> points = new ArrayList<>();
        for( int k = 0; k < nodes.size(); k++ ) {
            points.add( new JOMLVector3( nodes.get(k).getPosition() ) );
        }
        return points;
    }

    public void setPoints(Vector3[] newPoints) {
        nodes.clear();
        nodes = new ArrayList<>();
        for( int k = 0; k < newPoints.length; k++ ) {
            Sphere cp = new Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS);
            cp.setPosition(JOMLVector3.convert(newPoints[k]));
            nodes.add(cp);
        }
    }

    public void addPoint(Node controlPoint) {
        nodes.add(controlPoint);
    }

    public List<Node> getNodes() {
        return nodes;
    }

    public void initializeSciView(SciView sciView, float controlPointDistance) {
        // This is where the command should change the current inputs setup
        sciView.stashControls();

        sciView.getSceneryInputHandler().addBehaviour( "place_control_point",
                placeControlPointBehaviour(sciView) );
        sciView.getSceneryInputHandler().addKeyBinding( "place_control_point", "double-click button1" );

        // Setup the scrolling behavior to adjust the control point distance
        sciView.getSceneryInputHandler().addBehaviour( "change_control_point_distance",
                distanceControlPointBehaviour(sciView) );
        sciView.getSceneryInputHandler().addKeyBinding( "change_control_point_distance", "scroll" );

        // Create target point
        targetPoint = new Sphere(ControlPoints.DEFAULT_RADIUS, ControlPoints.DEFAULT_SEGMENTS);
        Material mat = new Material();
        mat.setAmbient(Utils.convertToVector3f(ControlPoints.TARGET_COLOR));
        mat.setDiffuse(Utils.convertToVector3f(ControlPoints.TARGET_COLOR));
        targetPoint.setMaterial(mat);

        targetPoint.setPosition( sciView.getCamera().getPosition().add(sciView.getCamera().getForward().mul(controlPointDistance) ) );

        sciView.addNode(targetPoint,false);
        //sciView.getCamera().addChild(targetPoint);

        targetPoint.getUpdate().add(() -> {
            //targetPoint.getRotation().set(sciView.getCamera().getRotation().conjugate().rotateByAngleY((float) Math.PI));
            // Set rotation before setting position
            targetPoint.setPosition( sciView.getCamera().getPosition().add(sciView.getCamera().getForward().mul(controlPointDistance) ) );
            return null;
        });
    }

    private Behaviour placeControlPointBehaviour(SciView sciView) {
        ClickBehaviour b = new ClickBehaviour() {
            @Override
            public void click(int x, int y) {
                placeControlPoint(sciView);
            }
        };
        return b;
    }

    private Behaviour distanceControlPointBehaviour(SciView sciView) {
        ScrollBehaviour b = new ScrollBehaviour() {
            @Override
            public void scroll(double wheelRotation, boolean isHorizontal, int x, int y) {
                controlPointDistance += wheelRotation;
                targetPoint.setPosition( sciView.getCamera().getPosition().add(sciView.getCamera().getForward().mul(controlPointDistance) ) );
            }
        };
        return b;
    }

    private void placeControlPoint(SciView sciView) {

        Sphere controlPoint = new Sphere(ControlPoints.DEFAULT_RADIUS, ControlPoints.DEFAULT_SEGMENTS);
        Material mat = new Material();
        mat.setAmbient(Utils.convertToVector3f(ControlPoints.DEFAULT_COLOR));
        mat.setDiffuse(Utils.convertToVector3f(ControlPoints.DEFAULT_COLOR));
        controlPoint.setMaterial(mat);

        //controlPoint.setPosition( sciView.getCamera().getTransformation().mult(targetPoint.getPosition().xyzw()) );
        controlPoint.setPosition( targetPoint.getPosition() );

        addPoint( controlPoint );
        sciView.addNode( controlPoint, false );
    }

    public void cleanup(SciView sciView) {
        sciView.restoreControls();
        // Remove all control points
        for( Node n : getNodes() ) {
            sciView.deleteNode(n,false);
        }
        sciView.deleteNode(targetPoint,false);
    }
}

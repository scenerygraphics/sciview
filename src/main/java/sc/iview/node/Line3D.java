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
package sc.iview.node;

import graphics.scenery.*;
import org.joml.Vector3f;
import org.scijava.util.ColorRGB;
import org.scijava.util.Colors;
import sc.iview.Utils;
import sc.iview.vector.JOMLVector3;
import sc.iview.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

/**
 * A 3D line composed of scenery Nodes
 *
 * @author Kyle Harrington
 */
public class Line3D extends Node {
    private List<Node> edges;
    private List<Node> joints;
    private double edgeWidth = 0.05;
    private ColorRGB defaultColor = Colors.LIGHTSALMON;
    private boolean sphereJoints = true;

    public Line3D() {
        edges = new ArrayList<>();
    }

    public Line3D(List<Vector3> points, ColorRGB colorRGB, double edgeWidth) {
        this.defaultColor = colorRGB;
        this.edgeWidth = edgeWidth;

        edges = new ArrayList<>();
        if( sphereJoints ) joints= new ArrayList<>();
        for( int k = 0; k < points.size(); k++) {
            if( k > 0 ) {
                Node edge =
                        Cylinder.betweenPoints(
                                JOMLVector3.convert(points.get(k - 1)),
                                JOMLVector3.convert(points.get(k)),
                                (float) edgeWidth,
                                1f,
                                15);
                addLine(edge);
            }

            if( sphereJoints ) {
                Node joint = new Sphere((float) edgeWidth,15);
                joint.setPosition(JOMLVector3.convert(points.get(k)));
                joints.add(joint);
                addChild(joint);
            }
        }
    }

    public Line3D(List<Vector3> points, List<ColorRGB> colors, double edgeWidth) {
        this.edgeWidth = edgeWidth;

        edges = new ArrayList<>();
        if( sphereJoints ) joints= new ArrayList<>();
        for( int k = 0; k < points.size(); k++) {
            Vector3f c = Utils.convertToVector3f(colors.get(k));
            Material mat = new Material();
            mat.setDiffuse(c);
            mat.setAmbient(c);
            mat.setSpecular(c);

            if( k > 0 ) {
                Node edge =
                        Cylinder.betweenPoints(
                                JOMLVector3.convert(points.get(k - 1)),
                                JOMLVector3.convert(points.get(k)),
                                (float) edgeWidth,
                                1f,
                                15);
                edge.setMaterial(mat);
                addLine(edge);
            }

            if( sphereJoints ) {
                Node joint = new Sphere((float) edgeWidth,15);
                joint.setMaterial(mat);
                joint.setPosition(JOMLVector3.convert(points.get(k)));
                joints.add(joint);
                addChild(joint);
            }
        }
    }

    public void setColors( List<ColorRGB> colors ) {
        for( int k = 0; k < joints.size(); k++ ) {
            Vector3f c = Utils.convertToVector3f(colors.get(k));
            Material mat = new Material();
            mat.setDiffuse(c);
            mat.setAmbient(c);
            mat.setSpecular(c);
            joints.get(k).setMaterial(mat);
            joints.get(k).setNeedsUpdate(true);
            joints.get(k).setDirty(true);
            edges.get(k).setMaterial(mat);
            edges.get(k).setNeedsUpdate(true);
            edges.get(k).setDirty(true);
        }
    }

    public void addLine( Node l ) {
        edges.add(l);
        addChild(l);
        generateBoundingBox();
    }

    public List<Node> getEdges() {
        return edges;
    }

    /**
     * Generates an [OrientedBoundingBox] for this [Node]. This will take
     * geometry information into consideration if this Node implements [HasGeometry].
     * In case a bounding box cannot be determined, the function will return null.
     */
    public OrientedBoundingBox generateBoundingBox() {
        OrientedBoundingBox bb = new OrientedBoundingBox(this, 0.0f, 0.0f, 0.0f,
                0.0f, 0.0f, 0.0f);
        for( Node n : getChildren() ) {
            OrientedBoundingBox cBB = n.generateBoundingBox();
            if( cBB != null )
                bb = bb.expand(bb, cBB);
        }
        return bb;
    }
}

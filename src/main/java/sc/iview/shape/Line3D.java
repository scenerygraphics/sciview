package sc.iview.shape;

import cleargl.GLVector;
import graphics.scenery.Cylinder;
import graphics.scenery.Material;
import graphics.scenery.Node;
import graphics.scenery.Sphere;
import org.scijava.util.ColorRGB;
import org.scijava.util.Colors;
import sc.iview.Utils;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

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
                Node edge = Cylinder.betweenPoints(ClearGLVector3.convert(points.get(k - 1)), ClearGLVector3.convert(points.get(k)), (float) edgeWidth, 1f, 15);
                addLine(edge);
            }

            if( sphereJoints ) {
                Node joint = new Sphere((float) edgeWidth,15);
                joint.setPosition(ClearGLVector3.convert(points.get(k)));
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
            GLVector c = Utils.convertToGLVector(colors.get(k));
            Material mat = new Material();
            mat.setDiffuse(c);
            mat.setAmbient(c);
            mat.setSpecular(c);

            if( k > 0 ) {
                Node edge = Cylinder.betweenPoints(ClearGLVector3.convert(points.get(k - 1)), ClearGLVector3.convert(points.get(k)), (float) edgeWidth, 1f, 15);
                edge.setMaterial(mat);
                addLine(edge);
            }

            if( sphereJoints ) {
                Node joint = new Sphere((float) edgeWidth,15);
                joint.setMaterial(mat);
                joint.setPosition(ClearGLVector3.convert(points.get(k)));
                joints.add(joint);
                addChild(joint);
            }
        }
    }

    public void setColors( List<ColorRGB> colors ) {
        for( int k = 0; k < joints.size(); k++ ) {
            GLVector c = Utils.convertToGLVector(colors.get(k));
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
}

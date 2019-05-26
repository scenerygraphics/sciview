package sc.iview.shape;

import graphics.scenery.Cylinder;
import graphics.scenery.Node;
import graphics.scenery.Sphere;
import org.scijava.util.ColorRGB;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

public class Line3D extends Node {
    protected List<Node> edges;
    protected List<Node> joints;
    protected double edgeWidth;
    protected ColorRGB defaultColor;
    protected boolean sphereJoints = true;

    public Line3D() {
        edges = new ArrayList<>();
    }

    public Line3D(List<Vector3> points, ColorRGB colorRGB, double edgeWidth) {
        this.defaultColor = colorRGB;
        this.edgeWidth = edgeWidth;

        edges = new ArrayList<>();
        if( sphereJoints ) joints= new ArrayList<>();
        for( int k = 1; k < points.size(); k++) {
            Node edge = Cylinder.betweenPoints(ClearGLVector3.convert(points.get(k - 1)), ClearGLVector3.convert(points.get(k)), (float) edgeWidth, 1f, 15);
            addLine(edge);
            if( sphereJoints && k < points.size()-1 ) {
                Node joint = new Sphere((float) edgeWidth,15);
                joint.setPosition(ClearGLVector3.convert(points.get(k)));
                joints.add(joint);
                addChild(joint);
            }
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

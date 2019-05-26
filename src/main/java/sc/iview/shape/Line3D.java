package sc.iview.shape;

import graphics.scenery.Cylinder;
import graphics.scenery.Node;
import org.scijava.util.ColorRGB;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

public class Line3D extends Node {
    protected List<Node> edges;
    protected double edgeWidth;
    protected ColorRGB defaultColor;

    public Line3D() {
        edges = new ArrayList<>();
    }

    public Line3D(List<Vector3> points, ColorRGB colorRGB, double edgeWidth) {
        this.defaultColor = colorRGB;
        this.edgeWidth = edgeWidth;

        edges = new ArrayList<>();
        for( int k = 1; k < points.size(); k++) {
            Node edge = Cylinder.betweenPoints(ClearGLVector3.convert(points.get(k - 1)), ClearGLVector3.convert(points.get(k)), (float) (edgeWidth * 2), 1f, 15);
            addLine(edge);
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

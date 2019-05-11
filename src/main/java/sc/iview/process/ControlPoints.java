package sc.iview.process;

import graphics.scenery.Node;
import graphics.scenery.Sphere;
import org.scijava.util.ColorRGB;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import java.util.ArrayList;
import java.util.List;

public class ControlPoints {
    static public float DEFAULT_RADIUS = 0.5f;
    static public int DEFAULT_SEGMENTS = 18;
    static public ColorRGB DEFAULT_COLOR = ColorRGB.fromHSVColor(0.5,0.5,0.25);

    protected List<Node> nodes;

    public ControlPoints() {
        nodes = new ArrayList<>();
    }

    public void clearPoints() {
        nodes.clear();
    }

    public List<Vector3> getVertices() {
        List<Vector3> points = new ArrayList<>();
        for( int k = 0; k < nodes.size(); k++ ) {
            points.add( new ClearGLVector3( nodes.get(k).getPosition() ) );
        }
        return points;
    }

    public void setPoints(Vector3[] newPoints) {
        nodes.clear();
        nodes = new ArrayList<>();
        for( int k = 0; k < newPoints.length; k++ ) {
            Sphere cp = new Sphere(DEFAULT_RADIUS, DEFAULT_SEGMENTS);
            cp.setPosition(ClearGLVector3.convert(newPoints[k]));
            nodes.add(cp);
        }
    }

    public void addPoint(Node controlPoint) {
        nodes.add(controlPoint);
    }

    public List<Node> getNodes() {
        return nodes;
    }
}

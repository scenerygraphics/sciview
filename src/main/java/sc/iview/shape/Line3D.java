package sc.iview.shape;

import graphics.scenery.Node;

import java.util.ArrayList;
import java.util.List;

public class Line3D extends Node {
    protected List<Node> lines;

    public Line3D() {
        lines = new ArrayList<>();
    }

    public void addLine( Node l ) {
        lines.add(l);
        addChild(l);
    }

    public List<Node> getLines() {
        return lines;
    }
}

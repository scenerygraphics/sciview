package sc.iview.ui;

import graphics.scenery.Node;

import javax.swing.*;

public class ContextPopUp extends JPopupMenu {

    public ContextPopUp(Node n) {
        add( new JMenuItem("Name: " + n.getName()) );
        add( new JMenuItem("Type: " + n.getNodeType()) );
    }

}

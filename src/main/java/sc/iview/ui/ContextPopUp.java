package sc.iview.ui;

import graphics.scenery.Node;

import javax.swing.*;

public class ContextPopUp extends JPopupMenu {

    JMenuItem anItem;
    public ContextPopUp(Node n) {
        anItem = new JMenuItem("I'm a " + n.getNodeType());
        add(anItem);
    }

}

package sc.iview.ui;

import graphics.scenery.Node;
import graphics.scenery.Scene;
import sc.iview.SciView;

import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

public class ContextPopUpNodeChooser extends JPopupMenu {

    public ContextPopUpNodeChooser(final SciView sv) {
        for (Scene.RaycastMatch m : sv.objectSelectionLastResult.getMatches()) {
            Node n = m.getNode();
            //add( new JMenuItem(n.getName()+" ("+n.getNodeType()+")") ) -- getNodeType() says Node for everything...
            add( new JMenuItem(n.getName()) )
                .addActionListener((e) -> sv.setActiveNode(n));
        }
    }
}

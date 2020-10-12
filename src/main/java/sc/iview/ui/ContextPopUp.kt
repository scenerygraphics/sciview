package sc.iview.ui

import graphics.scenery.Node
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class ContextPopUp(n: Node) : JPopupMenu() {
    init {
        add(JMenuItem("Name: " + n.name))
        add(JMenuItem("Type: " + n.nodeType))
    }
}
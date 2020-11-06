package sc.iview.ui

import sc.iview.SciView
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class ContextPopUpNodeChooser(sv: SciView) : JPopupMenu() {
    init {
        for (m in sv.objectSelectionLastResult.matches) {
            var n = m.node
            add( JMenuItem(n.name) )
                    .addActionListener { sv.activeNode = n }
        }
    }
}
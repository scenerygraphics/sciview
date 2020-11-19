package sc.iview.ui

import sc.iview.SciView
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class ContextPopUpNodeChooser(sv: SciView) : JPopupMenu() {
    init {
        sv.controls.objectSelectionLastResult?.matches?.forEach { match ->
            add( JMenuItem(match.node.name) )
                    .addActionListener { sv.setActiveNode(match.node) }
        }
    }
}
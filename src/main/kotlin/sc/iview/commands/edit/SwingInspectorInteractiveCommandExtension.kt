package sc.iview.commands.edit

import graphics.scenery.Node
import org.scijava.ui.swing.widget.SwingInputPanel

interface SwingInspectorInteractiveCommandExtension {
    fun create(inputPanel: SwingInputPanel, sceneNode: Node, uiDebug: Boolean)
}
package sc.iview.commands.edit

import graphics.scenery.Node
import org.scijava.ui.swing.widget.SwingInputPanel

/**
 * Interface for Swing-based extensions for [InspectorInteractiveCommand].
 */
interface SwingInspectorInteractiveCommandExtension {

    /**
     * The create() function is called when the Swing panel is contstructed, with [inputPanel] being
     * the panel under construction, to which Swing objects can be attached. [sceneNode] is the
     * currently-selected [Node] for which the inspector is being constructed, and [uiDebug] is
     * a debug flag which can be used to determine whether additional debug output or visualisation
     * should be provided.
     */
    fun create(inputPanel: SwingInputPanel, sceneNode: Node, uiDebug: Boolean)
}
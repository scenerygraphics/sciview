package sc.iview.commands.edit

import graphics.scenery.Node
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.TransferFunctionEditor
import graphics.scenery.volumes.Volume
import net.miginfocom.swing.MigLayout
import org.jfree.chart.ChartPanel
import org.scijava.ui.swing.widget.SwingInputPanel
import sc.iview.ui.SwingNodePropertyEditor.Companion.maybeActivateDebug
import java.awt.Dimension
import javax.swing.JPanel

/**
 * Inspector extension that provides a [TransferFunctionEditor] for [Volume] nodes.
 */
class SwingVolumeProperties : SwingInspectorInteractiveCommandExtension {
    val logger by lazyLogger()

    /**
     * Creates a [TransferFunctionEditor] panel as child of the [inputPanel]'s Volume group.
     */
    override fun create(inputPanel: SwingInputPanel, sceneNode: Node, uiDebug: Boolean) {
        if(sceneNode !is Volume) {
            logger.error("Wrong node type for SwingVolumeProperties")
            return
        }
        // This will find the group that corresponds to the expandable label. If the type of
        // the node is indeed Volume, this must exist.
        val parent = inputPanel.component.components.find { it.name == "group:Volume" } as? JPanel
        val tfe = TransferFunctionEditor(sceneNode)
        tfe.preferredSize = Dimension(300, 300)
        tfe.components.find { it is ChartPanel }?.minimumSize = Dimension(100, 200)

        tfe.layout = MigLayout("fillx,flowy,insets 0 0 0 0".maybeActivateDebug(uiDebug), "[right,fill,grow]")
        parent?.add(tfe, "span 2, growx")
    }
}
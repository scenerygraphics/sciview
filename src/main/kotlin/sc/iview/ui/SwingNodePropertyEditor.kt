/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
package sc.iview.ui

import com.intellij.ui.components.JBPanel
import graphics.scenery.Camera
import graphics.scenery.Node
import graphics.scenery.Scene
import graphics.scenery.volumes.TransferFunctionEditor
import graphics.scenery.volumes.Volume
import net.miginfocom.swing.MigLayout
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.Context
import org.scijava.command.CommandService
import org.scijava.event.EventHandler
import org.scijava.log.LogService
import org.scijava.module.*
import org.scijava.plugin.Parameter
import org.scijava.plugin.PluginService
import org.scijava.service.Service
import org.scijava.ui.swing.widget.SwingInputPanel
import org.scijava.util.DebugUtils
import org.scijava.widget.UIComponent
import sc.iview.SciView
import sc.iview.commands.edit.Properties
import sc.iview.commands.help.Help
import sc.iview.event.NodeActivatedEvent
import sc.iview.event.NodeAddedEvent
import sc.iview.event.NodeChangedEvent
import sc.iview.event.NodeRemovedEvent
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JMenuItem
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JSplitPane
import javax.swing.JTextArea
import javax.swing.JTree
import javax.swing.WindowConstants
import javax.swing.border.EmptyBorder
import javax.swing.event.TreeSelectionEvent
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import kotlin.concurrent.thread

/**
 * Interactive UI for visualizing and editing the scene graph.
 *
 * @author Curtis Rueden
 * @author Ulrik Guenther
 */
class SwingNodePropertyEditor(private val sciView: SciView) : UIComponent<JPanel> {
    @Parameter
    private lateinit var pluginService: PluginService

    @Parameter
    private lateinit var moduleService: ModuleService

    @Parameter
    private lateinit var commandService: CommandService

    @Parameter
    private lateinit var log: LogService

    private var panel: JPanel? = null
    private lateinit var treeModel: DefaultTreeModel

    lateinit var tree: JTree
        private set
    lateinit var props: JBPanel<*>

    fun getProps(): JPanel {
        return props
    }

    /** Creates and displays a window containing the scene editor.  */
    fun show() {
        val frame = JFrame("Node Properties")
        frame.setLocation(200, 200)
        frame.contentPane = component
        // FIXME: Why doesn't the frame disappear when closed?
        frame.defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        frame.setSize(600, 400)
        frame.isVisible = true
    }

    override fun getComponent(): JPanel {
        if (panel == null) {
            initPanel()
        }
        return panel!!
    }

    override fun getComponentType(): Class<JPanel> {
        return JPanel::class.java
    }

    @EventHandler
    private fun onEvent(evt: NodeAddedEvent) {
        val node = evt.node ?: return
        log.trace("Node added: $node");
        rebuildTree()
    }

    @EventHandler
    private fun onEvent(evt: NodeRemovedEvent) {
        val node = evt.node ?: return
        log.trace("Node removed: $node");
        rebuildTree()
        if (sciView.activeNode == null){
            updateProperties(null)
        }
    }

    @EventHandler
    private fun onEvent(evt: NodeChangedEvent) {
        val node = evt.node ?: return
        if (node == sciView.activeNode) {
            updateProperties(sciView.activeNode)
        }
        updateTree(node)
    }

    @EventHandler
    private fun onEvent(evt: NodeActivatedEvent) {
        val node = evt.node ?: return
        updateProperties(node)
        updateTree(node)
    }

    /** Initializes [.panel].  */
    @Synchronized
    private fun initPanel() {
        if (panel != null) return
        val p = JPanel()
        p.layout = BorderLayout()
        createTree()
        props = JBPanel<Nothing>()
        props.layout = MigLayout("inset 0", "[grow,fill]", "[grow,fill]")
        updateProperties(null)
        val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT,  //
                JScrollPane(tree),  //
                JScrollPane(props))
        splitPane.dividerLocation = 200
        p.add(splitPane, BorderLayout.CENTER)
        panel = p
    }

    private fun createTree() {
        treeModel = DefaultTreeModel(SwingSceneryTreeNode(sciView))
        tree = JTree(treeModel)
        tree.isRootVisible = true
        tree.cellRenderer = SwingNodePropertyTreeCellRenderer()
        //        tree.setCellEditor(new NodePropertyTreeCellEditor(sciView));
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        tree.addTreeSelectionListener { e: TreeSelectionEvent ->
            val sceneNode = sceneNode(e.newLeadSelectionPath)
            sciView.setActiveNode(sceneNode)
            updateProperties(sceneNode)
        }
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                super.mouseClicked(e)
                if (e.clickCount == 2 && e.button == MouseEvent.BUTTON1) {
                    val n = tree.lastSelectedPathComponent as? SwingSceneryTreeNode ?: return
                    val node = n.userObject as? Node ?: return
                    sciView.setActiveCenteredNode(node)
                } else if (e.button == MouseEvent.BUTTON3) {
                    val x = e.x
                    val y = e.y
                    val tree = e.source as JTree
                    val path = tree.getPathForLocation(x, y) ?: return
                    tree.selectionPath = path
                    val obj = path.lastPathComponent as? SwingSceneryTreeNode ?: return
                    val popup = JPopupMenu()

                    val labelItem = if(obj.node == null) {
                        return
                    } else {
                        JMenuItem(obj.node.name)
                    }
                    labelItem.isEnabled = false
                    popup.add(labelItem)
                    if (obj.node is Camera) {
                        val resetItem = JMenuItem("Reset camera")
                        resetItem.foreground = Color.RED
                        resetItem.addActionListener { _: ActionEvent? ->
                            obj.node.position = Vector3f(0.0f)
                            obj.node.rotation = Quaternionf(0.0f, 0.0f, 0.0f, 1.0f)
                        }
                        popup.add(resetItem)
                    }
                    val hideShow = JMenuItem("Hide")
                    if (obj.node.visible) {
                        hideShow.text = "Hide"
                    } else {
                        hideShow.text = "Show"
                    }
                    hideShow.addActionListener { _: ActionEvent? -> obj.node.visible = !obj.node.visible }
                    popup.add(hideShow)
                    val removeItem = JMenuItem("Remove")
                    removeItem.foreground = Color.RED
                    removeItem.addActionListener { _: ActionEvent? -> sciView.deleteNode(obj.node, true) }
                    popup.add(removeItem)
                    popup.show(tree, x, y)
                } else {
                    val path = tree.getPathForLocation(e.x, e.y)
                    if (path != null && e.x / 1.2 < tree.getPathBounds(path).x + 16) {
                        val n = path.lastPathComponent as? SwingSceneryTreeNode
                        if (n != null) {
                            val node = n.node
                            if (node != null && node !is Camera && node !is Scene) {
                                node.visible = !node.visible
                                tree.repaint()
                            }
                        }
                        e.consume()
                    }
                }
            }
        })
    }

    private fun updateTree(node: Node) {
        val treeNode = getTreeNode(node, treeModel.root as TreeNode)?: return
        treeModel.nodeChanged(treeNode)
    }

    private fun getTreeNode(node: Node, parent: TreeNode): TreeNode? {
        for(i in 0..treeModel.getChildCount(parent)-1) {
            val child = treeModel.getChild(parent, i) as SwingSceneryTreeNode
            if(child.node == node) return child
            val treeNode = getTreeNode(node, child)
            if(treeNode != null) return treeNode
        }
        return null;
    }

    var currentNode: Node? = null
        private set
    private var currentProperties: Properties? = null
    private lateinit var inputPanel: SwingInputPanel
    private val updateLock = ReentrantLock()

    /** Generates a properties panel for the given node.  */
    fun updateProperties(sceneNode: Node?, rebuild: Boolean = false) {
        if (sceneNode == null) {
            try {
                if (updateLock.tryLock() || updateLock.tryLock(200, TimeUnit.MILLISECONDS)) {
                    updatePropertiesPanel(null)
                    updateLock.unlock()
                }
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            return
        }
        try {
            if (updateLock.tryLock() || updateLock.tryLock(200, TimeUnit.MILLISECONDS)) {
                if (!rebuild && currentNode === sceneNode && currentProperties != null) {
                    currentProperties!!.updateCommandFields()
                    inputPanel.refresh()
                    updateLock.unlock()
                    return
                }
                currentNode = sceneNode
                // Prepare the Properties command module instance.
                val info = commandService.getCommand(Properties::class.java)
                val module = moduleService.createModule(info)
                resolveInjectedInputs(module)
                module.setInput("sciView", sciView)
                module.setInput("sceneNode", sceneNode)
                module.resolveInput("sciView")
                module.resolveInput("sceneNode")
                val p = module.delegateObject as Properties
                currentProperties = p
                p.setSceneNode(sceneNode)

                val additionalUIs = sceneNode.metadata
                    .filter { it.key.startsWith("sciview-inspector-") }
                    .filter { it.value as? CustomPropertyUI != null }

                @Suppress("UNCHECKED_CAST")
                additionalUIs.forEach { (name, value) ->
                    val ui = value as CustomPropertyUI
                    log.info("Additional UI requested by $name, module ${ui.module.info.name}")

                    for (moduleItem in ui.getMutableInputs()) {
                        log.info("${moduleItem.name}/${moduleItem.label} added, based on ${ui.module}")
                        p.addInput(moduleItem, ui.module)
                    }
                }

                // Prepare the SwingInputHarvester.
                val pluginInfo = pluginService.getPlugin(SwingGroupingInputHarvester::class.java)
                val pluginInstance = pluginService.createInstance(pluginInfo)
                val harvester = pluginInstance as SwingGroupingInputHarvester
                inputPanel = harvester.createInputPanel()
                inputPanel.component.layout = MigLayout("fillx,wrap 1,debug,insets 0 0 0 0", "[right,fill,grow]")

                // Build the panel.
                try {
                    harvester.buildPanel(inputPanel, module)
                    updatePropertiesPanel(inputPanel.component)

                    // TODO: This needs to move to a widget and be included in Properties
                    if(sceneNode is Volume) {
                        val tfe = TransferFunctionEditor(sceneNode, sceneNode.name)
                        tfe.preferredSize = Dimension(300, 300)
                        tfe.layout = MigLayout("fillx,flowy,insets 0 0 0 0, debug", "[right,fill,grow]")
                        inputPanel.component.add(tfe)
                    }

                } catch (exc: ModuleException) {
                    log.error(exc)
                    val stackTrace = DebugUtils.getStackTrace(exc)
                    val textArea = JTextArea()
                    textArea.text = "<html><pre>$stackTrace</pre>"
                    updatePropertiesPanel(textArea)
                }

                updateLock.unlock()
            }
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    private fun updatePropertiesPanel(c: Component?) {
        props.removeAll()
        if (c == null) {
            var handler = sciView.sceneryInputHandler
            if (handler == null){
                // at startup this panel is initialized before scenery. Wait until sceneries input handler is initialized and start again.
                thread {
                    while (handler == null) {
                        Thread.sleep(200)
                        handler = sciView.sceneryInputHandler
                    }
                    updatePropertiesPanel(null)
                }
                return
            }
            val usageLabel = JLabel(
                ("<html><em>No node selected.</em><br><br>" +
                        Help.getBasicUsageText(handler)
                        + "</html>")
            )
            usageLabel.border = EmptyBorder(2, 5, 0, 5);
            usageLabel.preferredSize = Dimension(300, 100)
            props.add(usageLabel,"wrap")
        } else {
            props.add(c)
            props.size = c.size
        }
        props.validate()
        props.repaint()
    }

    private fun find(root: DefaultMutableTreeNode, n: Node): TreePath? {
        val e = root.depthFirstEnumeration()
        while (e.hasMoreElements()) {
            val node = e.nextElement() as DefaultMutableTreeNode
            if (node.userObject === n) {
                return TreePath(node.path)
            }
        }
        return null
    }

    /** Rebuilds the tree to match the state of the scene.  */
    fun rebuildTree() {
        val currentPath = tree.selectionPath
        treeModel.setRoot(SwingSceneryTreeNode(sciView))

//        treeModel.reload();
//        // TODO: retain previously expanded nodes only
//        for( int i = 0; i < tree.getRowCount(); i++ ) {
//            tree.expandRow( i );
//        }
//        updateProperties( sciView.getActiveNode() );
        if (currentPath != null) {
            val selectedNode = (currentPath.lastPathComponent as SwingSceneryTreeNode).node ?: return
            trySelectNode(selectedNode)
        }
    }

    fun trySelectNode(node: Node) {
        val newPath = find(treeModel.root as DefaultMutableTreeNode, node)
        if (newPath != null) {
            tree.selectionPath = newPath
            tree.scrollPathToVisible(newPath)
            updateProperties(node)
        }
    }

    /** Retrieves the scenery node of a given tree node.  */
    private fun sceneNode(treePath: TreePath?): Node? {
        if (treePath == null) return null
        val treeNode = treePath.lastPathComponent as? SwingSceneryTreeNode ?: return null
        val userObject = treeNode.userObject
        return if (userObject !is Node) null else userObject
    }

    /** HACK: Resolve injected [Context] and [Service] inputs.  */
    private fun resolveInjectedInputs(module: Module) {
        for (input in module.info.inputs()) {
            val type = input.type
            if (Context::class.java.isAssignableFrom(type) || Service::class.java.isAssignableFrom(type)) {
                module.resolveInput(input.name)
            }
        }
    }

    init {
        sciView.scijavaContext!!.inject(this)
    }
}

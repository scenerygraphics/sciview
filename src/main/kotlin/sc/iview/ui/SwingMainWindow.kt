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

import com.formdev.flatlaf.FlatLightLaf
import graphics.scenery.Node
import graphics.scenery.SceneryElement
import graphics.scenery.backends.Renderer
import graphics.scenery.utils.lazyLogger
import graphics.scenery.utils.SceneryJPanel
import org.joml.Vector2f
import org.scijava.menu.MenuService
import org.scijava.ui.swing.menu.SwingJMenuBarCreator
import sc.iview.SciView
import sc.iview.SciViewService
import sc.iview.SplashLabel
import sc.iview.Utils
import java.awt.*
import java.awt.event.*
import java.util.*
import javax.script.ScriptException
import javax.swing.*
import javax.swing.Timer
import kotlin.concurrent.thread
import kotlin.math.roundToInt

/**
 * Class for Swing-based main window.
 *
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
class SwingMainWindow(val sciview: SciView) : MainWindow {
    private val logger by lazyLogger()
    private var previousSidebarPosition = 0

    lateinit var sceneryJPanel: SceneryJPanel
        private set
    lateinit var mainSplitPane: JSplitPane
        protected set
    lateinit var inspector: JSplitPane
        protected set
    lateinit var interpreterPane: REPLPane
        protected set
    lateinit var nodePropertyEditor: SwingNodePropertyEditor
        protected set
    lateinit var frame: JFrame
        protected set
    var sidebarOpen = false
        protected set

    private lateinit var splashLabel: SplashLabel
    private var defaultSidebarAction: (() -> Any)? = null

    init {
        val initializer = Runnable {
            FlatLightLaf.setup()
            try {
                UIManager.setLookAndFeel(FlatLightLaf())
            } catch (ex: Exception) {
                System.err.println("Failed to initialize Flat Light LaF, falling back to Swing default.")
            }

            var x: Int
            var y: Int
            try {
                val screenSize = Toolkit.getDefaultToolkit().screenSize
                x = screenSize.width / 2 - sciview.windowWidth / 2
                y = screenSize.height / 2 - sciview.windowHeight / 2
            } catch (e: HeadlessException) {
                x = 10
                y = 10
            }

            frame = JFrame("sciview")
            frame.layout = BorderLayout(0, 0)
            frame.setSize(sciview.windowWidth, sciview.windowHeight)
            frame.setLocation(x, y)

            splashLabel = SplashLabel()
            val glassPane = frame.glassPane as JPanel
            glassPane.isVisible = true
            glassPane.layout = BorderLayout()
            glassPane.isOpaque = false
            glassPane.add(splashLabel, BorderLayout.CENTER)
            glassPane.requestFocusInWindow()
            glassPane.revalidate()

            frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            nodePropertyEditor = SwingNodePropertyEditor(sciview)

            sceneryJPanel = SceneryJPanel()
            JPopupMenu.setDefaultLightWeightPopupEnabled(false)
            val swingMenuBar = JMenuBar()
            val menus = sciview.scijavaContext?.getService(MenuService::class.java)
                    ?: throw IllegalStateException("MenuService not available")
            SwingJMenuBarCreator().createMenus(menus.getMenu("SciView"), swingMenuBar)

            val bar = ProgressPie()
            bar.value = 0.0
            bar.minimumSize = Dimension(30, 30)
            bar.maximumSize = Dimension(30, 30)
            bar.preferredSize = Dimension(30, 30)
            val progressLabel = JLabel("<html><strong></strong></html>")
            progressLabel.horizontalAlignment = SwingConstants.RIGHT
            swingMenuBar.add(Box.createHorizontalGlue())
            swingMenuBar.add(progressLabel)
            swingMenuBar.add(bar)
            frame.jMenuBar = swingMenuBar

            sciview.taskManager.update = { current ->
                if (current != null) {
                    progressLabel.text = "<html><strong>${current.source}:</strong> ${current.status} </html>"
                    bar.value = current.completion.toDouble()
                } else {
                    progressLabel.text = ""
                    bar.value = 0.0
                }

                bar.repaint()
            }
            sceneryJPanel.isVisible = true
            nodePropertyEditor.component // Initialize node property panel

            val inspectorTree = nodePropertyEditor.tree
            inspectorTree.toggleClickCount = 0 // This disables expanding menus on double click
            val inspectorProperties = nodePropertyEditor.props

            val container = JPanel(CardLayout())

            var propsPane = JScrollPane(inspectorProperties)
            var treePane = JScrollPane(inspectorTree)
            propsPane.verticalScrollBar.unitIncrement = 16
            treePane.verticalScrollBar.unitIncrement = 16
            inspector = JSplitPane(JSplitPane.VERTICAL_SPLIT,  //
                    treePane,
                    propsPane)
            inspector.dividerLocation = sciview.windowHeight / 3
            inspector.isContinuousLayout = true
            inspector.border = BorderFactory.createEmptyBorder()
            inspector.dividerSize = 4
            inspector.name = "Inspector"
            container.add(inspector, "Inspector")

            // We need to get the surface scale here before initialising scenery's renderer, as
            // the information is needed already at initialisation time.
            val dt = frame.graphicsConfiguration.defaultTransform
            val surfaceScale = Vector2f(dt.scaleX.toFloat(), dt.scaleY.toFloat())
            sciview.getScenerySettings().set("Renderer.SurfaceScale", surfaceScale)
            interpreterPane = REPLPane(sciview.scijavaContext)
            interpreterPane.component.border = BorderFactory.createEmptyBorder()
            interpreterPane.component.name = "REPL"
            container.add(interpreterPane.component, "REPL")
            thread {
                initializeInterpreter()
            }
            mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                    sceneryJPanel,
                    container
            )
            mainSplitPane.dividerLocation = frame.width
            mainSplitPane.border = BorderFactory.createEmptyBorder()
            mainSplitPane.dividerSize = 4
            mainSplitPane.resizeWeight = 1.0
            mainSplitPane.rightComponent = null

            val toolbar = JToolBar()
            toolbar.orientation = SwingConstants.VERTICAL

            val inspectorIcon = Utils.getScaledImageIcon(SciView::class.java.getResource("toolbox.png"), 16, 16)
            val inspectorButton = JToggleButton()
            inspectorButton.toolTipText = "Inspector"

            val inspectorAction = object : AbstractAction("Inspector", inspectorIcon) {
                override fun actionPerformed(e: ActionEvent) {
                    container.toggleSidebarComponent(inspector, toolbar, inspectorButton, e)
                }
            }

            defaultSidebarAction = { container.toggleSidebarComponent(inspector, toolbar, inspectorButton, null) }

            inspectorButton.action = inspectorAction
            inspectorButton.icon = inspectorIcon
            inspectorButton.hideActionText = true

            val replIcon = Utils.getScaledImageIcon(SciView::class.java.getResource("terminal.png"), 16, 16)
            val replButton = JToggleButton()
            replButton.toolTipText = "Script Interpreter"

            val replAction = object : AbstractAction("REPL", replIcon) {
                override fun actionPerformed(e: ActionEvent) {
                    container.toggleSidebarComponent(interpreterPane.component, toolbar, replButton, e)
                }
            }

            replButton.action = replAction
            replButton.icon = replIcon
            replButton.hideActionText = true

            toolbar.add(inspectorButton)
            toolbar.add(replButton)

            //frame.add(mainSplitPane, BorderLayout.CENTER);
            frame.add(mainSplitPane, BorderLayout.CENTER)
            frame.add(toolbar, BorderLayout.EAST)
            frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
            frame.addWindowListener(object : WindowAdapter() {
                override fun windowClosing(e: WindowEvent) {
                    logger.debug("Closing SciView window.")
                    close()
                    sciview.scijavaContext?.service(SciViewService::class.java)?.close(sciview)
                    sciview.isClosed = true
                }
            })

            frame.isVisible = true
            glassPane.repaint()

            sciview.sceneryPanel[0] = sceneryJPanel
            val renderer = Renderer.createRenderer(
                    sciview.hub,
                    sciview.applicationName,
                    sciview.currentScene,
                    sciview.windowWidth,
                    sciview.windowHeight,
                    sciview.sceneryPanel[0]
            )

            sciview.setRenderer(renderer)
            sciview.hub.add(SceneryElement.Renderer, renderer)
            sciview.reset()

            SwingUtilities.invokeLater {
                try {
                    while (!sciview.getSceneryRenderer()!!.firstImageReady) {
                        logger.debug("Waiting for renderer initialisation")
                        Thread.sleep(300)
                    }
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    logger.error("Renderer construction interrupted.")
                }
                nodePropertyEditor.rebuildTree()
                logger.info("Done initializing SciView")

                // subscribe to Node{Added, Removed, Changed} events, happens automatically
//            eventService!!.subscribe(this)
                sceneryJPanel.isVisible = true

                // install hook to keep inspector updated on external changes (scripting, etc)
                sciview.currentScene.onNodePropertiesChanged["updateInspector"] = { node: Node ->
                    if (node === nodePropertyEditor.currentNode) {
                        nodePropertyEditor.updateProperties(node)
                    }
                }

                // Enable push rendering by default
                renderer.pushMode = true
                sciview.camera!!.setPosition(1.65, 1)
                glassPane.isVisible = false

                sceneryJPanel.minimumSize = Dimension(256, 256)
            }
        }

        if(SwingUtilities.isEventDispatchThread()) {
            initializer.run()
        } else {
            SwingUtilities.invokeAndWait(initializer)
        }
    }

    fun JPanel.toggleSidebarComponent(component: Component, toolbar: JToolBar, button: JToggleButton, event: ActionEvent?) {
        logger.debug("Button: ${button.text} (${button.isSelected}), event origin: ${(event?.source as? JToggleButton)?.text}")
        if(previousSidebarPosition == 0) {
            // default sidebar open position is 2/3 of the window width
            previousSidebarPosition = (frame.width * 0.666f).roundToInt()
        }

        if(!sidebarOpen) {
            logger.debug("Sidebar closed, opening ${component.name}")
            mainSplitPane.dividerLocation = previousSidebarPosition
            mainSplitPane.rightComponent = this
            val cl = this.layout as CardLayout
            cl.show(this, component.name)
            sidebarOpen = true

            return
        }

        if(event == null) {
            mainSplitPane.dividerLocation = frame.width
            mainSplitPane.rightComponent = null
            toolbar.components.filterIsInstance<JToggleButton>().forEach { (it as? JToggleButton)?.isSelected = false }
            sidebarOpen = false
            return
        }

        if(sidebarOpen && button.isSelected && event.source == button){
            logger.debug("Sidebar open on other, opening ${button.text}")
            toolbar.components.filter { it is JToggleButton && it != button }.forEach { (it as? JToggleButton)?.isSelected = false }
            val cl = this.layout as CardLayout
            cl.show(this, component.name)
            sidebarOpen = true
        }

        if(sidebarOpen && !button.isSelected && event.source == button){
            logger.debug("Sidebar open, closing")
            mainSplitPane.dividerLocation = frame.width
            mainSplitPane.rightComponent = null
            this.components.forEach { it.isVisible = false }
            sidebarOpen = false
        }
    }


    /**
     * Toggling the sidebar for inspector, REPL, etc, returns the new state, where true means visible.
     */
    override fun toggleSidebar(): Boolean {
        defaultSidebarAction?.invoke()
        return sidebarOpen
    }

    /**
     * Initializer for the REPL.
     */
    override fun initializeInterpreter() {
        val startupCode = Scanner(SciView::class.java.getResourceAsStream("startup.py"), "UTF-8").useDelimiter("\\A").next()
        interpreterPane.repl.interpreter.bindings["sciview"] = this.sciview
        try {
            interpreterPane.repl.interpreter.eval(startupCode)
        } catch (e: ScriptException) {
            e.printStackTrace()
        }
    }

    /**
     * Adds a new [WindowListener], [wl], to the [frame].
     */
    fun addWindowListener(wl: WindowListener?) {
        frame.addWindowListener(wl)
    }

    /**
     * Shows a context menu in the rendering window at [x], [y].
     */
    override fun showContextNodeChooser(x: Int, y: Int) {
        ContextPopUpNodeChooser(sciview).show(sceneryJPanel, x, y)
    }

    /**
     * Signal to rebuild scene tree in the UI.
     */
    override fun rebuildSceneTree() {
        nodePropertyEditor.rebuildTree()
    }

    /**
     * Signal to select a specific [node].
     */
    override fun selectNode(node: Node?) {
        if(node != null) {
            nodePropertyEditor.trySelectNode(node)
        }
    }

    /**
     * Closes the main window.
     */
    override fun close() {
        frame.dispose()
        sciview.dispose()
    }
}

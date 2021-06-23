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
import com.intellij.ui.tabs.JBTabsPosition
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBEditorTabs
import graphics.scenery.Node
import graphics.scenery.SceneryElement
import graphics.scenery.backends.Renderer
import graphics.scenery.utils.LazyLogger
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

/**
 * Class for Swing-based main window.
 *
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
class SwingMainWindow(val sciview: SciView) : MainWindow {
    private val logger by LazyLogger()
    private var sidebarHidden = false
    private var previousSidebarPosition = 0

    var sceneryJPanel: SceneryJPanel
        private set
    var mainSplitPane: JSplitPane
        protected set
    var inspector: JSplitPane
        protected set
    var interpreterPane: REPLPane
        protected set
    var nodePropertyEditor: SwingNodePropertyEditor
        protected set
    var frame: JFrame
        protected set

    private var splashLabel: SplashLabel

    init {
        FlatLightLaf.install()
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

        frame = JFrame("SciView")
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
        val menus = sciview.scijavaContext?.getService(MenuService::class.java) ?: throw IllegalStateException("MenuService not available")
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
            if(current != null) {
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
        val tp = JBEditorTabs(null)
        tp.tabsPosition = JBTabsPosition.right
        tp.isSideComponentVertical = true

        inspector = JSplitPane(JSplitPane.VERTICAL_SPLIT,  //
                JScrollPane(inspectorTree),
                JScrollPane(inspectorProperties))
        inspector.dividerLocation = sciview.windowHeight / 3
        inspector.isContinuousLayout = true
        inspector.border = BorderFactory.createEmptyBorder()
        inspector.dividerSize = 1

        val inspectorIcon = Utils.getScaledImageIcon(SciView::class.java.getResource("toolbox.png"), 16, 16)
        val tiInspector = TabInfo(inspector, inspectorIcon)
        tiInspector.text = ""
        tp.addTab(tiInspector)

        // We need to get the surface scale here before initialising scenery's renderer, as
        // the information is needed already at initialisation time.
        val dt = frame.graphicsConfiguration.defaultTransform
        val surfaceScale = Vector2f(dt.scaleX.toFloat(), dt.scaleY.toFloat())
        sciview.getScenerySettings().set("Renderer.SurfaceScale", surfaceScale)
        interpreterPane = REPLPane(sciview.scijavaContext)
        interpreterPane.component.border = BorderFactory.createEmptyBorder()
        val interpreterIcon = Utils.getScaledImageIcon(SciView::class.java.getResource("terminal.png"), 16, 16)
        val tiREPL = TabInfo(interpreterPane.component, interpreterIcon)
        tiREPL.text = ""
        tp.addTab(tiREPL)
        tp.addTabMouseListener(object : MouseListener {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    toggleSidebar()
                }
            }

            override fun mousePressed(e: MouseEvent) {}
            override fun mouseReleased(e: MouseEvent) {}
            override fun mouseEntered(e: MouseEvent) {}
            override fun mouseExited(e: MouseEvent) {}
        })
        initializeInterpreter()
        mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT,  //
                sceneryJPanel,
                tp.component
        )
        mainSplitPane.dividerLocation = frame.size.width - 36
        mainSplitPane.border = BorderFactory.createEmptyBorder()
        mainSplitPane.dividerSize = 1
        mainSplitPane.resizeWeight = 0.5
        sidebarHidden = true

        //frame.add(mainSplitPane, BorderLayout.CENTER);
        frame.add(mainSplitPane, BorderLayout.CENTER)
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                logger.debug("Closing SciView window.")
                close()
                sciview.scijavaContext.service(SciViewService::class.java).close(sciview)
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

    /**
     * Toggling the sidebar for inspector, REPL, etc, returns the new state, where true means visible.
     */
    override fun toggleSidebar(): Boolean {
        if (!sidebarHidden) {
            previousSidebarPosition = mainSplitPane.dividerLocation
            // TODO: remove hard-coded tab width
            mainSplitPane.dividerLocation = frame.size.width - 36
            sidebarHidden = true
        } else {
            if (previousSidebarPosition == 0) {
                previousSidebarPosition = sciview.windowWidth / 3 * 2
            }
            mainSplitPane.dividerLocation = previousSidebarPosition
            sidebarHidden = false
        }
        return sidebarHidden
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
        sciview.close()
    }
}

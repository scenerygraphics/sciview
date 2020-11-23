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
import org.lwjgl.system.Platform
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
import kotlin.concurrent.thread

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

        // TODO: check for jdk 8 v. jdk 11 on linux and choose renderer accordingly
        if (Platform.get() === Platform.LINUX) {
            var version = System.getProperty("java.version")
            if (version.startsWith("1.")) {
                version = version.substring(2, 3)
            } else {
                val dot = version.indexOf(".")
                if (dot != -1) {
                    version = version.substring(0, dot)
                }
            }

            // If Linux and JDK 8, then use OpenGLRenderer
            if (version == "8") System.setProperty("scenery.Renderer", "OpenGLRenderer")
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

        val p = JPanel(BorderLayout(0, 0))
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

        p.layout = OverlayLayout(p)
        p.background = Color(50, 48, 47)
        p.add(sceneryJPanel, BorderLayout.CENTER)

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
                p,
                tp.component
        )
        mainSplitPane.dividerLocation = frame.size.width - 36
        mainSplitPane.border = BorderFactory.createEmptyBorder()
        mainSplitPane.dividerSize = 1
        mainSplitPane.resizeWeight = 0.9
        sidebarHidden = true

        //frame.add(mainSplitPane, BorderLayout.CENTER);
        frame.add(mainSplitPane, BorderLayout.CENTER)
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
        interpreterPane.repl.interpreter.bindings["sciView"] = this
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
    }
}
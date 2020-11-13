/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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
package sc.iview

import bdv.BigDataViewer
import bdv.cache.CacheControl
import bdv.tools.brightness.ConverterSetup
import bdv.util.AxisOrder
import bdv.util.RandomAccessibleIntervalSource
import bdv.util.RandomAccessibleIntervalSource4D
import bdv.util.volatiles.VolatileView
import bdv.viewer.Source
import bdv.viewer.SourceAndConverter
import cleargl.GLVector
import com.formdev.flatlaf.FlatLightLaf
import com.intellij.ui.tabs.JBTabsPosition
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.impl.JBEditorTabs
import graphics.scenery.*
import graphics.scenery.Box
import graphics.scenery.Scene.RaycastResult
import graphics.scenery.backends.Renderer
import graphics.scenery.backends.opengl.OpenGLRenderer
import graphics.scenery.backends.vulkan.VulkanRenderer
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerInput
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.controls.behaviours.FPSCameraControl
import graphics.scenery.controls.behaviours.MovementCommand
import graphics.scenery.controls.behaviours.SelectCommand
import graphics.scenery.utils.*
import graphics.scenery.utils.ExtractsNatives.Companion.getPlatform
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.RAIVolume
import graphics.scenery.volumes.Volume
import graphics.scenery.volumes.Volume.Companion.fromXML
import graphics.scenery.volumes.Volume.Companion.setupId
import graphics.scenery.volumes.Volume.VolumeDataSource.RAISource
import io.scif.SCIFIOService
import net.imagej.Dataset
import net.imagej.ImageJService
import net.imagej.axis.CalibratedAxis
import net.imagej.axis.DefaultAxisType
import net.imagej.axis.DefaultLinearAxis
import net.imagej.interval.CalibratedRealInterval
import net.imagej.lut.LUTService
import net.imagej.mesh.Mesh
import net.imagej.units.UnitService
import net.imglib2.*
import net.imglib2.display.ColorTable
import net.imglib2.img.Img
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.NumericType
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.view.Views
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.lwjgl.system.Platform
import org.scijava.Context
import org.scijava.`object`.ObjectService
import org.scijava.command.CommandService
import org.scijava.display.Display
import org.scijava.event.EventHandler
import org.scijava.event.EventService
import org.scijava.io.IOService
import org.scijava.log.LogLevel
import org.scijava.log.LogService
import org.scijava.menu.MenuService
import org.scijava.plugin.Parameter
import org.scijava.service.SciJavaService
import org.scijava.thread.ThreadService
import org.scijava.ui.behaviour.Behaviour
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.swing.menu.SwingJMenuBarCreator
import org.scijava.util.ColorRGB
import org.scijava.util.Colors
import org.scijava.util.VersionUtils
import sc.iview.commands.help.Help
import sc.iview.commands.view.NodePropertyEditor
import sc.iview.controls.behaviours.*
import sc.iview.event.NodeActivatedEvent
import sc.iview.event.NodeAddedEvent
import sc.iview.event.NodeChangedEvent
import sc.iview.event.NodeRemovedEvent
import sc.iview.process.MeshConverter
import sc.iview.ui.ContextPopUpNodeChooser
import sc.iview.ui.REPLPane
import tpietzsch.example2.VolumeViewerOptions
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.*
import java.net.URL
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.*
import java.util.concurrent.Future
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.imageio.ImageIO
import javax.script.ScriptException
import javax.swing.*
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.sin

/**
 * Main SciView class.
 *
 * @author Kyle Harrington
 */
// we suppress unused warnings here because @Parameter-annotated fields
// get updated automatically by SciJava.
class SciView : SceneryBase, CalibratedRealInterval<CalibratedAxis> {
    private val sceneryPanel = arrayOf<SceneryPanel?>(null)

    /**
     * Mouse controls for FPS movement and Arcball rotation
     */
    lateinit var targetArcball: SciView.AnimatedCenteringBeforeArcBallControl
    protected var fpsControl: FPSCameraControl? = null
    /*
     * Return the default floor object
     *//*
     * Set the default floor object
     */
    /**
     * The floor that orients the user in the scene
     */
    var floor: Node? = null
    protected var vrActive = false

    /**
     * The primary camera/observer in the scene
     */
    var camera: Camera? = null
    set(value) {
        field = value
        setActiveObserver(field)
    }

    /**
     * Geometry/Image information of scene
     */
    private lateinit var axes: Array<CalibratedAxis>

    @Parameter
    private val log: LogService? = null

    @Parameter
    private val menus: MenuService? = null

    @Parameter
    private val io: IOService? = null

    @Parameter
    private val eventService: EventService? = null

    @Parameter
    private val lutService: LUTService? = null

    @Parameter
    private val threadService: ThreadService? = null

    @Parameter
    private val objectService: ObjectService? = null

    @Parameter
    private val unitService: UnitService? = null

    /**
     * Queue keeps track of the currently running animations
     */
    private var animations: Queue<Future<*>>? = null

    /**
     * Animation pause tracking
     */
    private var animating = false

    /**
     * This tracks the actively selected Node in the scene
     */
    var activeNode: Node? = null
        private set

    /**
     * Speeds for input controls
     */
    var controlsParameters: ControlsParameters = ControlsParameters()
    /*
     * Return the SciJava Display that contains SciView
     *//*
     * Set the SciJava Display
     */  var display: Display<*>? = null
    private var splashLabel: SplashLabel? = null

    /**
     * Return the current SceneryJPanel. This is necessary for custom context menus
     * @return panel the current SceneryJPanel
     */
    var sceneryJPanel: SceneryJPanel? = null
        private set
    private var mainSplitPane: JSplitPane? = null
    private var inspector: JSplitPane? = null
    private var interpreterPane: REPLPane? = null
    private var nodePropertyEditor: NodePropertyEditor? = null
    var lights: ArrayList<PointLight>? = null
        private set
    private var controlStack: Stack<HashMap<String, Any>>? = null
    private var frame: JFrame? = null
    private val notAbstractNode: Predicate<in Node> = Predicate { node: Node -> !(node is Camera || node is Light || node === floor) }
    var isClosed = false
        private set
    private val notAbstractBranchingFunction = Function { node: Node -> node.children.stream().filter(notAbstractNode).collect(Collectors.toList()) }

    // If true, then when a new node is added to the scene, the camera will refocus on this node by default
    var centerOnNewNodes = false

    // If true, then when a new node is added the thread will block until the node is added to the scene. This is required for
    //   centerOnNewNodes
    var blockOnNewNodes = false
    private var headlight: PointLight? = null

    constructor(context: Context) : super("SciView", 1280, 720, false, context) {
        context.inject(this)
    }

    constructor(applicationName: String?, windowWidth: Int, windowHeight: Int) : super(applicationName!!, windowWidth, windowHeight, false) {}

    fun publicGetInputHandler(): InputHandler {
        return inputHandler!!
    }

    /**
     * Toggle video recording with scenery's video recording mechanism
     * Note: this video recording may skip frames because it is asynchronous
     */
    fun toggleRecordVideo() {
        if (renderer is OpenGLRenderer) (renderer as OpenGLRenderer).recordMovie() else (renderer as VulkanRenderer).recordMovie()
    }

    /**
     * Toggle video recording with scenery's video recording mechanism
     * Note: this video recording may skip frames because it is asynchronous
     *
     * @param filename destination for saving video
     * @param overwrite should the file be replaced, otherwise a unique incrementing counter will be appended
     */
    fun toggleRecordVideo(filename: String?, overwrite: Boolean) {
        if (renderer is OpenGLRenderer) (renderer as OpenGLRenderer).recordMovie(filename!!, overwrite) else (renderer as VulkanRenderer).recordMovie(filename!!, overwrite)
    }

    /**
     * This pushes the current input setup onto a stack that allows them to be restored with restoreControls
     * This pushes the current input setup onto a stack that allows them to be restored with restoreControls.
     * It stacks in particular: all keybindings, all Behaviours, and all step sizes and mouse sensitivities
     * (which are held together in [ControlsParameters]).
     *
     * *Word of warning:* The stashing memorizes *references only* on currently used controls
     * (such as, e.g., [MovementCommand], [FPSCameraControl] or [NodeTranslateControl]),
     * it *does not* create an extra copy of any control. That said, if you modify any control
     * object despite it was already stashed with this method, the change will be visible in the "stored"
     * control and will not go away after the restore... To be on the safe side for now at least, *create
     * new and modified* controls rather than directly changing them.
     */
    fun stashControls() {
        val controlState = HashMap<String, Any>()
        val handler = inputHandler
        if (handler == null) {
            logger.error("InputHandler is null, cannot store controls")
            return
        }

        //behaviours:
        for (actionName in handler.getAllBehaviours()) {
            controlState[STASH_BEHAVIOUR_KEY + actionName] = handler.getBehaviour(actionName) as Any
        }

        //bindings:
        for (actionName in handler.getAllBehaviours()) {
            for (trigger in handler.getKeyBindings(actionName)) {
                controlState[STASH_BINDING_KEY + actionName] = trigger.toString()
            }
        }

        //finally, stash the control parameters
        controlState[STASH_CONTROLSPARAMS_KEY] = controlsParameters

        //...and stash it!
        controlStack!!.push(controlState)
    }

    /**
     * This pops/restores the previously stashed controls. Emits a warning if there are no stashed controls.
     * It first clears all controls, and then resets in particular: all keybindings, all Behaviours,
     * and all step sizes and mouse sensitivities (which are held together in [ControlsParameters]).
     *
     * *Some recent changes may not be removed* with this restore --
     * see discussion in the docs of [SciView.stashControls] for more details.
     */
    fun restoreControls() {
        if (controlStack!!.empty()) {
            logger.warn("Not restoring controls, the controls stash stack is empty!")
            return
        }
        val inputHandler = inputHandler
        if (inputHandler == null) {
            logger.error("InputHandler is null, cannot restore controls")
            return
        }

        //clear the input handler entirely
        for (actionName in inputHandler.getAllBehaviours()) {
            inputHandler.removeKeyBinding(actionName)
            inputHandler.removeBehaviour(actionName)
        }

        //retrieve the most recent stash with controls
        val controlState = controlStack!!.pop()
        for (control in controlState.entries) {
            var key: String
            when {
                control.key.startsWith(STASH_BEHAVIOUR_KEY) -> {
                    //processing behaviour
                    key = control.key.substring(STASH_BEHAVIOUR_KEY.length)
                    inputHandler.addBehaviour(key, control.value as Behaviour)
                }
                control.key.startsWith(STASH_BINDING_KEY) -> {
                    //processing key binding
                    key = control.key.substring(STASH_BINDING_KEY.length)
                    inputHandler.addKeyBinding(key, (control.value as String))
                }
                control.key.startsWith(STASH_CONTROLSPARAMS_KEY) -> {
                    //processing mouse sensitivities and step sizes...
                    controlsParameters = control.value as ControlsParameters
                }
            }
        }
    }

    private val STASH_BEHAVIOUR_KEY = "behaviour:"
    private val STASH_BINDING_KEY = "binding:"
    private val STASH_CONTROLSPARAMS_KEY = "parameters:"

    /**
     * Reset the scene to initial conditions
     */
    fun reset() {
        // Initialize the 3D axes
        axes = arrayOf(
                DefaultLinearAxis(DefaultAxisType("X", true), "um", 1.0),
                DefaultLinearAxis(DefaultAxisType("Y", true), "um", 1.0),
                DefaultLinearAxis(DefaultAxisType("Z", true), "um", 1.0)
        )

        // Remove everything except camera
        val toRemove = getSceneNodes { n: Node? -> n !is Camera }
        for (n in toRemove) {
            deleteNode(n, false)
        }

        // Setup camera
        if (camera == null) {
            camera = DetachedHeadCamera()
            (camera as DetachedHeadCamera).position = Vector3f(0.0f, 1.65f, 0.0f)
            scene.addChild(camera as DetachedHeadCamera)
        }
        camera!!.position = Vector3f(0.0f, 1.65f, 5.0f)
        camera!!.perspectiveCamera(50.0f, windowWidth, windowHeight, 0.1f, 1000.0f)

        // Setup lights
        val tetrahedron = arrayOfNulls<Vector3f>(4)
        tetrahedron[0] = Vector3f(1.0f, 0f, -1.0f / Math.sqrt(2.0).toFloat())
        tetrahedron[1] = Vector3f(-1.0f, 0f, -1.0f / Math.sqrt(2.0).toFloat())
        tetrahedron[2] = Vector3f(0.0f, 1.0f, 1.0f / Math.sqrt(2.0).toFloat())
        tetrahedron[3] = Vector3f(0.0f, -1.0f, 1.0f / Math.sqrt(2.0).toFloat())
        lights = ArrayList()
        for (i in 0..3) { // TODO allow # initial lights to be customizable?
            val light = PointLight(150.0f)
            light.position = tetrahedron[i]!!.mul(25.0f)
            light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            light.intensity = 1.0f
            lights!!.add(light)
            //camera.addChild( light );
            scene.addChild(light)
        }

        // Make a headlight for the camera
        headlight = PointLight(150.0f)
        headlight!!.position = Vector3f(0f, 0f, -1f).mul(25.0f)
        headlight!!.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
        headlight!!.intensity = 0.5f
        headlight!!.name = "headlight"
        val lightSphere = Icosphere(1.0f, 2)
        headlight!!.addChild(lightSphere)
        lightSphere.material.diffuse = headlight!!.emissionColor
        lightSphere.material.specular = headlight!!.emissionColor
        lightSphere.material.ambient = headlight!!.emissionColor
        lightSphere.material.wireframe = true
        lightSphere.visible = false
        //lights.add( light );
        camera!!.nearPlaneDistance = 0.01f
        camera!!.farPlaneDistance = 1000.0f
        camera!!.addChild(headlight!!)
        floor = InfinitePlane() //new Box( new Vector3f( 500f, 0.2f, 500f ) );
        (floor as InfinitePlane).type = InfinitePlane.Type.Grid
        (floor as Node).name = "Floor"
        scene.addChild(floor as Node)
    }

    /**
     * Initialization of SWING and scenery. Also triggers an initial population of lights/camera in the scene
     */
    override fun init() {

        // Darcula dependency went missing from maven repo, factor it out
//        if(Boolean.parseBoolean(System.getProperty("sciview.useDarcula", "false"))) {
//            try {
//                BasicLookAndFeel darcula = new DarculaLaf();
//                UIManager.setLookAndFeel(darcula);
//            } catch (Exception e) {
//                getLogger().info("Could not load Darcula Look and Feel");
//            }
//        }
        val logLevel = System.getProperty("scenery.LogLevel", "info")
        log!!.level = LogLevel.value(logLevel)
        LogbackUtils.setLogLevel(null, logLevel)
        System.getProperties().stringPropertyNames().forEach(Consumer { name: String ->
            if (name.startsWith("scenery.LogLevel")) {
                LogbackUtils.setLogLevel("", System.getProperty(name, "info"))
            }
        })

        // determine imagej-launcher version and to disable Vulkan if XInitThreads() fix
        // is not deployed
        try {
            val launcherClass = Class.forName("net.imagej.launcher.ClassLauncher")
            var versionString = VersionUtils.getVersion(launcherClass)
            if (versionString != null && getPlatform() == ExtractsNatives.Platform.LINUX) {
                versionString = versionString.substring(0, 5)
                val launcherVersion = Version(versionString)
                val nonWorkingVersion = Version("4.0.5")
                if (launcherVersion.compareTo(nonWorkingVersion) <= 0
                        && !java.lang.Boolean.parseBoolean(System.getProperty("sciview.DisableLauncherVersionCheck", "false"))) {
                    logger.info("imagej-launcher version smaller or equal to non-working version ($versionString vs. 4.0.5), disabling Vulkan as rendering backend. Disable check by setting 'scenery.DisableLauncherVersionCheck' system property to 'true'.")
                    System.setProperty("scenery.Renderer", "OpenGLRenderer")
                } else {
                    logger.info("imagej-launcher version bigger that non-working version ($versionString vs. 4.0.5), all good.")
                }
            }
        } catch (cnfe: ClassNotFoundException) {
            // Didn't find the launcher, so we're probably good.
            logger.info("imagej-launcher not found, not touching renderer preferences.")
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
            x = screenSize.width / 2 - windowWidth / 2
            y = screenSize.height / 2 - windowHeight / 2
        } catch (e: HeadlessException) {
            x = 10
            y = 10
        }
        frame = JFrame("SciView")
        frame!!.layout = BorderLayout(0, 0)
        frame!!.setSize(windowWidth, windowHeight)
        frame!!.setLocation(x, y)
        frame!!.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        nodePropertyEditor = NodePropertyEditor(this)
        val p = JPanel(BorderLayout(0, 0))
        sceneryJPanel = SceneryJPanel()
        JPopupMenu.setDefaultLightWeightPopupEnabled(false)
        val swingMenuBar = JMenuBar()
        SwingJMenuBarCreator().createMenus(menus!!.getMenu("SciView"), swingMenuBar)
        frame!!.jMenuBar = swingMenuBar
        p.layout = OverlayLayout(p)
        p.background = Color(50, 48, 47)
        p.add(sceneryJPanel, BorderLayout.CENTER)
        sceneryJPanel!!.isVisible = true
        nodePropertyEditor!!.component // Initialize node property panel
        val inspectorTree = nodePropertyEditor!!.tree
        inspectorTree.toggleClickCount = 0 // This disables expanding menus on double click
        val inspectorProperties = nodePropertyEditor!!.props
        val tp = JBEditorTabs(null)
        tp.tabsPosition = JBTabsPosition.right
        tp.isSideComponentVertical = true
        inspector = JSplitPane(JSplitPane.VERTICAL_SPLIT,  //
                JScrollPane(inspectorTree),
                JScrollPane(inspectorProperties))
        inspector!!.dividerLocation = windowHeight / 3
        inspector!!.isContinuousLayout = true
        inspector!!.border = BorderFactory.createEmptyBorder()
        inspector!!.dividerSize = 1
        val inspectorIcon = getScaledImageIcon(this.javaClass.getResource("toolbox.png"), 16, 16)
        val tiInspector = TabInfo(inspector, inspectorIcon)
        tiInspector.text = ""
        tp.addTab(tiInspector)

        // We need to get the surface scale here before initialising scenery's renderer, as
        // the information is needed already at initialisation time.
        val dt = frame!!.graphicsConfiguration.defaultTransform
        val surfaceScale = Vector2f(dt.scaleX.toFloat(), dt.scaleY.toFloat())
        settings.set("Renderer.SurfaceScale", surfaceScale)
        interpreterPane = REPLPane(scijavaContext)
        interpreterPane!!.component.border = BorderFactory.createEmptyBorder()
        val interpreterIcon = getScaledImageIcon(this.javaClass.getResource("terminal.png"), 16, 16)
        val tiREPL = TabInfo(interpreterPane!!.component, interpreterIcon)
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
        mainSplitPane!!.dividerLocation = frame!!.size.width - 36
        mainSplitPane!!.border = BorderFactory.createEmptyBorder()
        mainSplitPane!!.dividerSize = 1
        mainSplitPane!!.resizeWeight = 0.9
        sidebarHidden = true

        //frame.add(mainSplitPane, BorderLayout.CENTER);
        frame!!.add(mainSplitPane, BorderLayout.CENTER)
        val sciView = this
        frame!!.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                logger.debug("Closing SciView window.")
                close()
                scijavaContext!!.service(SciViewService::class.java).close(sciView)
                isClosed = true
            }
        })
        splashLabel = SplashLabel()
        frame!!.glassPane = splashLabel
        frame!!.glassPane.isVisible = true
        frame!!.glassPane.requestFocusInWindow()
        //            frame.getGlassPane().setBackground(new java.awt.Color(50, 48, 47, 255));
        frame!!.isVisible = true
        sceneryPanel[0] = sceneryJPanel
        renderer = Renderer.createRenderer(hub, applicationName, scene,
                windowWidth, windowHeight,
                sceneryPanel[0])
        hub.add(SceneryElement.Renderer, renderer!!)
        reset()
        animations = LinkedList()
        controlStack = Stack()
        SwingUtilities.invokeLater {
            try {
                while (!getSceneryRenderer()!!.firstImageReady) {
                    logger.debug("Waiting for renderer initialisation")
                    Thread.sleep(300)
                }
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                logger.error("Renderer construction interrupted.")
            }
            nodePropertyEditor!!.rebuildTree()
            logger.info("Done initializing SciView")

            // subscribe to Node{Added, Removed, Changed} events, happens automatically
//            eventService!!.subscribe(this)
            frame!!.glassPane.isVisible = false
            sceneryJPanel!!.isVisible = true

            // install hook to keep inspector updated on external changes (scripting, etc)
            scene.onNodePropertiesChanged["updateInspector"] = { node: Node ->
                if (node === nodePropertyEditor!!.currentNode) {
                    nodePropertyEditor!!.updateProperties(node)
                }
            }

            // Enable push rendering by default
            renderer!!.pushMode = true
            sciView.camera!!.setPosition(1.65, 1)
        }
    }

    private var sidebarHidden = false
    private var previousSidebarPosition = 0
    fun toggleSidebar(): Boolean {
        if (!sidebarHidden) {
            previousSidebarPosition = mainSplitPane!!.dividerLocation
            // TODO: remove hard-coded tab width
            mainSplitPane!!.dividerLocation = frame!!.size.width - 36
            sidebarHidden = true
        } else {
            if (previousSidebarPosition == 0) {
                previousSidebarPosition = windowWidth / 3 * 2
            }
            mainSplitPane!!.dividerLocation = previousSidebarPosition
            sidebarHidden = false
        }
        return sidebarHidden
    }

    private fun getScaledImageIcon(resource: URL, width: Int, height: Int): ImageIcon {
        val first = ImageIcon(resource)
        val resizedImg = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g2 = resizedImg.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(first.image, 0, 0, width, height, null)
        g2.dispose()
        return ImageIcon(resizedImg)
    }

    private fun initializeInterpreter() {
        val startupCode = Scanner(SciView::class.java.getResourceAsStream("startup.py"), "UTF-8").useDelimiter("\\A").next()
        interpreterPane!!.repl.interpreter.bindings["sciView"] = this
        try {
            interpreterPane!!.repl.interpreter.eval(startupCode)
        } catch (e: ScriptException) {
            e.printStackTrace()
        }
    }

    /*
     * Completely close the SciView window + cleanup
     */
    fun closeWindow() {
        frame!!.dispose()
    }

    /*
     * Return true if the scene has been initialized
     */
    val isInitialized: Boolean
        get() = sceneInitialized()

    /**
     * Place the scene into the center of camera view, and zoom in/out such
     * that the whole scene is in the view (everything would be visible if it
     * would not be potentially occluded).
     */
    fun fitCameraToScene() {
        centerOnNode(scene)
        //TODO: smooth zoom in/out VLADO vlado Vlado
    }

    /**
     * Place the scene into the center of camera view.
     */
    fun centerOnScene() {
        centerOnNode(scene)
    }
    /*
     * Get the InputHandler that is managing mouse, input, VR controls, etc.
     */
    val sceneryInputHandler: InputHandler
        get() = inputHandler!!

    /*
     * Return a bounding box around a subgraph of the scenegraph
     */
    fun getSubgraphBoundingBox(n: Node): OrientedBoundingBox? {
        val predicate = Function<Node, List<Node>> { node: Node -> node.children }
        return getSubgraphBoundingBox(n, predicate)
    }

    /*
     * Return a bounding box around a subgraph of the scenegraph
     */
    fun getSubgraphBoundingBox(n: Node, branchFunction: Function<Node, List<Node>>): OrientedBoundingBox? {
        if (n.boundingBox == null && n.children.size != 0) {
            return n.getMaximumBoundingBox().asWorld()
        }
        val branches = branchFunction.apply(n)
        if (branches.size == 0) {
            return if (n.boundingBox == null) null else n.boundingBox!!.asWorld()
        }
        var bb = n.getMaximumBoundingBox()
        for (c in branches) {
            val cBB = getSubgraphBoundingBox(c, branchFunction)
            if (cBB != null) bb = bb.expand(bb, cBB)
        }
        return bb
    }

    /**
     * Place the active node into the center of camera view.
     */
    fun centerOnActiveNode() {
        if (activeNode == null) return
        centerOnNode(activeNode)
    }

    /**
     * Place the specified node into the center of camera view.
     */
    fun centerOnNode(currentNode: Node?) {
        if (currentNode == null) {
            log!!.info("Cannot center on null node.")
            return
        }

        //center the on the same spot as ArcBall does
        centerOnPosition(currentNode.getMaximumBoundingBox().getBoundingSphere().origin)
    }

    /**
     * Center the camera on the specified Node
     */
    fun centerOnPosition(currentPos: Vector3f?) {
        //desired view direction in world coords
        val worldDirVec = Vector3f(currentPos).sub(camera!!.position)
        if (worldDirVec.lengthSquared() < 0.01) {
            //ill defined task, happens typically when cam is inside the node which we want center on
            log!!.info("Camera is on the spot you want to look at. Please, move the camera away first.")
            return
        }
        val camForwardXZ = Vector2f(camera!!.forward.x, camera!!.forward.z)
        val wantLookAtXZ = Vector2f(worldDirVec.x, worldDirVec.z)
        var totalYawAng = camForwardXZ.normalize().dot(wantLookAtXZ.normalize()).toDouble()
        //while mathematically impossible, cumulated numerical inaccuracies have different opinion
        totalYawAng = if (totalYawAng > 1) {
            0.0
        } else {
            acos(totalYawAng)
        }

        //switch direction?
        camForwardXZ[camForwardXZ.y] = -camForwardXZ.x
        if (wantLookAtXZ.dot(camForwardXZ) > 0) totalYawAng *= -1.0
        val camForwardYed = Vector3f(camera!!.forward)
        Quaternionf().rotateXYZ(0f, (-totalYawAng).toFloat(), 0f).normalize().transform(camForwardYed)
        var totalPitchAng = camForwardYed.normalize().dot(worldDirVec.normalize()).toDouble()
        totalPitchAng = if (totalPitchAng > 1) {
            0.0
        } else {
            acos(totalPitchAng)
        }

        //switch direction?
        if (camera!!.forward.y > worldDirVec.y) totalPitchAng *= -1.0
        if (camera!!.up.y < 0) totalPitchAng *= -1.0

        //animation options: control delay between animation frames -- fluency
        val rotPausePerStep: Long = 30 //miliseconds

        //animation options: control max number of steps -- upper limit on total time for animation
        val rotMaxSteps = 999999 //effectively disabled....

        //animation options: the hardcoded 5 deg (0.087 rad) -- smoothness
        //how many steps when max update/move is 5 deg
        val totalDeltaAng = Math.max(Math.abs(totalPitchAng), Math.abs(totalYawAng)).toFloat()
        var rotSteps = Math.ceil(totalDeltaAng / 0.087).toInt()
        if (rotSteps > rotMaxSteps) rotSteps = rotMaxSteps

        /*
        log.info("centering over "+rotSteps+" steps the pitch " + 180*totalPitchAng/Math.PI
                + " and the yaw " + 180*totalYawAng/Math.PI);
        */

        //angular progress aux variables
        var donePitchAng = 0.0
        var doneYawAng = 0.0
        var deltaAng: Float
        camera!!.targeted = false
        var i = 1
        while (i <= rotSteps) {

            //this emulates ease-in ease-out animation, both vars are in [0:1]
            var timeProgress = i.toFloat() / rotSteps
            val angProgress = (if (2.let { timeProgress *= it; timeProgress } <= 1) //two cubics connected smoothly into S-shape curve from [0,0] to [1,1]
                timeProgress * timeProgress * timeProgress else 2.let { timeProgress -= it; timeProgress } * timeProgress * timeProgress + 2) / 2

            //rotate now by this ang: "where should I be by now" minus "where I got last time"
            deltaAng = (angProgress * totalPitchAng - donePitchAng).toFloat()
            val pitchQ = Quaternionf().rotateXYZ(-deltaAng, 0f, 0f).normalize()
            deltaAng = (angProgress * totalYawAng - doneYawAng).toFloat()
            val yawQ = Quaternionf().rotateXYZ(0f, deltaAng, 0f).normalize()
            camera!!.rotation = pitchQ.mul(camera!!.rotation).mul(yawQ).normalize()
            donePitchAng = angProgress * totalPitchAng
            doneYawAng = angProgress * totalYawAng
            try {
                Thread.sleep(rotPausePerStep)
            } catch (e: InterruptedException) {
                i = rotSteps
            }
            ++i
        }
    }

    /**
     * Activate the node, and center the view on it.
     * @param n
     * @return the currently active node
     */
    fun setActiveCenteredNode(n: Node?): Node? {
        //activate...
        val ret = setActiveNode(n)
        //...and center it
        ret?.let { centerOnNode(it) }
        return ret
    }

    //a couple of shortcut methods to readout controls params
    fun getFPSSpeedSlow(): Float {
        return controlsParameters.getFpsSpeedSlow()
    }

    fun getFPSSpeedFast(): Float {
        return controlsParameters.getFpsSpeedFast()
    }

    fun getFPSSpeedVeryFast(): Float {
        return controlsParameters.getFpsSpeedVeryFast()
    }

    fun getMouseSpeed(): Float {
        return controlsParameters.getMouseSpeedMult()
    }

    fun getMouseScrollSpeed(): Float {
        return controlsParameters.getMouseScrollMult()
    }

    //a couple of setters with scene sensible boundary checks
    fun setFPSSpeedSlow(slowSpeed: Float) {
        controlsParameters.setFpsSpeedSlow(paramWithinBounds(slowSpeed, FPSSPEED_MINBOUND_SLOW, FPSSPEED_MAXBOUND_SLOW))
    }

    fun setFPSSpeedFast(fastSpeed: Float) {
        controlsParameters.setFpsSpeedFast(paramWithinBounds(fastSpeed, FPSSPEED_MINBOUND_FAST, FPSSPEED_MAXBOUND_FAST))
    }

    fun setFPSSpeedVeryFast(veryFastSpeed: Float) {
        controlsParameters.setFpsSpeedVeryFast(paramWithinBounds(veryFastSpeed, FPSSPEED_MINBOUND_VERYFAST, FPSSPEED_MAXBOUND_VERYFAST))
    }

    fun setFPSSpeed(newBaseSpeed: Float) {
        // we don't want to escape bounds checking
        // (so we call "our" methods rather than directly the controlsParameters)
        setFPSSpeedSlow(1f * newBaseSpeed)
        setFPSSpeedFast(20f * newBaseSpeed)
        setFPSSpeedVeryFast(500f * newBaseSpeed)

        //report what's been set in the end
        log!!.debug("FPS speeds: slow=" + controlsParameters.getFpsSpeedSlow()
                .toString() + ", fast=" + controlsParameters.getFpsSpeedFast()
                .toString() + ", very fast=" + controlsParameters.getFpsSpeedVeryFast())
    }

    fun setMouseSpeed(newSpeed: Float) {
        controlsParameters.setMouseSpeedMult(paramWithinBounds(newSpeed, MOUSESPEED_MINBOUND, MOUSESPEED_MAXBOUND))
        log!!.debug("Mouse movement speed: " + controlsParameters.getMouseSpeedMult())
    }

    fun setMouseScrollSpeed(newSpeed: Float) {
        controlsParameters.setMouseScrollMult(paramWithinBounds(newSpeed, MOUSESCROLL_MINBOUND, MOUSESCROLL_MAXBOUND))
        log!!.debug("Mouse scroll speed: " + controlsParameters.getMouseScrollMult())
    }

    // TODO replace with coerce?
    private fun paramWithinBounds(param: Float, minBound: Float, maxBound: Float): Float {
        var newParam = param
        if (newParam < minBound) newParam = minBound else if (newParam > maxBound) newParam = maxBound
        return newParam
    }


    fun setObjectSelectionMode() {
        val selectAction = { nearest: RaycastResult, x: Int, y: Int ->
            if (!nearest.matches.isEmpty()) {
                // copy reference on the last object picking result into "public domain"
                // (this must happen before the ContextPopUpNodeChooser menu!)
                objectSelectionLastResult = nearest

                // Setup the context menu for this picking
                // (in the menu, the user will chose node herself)
                ContextPopUpNodeChooser(this).show(sceneryJPanel, x, y)
            }
        }
        setObjectSelectionMode(selectAction)
    }

    var objectSelectionLastResult: RaycastResult? = null

    /*
     * Set the action used during object selection
     */
    fun setObjectSelectionMode(selectAction: Function3<RaycastResult, Int, Int, Unit>?) {
        val h = inputHandler
        val ignoredObjects = ArrayList<Class<*>>()
        ignoredObjects.add(BoundingGrid::class.java)
        ignoredObjects.add(Camera::class.java) //do not mess with "scene params", allow only "scene data" to be selected
        ignoredObjects.add(DetachedHeadCamera::class.java)
        ignoredObjects.add(DirectionalLight::class.java)
        ignoredObjects.add(PointLight::class.java)
        if (h == null) {
            logger.error("InputHandler is null, cannot change object selection mode.")
            return
        }
        h.addBehaviour("node: choose one from the view panel",
                SelectCommand("objectSelector", renderer!!, scene,
                        { scene.findObserver() }, false, ignoredObjects,
                        selectAction!!))
        h.addKeyBinding("node: choose one from the view panel", "double-click button1")
    }

    /*
     * Initial configuration of the scenery InputHandler
     * This is automatically called and should not be used directly
     */
    override fun inputSetup() {
        val h = inputHandler
        if (h == null) {
            logger.error("InputHandler is null, cannot run input setup.")
            return
        }
        //when we get here, the Behaviours and key bindings from scenery are already in place

        //possibly, disable some (unused?) controls from scenery
        /*
        h.removeBehaviour( "gamepad_camera_control");
        h.removeKeyBinding("gamepad_camera_control");
        h.removeBehaviour( "gamepad_movement_control");
        h.removeKeyBinding("gamepad_movement_control");
        */

        // node-selection and node-manipulation (translate & rotate) controls
        setObjectSelectionMode()
        val nodeTranslateControl = NodeTranslateControl(this)
        h.addBehaviour("node: move selected one left, right, up, or down", nodeTranslateControl)
        h.addKeyBinding("node: move selected one left, right, up, or down", "ctrl button1")
        h.addBehaviour("node: move selected one closer or further away", nodeTranslateControl)
        h.addKeyBinding("node: move selected one closer or further away", "ctrl scroll")
        h.addBehaviour("node: rotate selected one", NodeRotateControl(this))
        h.addKeyBinding("node: rotate selected one", "ctrl shift button1")

        // within-scene navigation: ArcBall and FPS
        enableArcBallControl()
        enableFPSControl()

        // whole-scene rolling
        h.addBehaviour("view: rotate (roll) clock-wise", SceneRollControl(this, +0.05f)) //2.8 deg
        h.addKeyBinding("view: rotate (roll) clock-wise", "R")
        h.addBehaviour("view: rotate (roll) counter clock-wise", SceneRollControl(this, -0.05f))
        h.addKeyBinding("view: rotate (roll) counter clock-wise", "shift R")
        h.addBehaviour("view: rotate (roll) with mouse", h.getBehaviour("view: rotate (roll) clock-wise")!!)
        h.addKeyBinding("view: rotate (roll) with mouse", "ctrl button3")

        // adjusters of various controls sensitivities
        h.addBehaviour("moves: step size decrease", ClickBehaviour { _: Int, _: Int -> setFPSSpeed(getFPSSpeedSlow() - 0.01f) })
        h.addKeyBinding("moves: step size decrease", "MINUS")
        h.addBehaviour("moves: step size increase", ClickBehaviour { _: Int, _: Int -> setFPSSpeed(getFPSSpeedSlow() + 0.01f) })
        h.addKeyBinding("moves: step size increase", "EQUALS")
        h.addBehaviour("mouse: move sensitivity decrease", ClickBehaviour { _: Int, _: Int -> setMouseSpeed(getMouseSpeed() - 0.02f) })
        h.addKeyBinding("mouse: move sensitivity decrease", "M MINUS")
        h.addBehaviour("mouse: move sensitivity increase", ClickBehaviour { _: Int, _: Int -> setMouseSpeed(getMouseSpeed() + 0.02f) })
        h.addKeyBinding("mouse: move sensitivity increase", "M EQUALS")
        h.addBehaviour("mouse: scroll sensitivity decrease", ClickBehaviour { _: Int, _: Int -> setMouseScrollSpeed(getMouseScrollSpeed() - 0.3f) })
        h.addKeyBinding("mouse: scroll sensitivity decrease", "S MINUS")
        h.addBehaviour("mouse: scroll sensitivity increase", ClickBehaviour { _: Int, _: Int -> setMouseScrollSpeed(getMouseScrollSpeed() + 0.3f) })
        h.addKeyBinding("mouse: scroll sensitivity increase", "S EQUALS")

        // help window
        h.addBehaviour("show help", showHelpDisplay())
        h.addKeyBinding("show help", "F1")
    }

    /*
     * Change the control mode to circle around the active object in an arcball
     */
    private fun enableArcBallControl() {
        val h = inputHandler
        if (h == null) {
            logger.error("InputHandler is null, cannot setup arcball control.")
            return
        }

        val target: Vector3f = activeNode?.position ?: Vector3f(0.0f, 0.0f, 0.0f)

        //setup ArcballCameraControl from scenery, register it with SciView's controlsParameters
        val cameraSupplier = Supplier { scene.findObserver() }
        targetArcball = AnimatedCenteringBeforeArcBallControl("view: rotate it around selected node", cameraSupplier,
                renderer!!.window.width,
                renderer!!.window.height, target)
        targetArcball.maximumDistance = Float.MAX_VALUE
        controlsParameters.registerArcballCameraControl(targetArcball)
        h.addBehaviour("view: rotate around selected node", targetArcball)
        h.addKeyBinding("view: rotate around selected node", "shift button1")
        h.addBehaviour("view: zoom outward or toward selected node", targetArcball)
        h.addKeyBinding("view: zoom outward or toward selected node", "shift scroll")
    }

    /*
     * A wrapping class for the {@ArcballCameraControl} that calls {@link CenterOnPosition()}
     * before the actual Arcball camera movement takes place. This way, the targeted node is
     * first smoothly brought into the centre along which Arcball is revolving, preventing
     * from sudden changes of view (and lost of focus from the user.
     */
    inner class AnimatedCenteringBeforeArcBallControl : ArcballCameraControl {
        //a bunch of necessary c'tors (originally defined in the ArcballCameraControl class)
        constructor(name: String, n: Function0<Camera?>, w: Int, h: Int, target: Function0<Vector3f>) : super(name, n, w, h, target) {}
        constructor(name: String,n: Supplier<Camera?>, w: Int, h: Int, target: Supplier<Vector3f>) : super(name, n, w, h, target) {}
        constructor(name: String, n: Function0<Camera?>, w: Int, h: Int, target: Vector3f) : super(name, n, w, h, target) {}
        constructor(name: String, n: Supplier<Camera?>, w: Int, h: Int, target: Vector3f) : super(name, n, w, h, target) {}

        override fun init(x: Int, y: Int) {
            centerOnPosition(targetArcball.target.invoke())
            super.init(x, y)
        }

        override fun scroll(wheelRotation: Double, isHorizontal: Boolean, x: Int, y: Int) {
            centerOnPosition(targetArcball.target.invoke())
            super.scroll(wheelRotation, isHorizontal, x, y)
        }
    }

    /*
     * Enable FPS style controls
     */
    private fun enableFPSControl() {
        val h = inputHandler
        if (h == null) {
            logger.error("InputHandler is null, cannot setup fps control.")
            return
        }

        // Mouse look around (Lclick) and move around (Rclick)
        //
        //setup FPSCameraControl from scenery, register it with SciView's controlsParameters
        val cameraSupplier = Supplier { scene.findObserver() }
        fpsControl = FPSCameraControl("view: freely look around", cameraSupplier, renderer!!.window.width,
                renderer!!.window.height)
        controlsParameters.registerFpsCameraControl(fpsControl)
        h.addBehaviour("view: freely look around", fpsControl!!)
        h.addKeyBinding("view: freely look around", "button1")

        //slow and fast camera motion
        h.addBehaviour("move_withMouse_back/forward/left/right", CameraTranslateControl(this, 1f))
        h.addKeyBinding("move_withMouse_back/forward/left/right", "button3")
        //
        //fast and very fast camera motion
        h.addBehaviour("move_withMouse_back/forward/left/right_fast", CameraTranslateControl(this, 10f))
        h.addKeyBinding("move_withMouse_back/forward/left/right_fast", "shift button3")

        // Keyboard move around (WASD keys)
        //
        //override 'WASD' from Scenery
        var mcW: MovementCommand
        var mcA: MovementCommand
        var mcS: MovementCommand
        var mcD: MovementCommand
        mcW = MovementCommand("move_forward", "forward", { scene.findObserver() }, controlsParameters.getFpsSpeedSlow())
        mcS = MovementCommand("move_backward", "back", { scene.findObserver() }, controlsParameters.getFpsSpeedSlow())
        mcA = MovementCommand("move_left", "left", { scene.findObserver() }, controlsParameters.getFpsSpeedSlow())
        mcD = MovementCommand("move_right", "right", { scene.findObserver() }, controlsParameters.getFpsSpeedSlow())
        controlsParameters.registerSlowStepMover(mcW)
        controlsParameters.registerSlowStepMover(mcS)
        controlsParameters.registerSlowStepMover(mcA)
        controlsParameters.registerSlowStepMover(mcD)
        h.addBehaviour("move_forward", mcW)
        h.addBehaviour("move_back", mcS)
        h.addBehaviour("move_left", mcA)
        h.addBehaviour("move_right", mcD)
        // 'WASD' keys are registered already in scenery

        //override shift+'WASD' from Scenery
        mcW = MovementCommand("move_forward_fast", "forward", { scene.findObserver() }, controlsParameters.getFpsSpeedFast())
        mcS = MovementCommand("move_backward_fast", "back", { scene.findObserver() }, controlsParameters.getFpsSpeedFast())
        mcA = MovementCommand("move_left_fast", "left", { scene.findObserver() }, controlsParameters.getFpsSpeedFast())
        mcD = MovementCommand("move_right_fast", "right", { scene.findObserver() }, controlsParameters.getFpsSpeedFast())
        controlsParameters.registerFastStepMover(mcW)
        controlsParameters.registerFastStepMover(mcS)
        controlsParameters.registerFastStepMover(mcA)
        controlsParameters.registerFastStepMover(mcD)
        h.addBehaviour("move_forward_fast", mcW)
        h.addBehaviour("move_back_fast", mcS)
        h.addBehaviour("move_left_fast", mcA)
        h.addBehaviour("move_right_fast", mcD)
        // shift+'WASD' keys are registered already in scenery

        //define additionally shift+ctrl+'WASD'
        mcW = MovementCommand("move_forward_veryfast", "forward", { scene.findObserver() }, controlsParameters.getFpsSpeedVeryFast())
        mcS = MovementCommand("move_back_veryfast", "back", { scene.findObserver() }, controlsParameters.getFpsSpeedVeryFast())
        mcA = MovementCommand("move_left_veryfast", "left", { scene.findObserver() }, controlsParameters.getFpsSpeedVeryFast())
        mcD = MovementCommand("move_right_veryfast", "right", { scene.findObserver() }, controlsParameters.getFpsSpeedVeryFast())
        controlsParameters.registerVeryFastStepMover(mcW)
        controlsParameters.registerVeryFastStepMover(mcS)
        controlsParameters.registerVeryFastStepMover(mcA)
        controlsParameters.registerVeryFastStepMover(mcD)
        h.addBehaviour("move_forward_veryfast", mcW)
        h.addBehaviour("move_back_veryfast", mcS)
        h.addBehaviour("move_left_veryfast", mcA)
        h.addBehaviour("move_right_veryfast", mcD)
        h.addKeyBinding("move_forward_veryfast", "ctrl shift W")
        h.addKeyBinding("move_back_veryfast", "ctrl shift S")
        h.addKeyBinding("move_left_veryfast", "ctrl shift A")
        h.addKeyBinding("move_right_veryfast", "ctrl shift D")

        // Keyboard only move up/down (XC keys)
        //
        //[[ctrl]+shift]+'XC'
        mcW = MovementCommand("move_up", "up", { scene.findObserver() }, controlsParameters.getFpsSpeedSlow())
        mcS = MovementCommand("move_down", "down", { scene.findObserver() }, controlsParameters.getFpsSpeedSlow())
        controlsParameters.registerSlowStepMover(mcW)
        controlsParameters.registerSlowStepMover(mcS)
        h.addBehaviour("move_up", mcW)
        h.addBehaviour("move_down", mcS)
        h.addKeyBinding("move_up", "C")
        h.addKeyBinding("move_down", "X")
        mcW = MovementCommand("move_up_fast", "up", { scene.findObserver() }, controlsParameters.getFpsSpeedFast())
        mcS = MovementCommand("move_down_fast", "down", { scene.findObserver() }, controlsParameters.getFpsSpeedFast())
        controlsParameters.registerFastStepMover(mcW)
        controlsParameters.registerFastStepMover(mcS)
        h.addBehaviour("move_up_fast", mcW)
        h.addBehaviour("move_down_fast", mcS)
        h.addKeyBinding("move_up_fast", "shift C")
        h.addKeyBinding("move_down_fast", "shift X")
        mcW = MovementCommand("move_up_veryfast", "up", { scene.findObserver() }, controlsParameters.getFpsSpeedVeryFast())
        mcS = MovementCommand("move_down_veryfast", "down", { scene.findObserver() }, controlsParameters.getFpsSpeedVeryFast())
        controlsParameters.registerVeryFastStepMover(mcW)
        controlsParameters.registerVeryFastStepMover(mcS)
        h.addBehaviour("move_up_veryfast", mcW)
        h.addBehaviour("move_down_veryfast", mcS)
        h.addKeyBinding("move_up_veryfast", "ctrl shift C")
        h.addKeyBinding("move_down_veryfast", "ctrl shift X")
    }

    /**
     * Add a box at the specified position with specified size, color, and normals on the inside/outside
     * @param position position to put the box
     * @param size size of the box
     * @param color color of the box
     * @param inside are normals inside the box?
     * @return the Node corresponding to the box
     */
    @JvmOverloads
    fun addBox(position: Vector3f? = Vector3f(0.0f, 0.0f, 0.0f), size: Vector3f? = Vector3f(1.0f, 1.0f, 1.0f), color: ColorRGB? = DEFAULT_COLOR,
               inside: Boolean = false): Node? {
        // TODO: use a material from the current palate by default
        val boxmaterial = Material()
        boxmaterial.ambient = Vector3f(1.0f, 0.0f, 0.0f)
        boxmaterial.diffuse = Utils.convertToVector3f(color)
        boxmaterial.specular = Vector3f(1.0f, 1.0f, 1.0f)
        val box = size?.let { Box(it, inside) }
        box?.material = boxmaterial
        if (position != null) {
            box?.position = position
        }
        return addNode(box)
    }
    /**
     * Add a sphere at the specified positoin with a given radius and color
     * @param position position to put the sphere
     * @param radius radius the sphere
     * @param color color of the sphere
     * @return  the Node corresponding to the sphere
     */
    /**
     * Add a sphere at the specified position with a given radius
     * @param position position to put the sphere
     * @param radius radius of the sphere
     * @return the Node corresponding to the sphere
     */
    /**
     * Add a unit sphere at the origin
     * @return the Node corresponding to the sphere
     */
    @JvmOverloads
    fun addSphere(position: Vector3f? = Vector3f(0.0f, 0.0f, 0.0f), radius: Float = 1f, color: ColorRGB? = DEFAULT_COLOR): Node? {
        val material = Material()
        material.ambient = Vector3f(1.0f, 0.0f, 0.0f)
        material.diffuse = Utils.convertToVector3f(color)
        material.specular = Vector3f(1.0f, 1.0f, 1.0f)
        val sphere = Sphere(radius, 20)
        sphere.material = material
        if (position != null) {
            sphere.position = position
        }
        return addNode(sphere)
    }

    /**
     * Add a Cylinder at the given position with radius, height, and number of faces/segments
     * @param position position of the cylinder
     * @param radius radius of the cylinder
     * @param height height of the cylinder
     * @param num_segments number of segments to represent the cylinder
     * @return  the Node corresponding to the cylinder
     */
    fun addCylinder(position: Vector3f?, radius: Float, height: Float, num_segments: Int): Node? {
        val cyl = Cylinder(radius, height, num_segments)
        if (position != null) {
            cyl.position = position
        }
        return addNode(cyl)
    }

    /**
     * Add a Cone at the given position with radius, height, and number of faces/segments
     * @param position position to put the cone
     * @param radius radius of the cone
     * @param height height of the cone
     * @param num_segments number of segments used to represent cone
     * @return  the Node corresponding to the cone
     */
    fun addCone(position: Vector3f?, radius: Float, height: Float, num_segments: Int): Node? {
        val cone = Cone(radius, height, num_segments, Vector3f(0.0f, 0.0f, 1.0f))
        if (position != null) {
            cone.position = position
        }
        return addNode(cone)
    }

    /**
     * Add a line from start to stop with the given color
     * @param start start position of line
     * @param stop stop position of line
     * @param color color of line
     * @return the Node corresponding to the line
     */
    @JvmOverloads
    fun addLine(start: Vector3f = Vector3f(0.0f, 0.0f, 0.0f), stop: Vector3f = Vector3f(1.0f, 1.0f, 1.0f), color: ColorRGB? = DEFAULT_COLOR): Node? {
        return addLine(arrayOf(start, stop), color, 0.1)
    }

    /**
     * Add a multi-segment line that goes through the supplied points with a single color and edge width
     * @param points points along line including first and terminal points
     * @param color color of line
     * @param edgeWidth width of line segments
     * @return the Node corresponding to the line
     */
    fun addLine(points: Array<Vector3f>, color: ColorRGB?, edgeWidth: Double): Node? {
        val material = Material()
        material.ambient = Vector3f(1.0f, 1.0f, 1.0f)
        material.diffuse = Utils.convertToVector3f(color)
        material.specular = Vector3f(1.0f, 1.0f, 1.0f)
        val line = Line(points.size)
        for (pt in points) {
            line.addPoint(pt)
        }
        line.edgeWidth = edgeWidth.toFloat()
        line.material = material
        line.position = points[0]
        return addNode(line)
    }

    /**
     * Add a PointLight source at the origin
     * @return a Node corresponding to the PointLight
     */
    fun addPointLight(): Node? {
        val material = Material()
        material.ambient = Vector3f(1.0f, 0.0f, 0.0f)
        material.diffuse = Vector3f(0.0f, 1.0f, 0.0f)
        material.specular = Vector3f(1.0f, 1.0f, 1.0f)
        val light = PointLight(5.0f)
        light.material = material
        light.position = Vector3f(0.0f, 0.0f, 0.0f)
        lights!!.add(light)
        return addNode(light)
    }

    /**
     * Position all lights that were initialized by default around the scene in a circle at Y=0
     */
    fun surroundLighting() {
        val bb = getSubgraphBoundingBox(scene, notAbstractBranchingFunction)
        val (c, r) = bb!!.getBoundingSphere()
        // Choose a good y-position, then place lights around the cross-section through this plane
        val y = 0f
        for (k in lights!!.indices) {
            val light = lights!![k]
            val x = (c.x() + r * cos(if (k == 0) 0.0 else Math.PI * 2 * (k.toFloat() / lights!!.size.toFloat()))).toFloat()
            val z = (c.y() + r * sin(if (k == 0) 0.0 else Math.PI * 2 * (k.toFloat() / lights!!.size.toFloat()))).toFloat()
            light.lightRadius = 2 * r
            light.position = Vector3f(x, y, z)
        }
    }

    /**
     * Write a scenery mesh as an stl to the given file
     * @param filename filename of the stl
     * @param scMesh mesh to save
     */
    fun writeSCMesh(filename: String?, scMesh: graphics.scenery.Mesh) {
        val f = File(filename)
        val out: BufferedOutputStream
        try {
            out = BufferedOutputStream(FileOutputStream(f))
            out.write("solid STL generated by FIJI\n".toByteArray())
            val normalsFB = scMesh.normals
            val verticesFB = scMesh.vertices
            while (verticesFB.hasRemaining() && normalsFB.hasRemaining()) {
                out.write("""facet normal ${normalsFB.get()} ${normalsFB.get()} ${normalsFB.get()}
""".toByteArray())
                out.write("outer loop\n".toByteArray())
                for (v in 0..2) {
                    out.write("""vertex	${verticesFB.get()} ${verticesFB.get()} ${verticesFB.get()}
""".toByteArray())
                }
                out.write("endloop\n".toByteArray())
                out.write("endfacet\n".toByteArray())
            }
            out.write("endsolid vcg\n".toByteArray())
            out.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Return the default point size to use for point clouds
     * @return default point size used for point clouds
     */
    private val defaultPointSize: Float
        get() = 0.025f

    /**
     * Create an array of normal vectors from a set of vertices corresponding to triangles
     *
     * @param verts vertices to use for computing normals, assumed to be ordered as triangles
     * @return array of normals
     */
    fun makeNormalsFromVertices(verts: ArrayList<RealPoint>): FloatArray {
        val normals = FloatArray(verts.size) // div3 * 3coords
        var k = 0
        while (k < verts.size) {
            val v1 = Vector3f(verts[k].getFloatPosition(0),  //
                    verts[k].getFloatPosition(1),  //
                    verts[k].getFloatPosition(2))
            val v2 = Vector3f(verts[k + 1].getFloatPosition(0),
                    verts[k + 1].getFloatPosition(1),
                    verts[k + 1].getFloatPosition(2))
            val v3 = Vector3f(verts[k + 2].getFloatPosition(0),
                    verts[k + 2].getFloatPosition(1),
                    verts[k + 2].getFloatPosition(2))
            val a = v2.sub(v1)
            val b = v3.sub(v1)
            val n = a.cross(b).normalize()
            normals[k / 3] = n[0]
            normals[k / 3 + 1] = n[1]
            normals[k / 3 + 2] = n[2]
            k += 3
        }
        return normals
    }

    /**
     * Open a file specified by the source path. The file can be anything that SciView knows about: mesh, volume, point cloud
     * @param source string of a data source
     * @throws IOException
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class)
    fun open(source: String) {
        if (source.endsWith(".xml")) {
            addNode(fromXML(source, hub, VolumeViewerOptions()))
            return
        }
        val data = io!!.open(source)
        if (data is Mesh)
            addMesh(data)
        else if (data is graphics.scenery.Mesh)
            addMesh(data)
        else if (data is PointCloud)
            addPointCloud(data)
        else if (data is Dataset)
            addVolume(data)
        else if (data is RandomAccessibleInterval<*>)
            addVolume(data as RandomAccessibleInterval<RealType<*>>, source)
        else if (data is List<*>) {
            val list = data
            require(!list.isEmpty()) { "Data source '$source' appears empty." }
            val element = list[0]
            if (element is RealLocalizable) {
                // NB: For now, we assume all elements will be RealLocalizable.
                // Highly likely to be the case, barring antagonistic importers.
                val points = list as List<RealLocalizable>
                addPointCloud(points, source)
            } else {
                val type = if (element == null) "<null>" else element.javaClass.name
                throw IllegalArgumentException("Data source '" + source +  //
                        "' contains elements of unknown type '" + type + "'")
            }
        } else {
            val type = if (data == null) "<null>" else data.javaClass.name
            throw IllegalArgumentException("Data source '" + source +  //
                    "' contains data of unknown type '" + type + "'")
        }
    }

    /**
     * Add the given points to the scene as a PointCloud with a given name
     * @param points points to use in a PointCloud
     * @param name name of the PointCloud
     * @return
     */
    @JvmOverloads
    fun addPointCloud(points: Collection<RealLocalizable>,
                      name: String? = "PointCloud"): Node? {
        val flatVerts = FloatArray(points.size * 3)
        var k = 0
        for (point in points) {
            flatVerts[k * 3] = point.getFloatPosition(0)
            flatVerts[k * 3 + 1] = point.getFloatPosition(1)
            flatVerts[k * 3 + 2] = point.getFloatPosition(2)
            k++
        }
        val pointCloud = PointCloud(defaultPointSize, name!!)
        val material = Material()
        val vBuffer: FloatBuffer = BufferUtils.allocateFloat(flatVerts.size * 4)
        val nBuffer: FloatBuffer = BufferUtils.allocateFloat(0)
        vBuffer.put(flatVerts)
        vBuffer.flip()
        pointCloud.vertices = vBuffer
        pointCloud.normals = nBuffer
        pointCloud.indices = BufferUtils.allocateInt(0)
        pointCloud.setupPointCloud()
        material.ambient = Vector3f(1.0f, 1.0f, 1.0f)
        material.diffuse = Vector3f(1.0f, 1.0f, 1.0f)
        material.specular = Vector3f(1.0f, 1.0f, 1.0f)
        pointCloud.material = material
        pointCloud.position = Vector3f(0f, 0f, 0f)
        return addNode(pointCloud)
    }

    /**
     * Add a PointCloud to the scene
     * @param pointCloud existing PointCloud to add to scene
     * @return a Node corresponding to the PointCloud
     */
    fun addPointCloud(pointCloud: PointCloud): Node? {
        pointCloud.setupPointCloud()
        pointCloud.material.ambient = Vector3f(1.0f, 1.0f, 1.0f)
        pointCloud.material.diffuse = Vector3f(1.0f, 1.0f, 1.0f)
        pointCloud.material.specular = Vector3f(1.0f, 1.0f, 1.0f)
        pointCloud.position = Vector3f(0f, 0f, 0f)
        return addNode(pointCloud)
    }

    /**
     * Add Node n to the scene and set it as the active node/publish it to the event service if activePublish is true
     * @param n node to add to scene
     * @param activePublish flag to specify whether the node becomes active *and* is published in the inspector/services
     * @return a Node corresponding to the Node
     */
    @JvmOverloads
    fun addNode(n: Node?, activePublish: Boolean = true): Node? {
        n?.let {
            scene.addChild(it)
            objectService?.addObject(n)
            if (blockOnNewNodes) {
                blockWhile({ sciView: SciView -> sciView.find(n.name) == null }, 20)
                //System.out.println("find(name) " + find(n.getName()) );
            }
            // Set new node as active and centered?
            setActiveNode(n);
            if (centerOnNewNodes) {
                centerOnNode(n)
            }
            if (activePublish) {
                eventService?.publish(NodeAddedEvent(n));
            }
        }
        return n;
    }

    /**
     * Add a scenery Mesh to the scene
     * @param scMesh scenery mesh to add to scene
     * @return a Node corresponding to the mesh
     */
    fun addMesh(scMesh: graphics.scenery.Mesh): Node? {
        val material = Material()
        material.ambient = Vector3f(1.0f, 0.0f, 0.0f)
        material.diffuse = Vector3f(0.0f, 1.0f, 0.0f)
        material.specular = Vector3f(1.0f, 1.0f, 1.0f)
        scMesh.material = material
        scMesh.position = Vector3f(0.0f, 0.0f, 0.0f)
        objectService?.addObject(scMesh)
        return addNode(scMesh)
    }

    /**
     * Add an ImageJ mesh to the scene
     * @param mesh net.imagej.mesh to add to scene
     * @return a Node corresponding to the mesh
     */
    fun addMesh(mesh: Mesh): Node? {
        val scMesh = MeshConverter.toScenery(mesh)
        return addMesh(scMesh)
    }

    /**
     * [Deprecated: use deleteNode]
     * Remove a Mesh from the scene
     * @param scMesh mesh to remove from scene
     */
    fun removeMesh(scMesh: graphics.scenery.Mesh?) {
        scene.removeChild(scMesh!!)
    }

    /**
     * Set the currently active node
     * @param n existing node that should become active focus of this SciView
     * @return the currently active node
     */
    fun setActiveNode(n: Node?): Node? {
        if (activeNode === n) return activeNode
        activeNode = n
        targetArcball.target = { n?.getMaximumBoundingBox()?.getBoundingSphere()?.origin ?: Vector3f(0.0f, 0.0f, 0.0f) }
        nodePropertyEditor?.trySelectNode(activeNode);
        eventService!!.publish(NodeActivatedEvent(activeNode))
        return activeNode
    }

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeAdded(event: NodeAddedEvent?) {
        nodePropertyEditor!!.rebuildTree()
    }

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeRemoved(event: NodeRemovedEvent?) {
        nodePropertyEditor!!.rebuildTree()
    }

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeChanged(event: NodeChangedEvent?) {
        nodePropertyEditor!!.rebuildTree()
    }

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeActivated(event: NodeActivatedEvent?) {
        // TODO: add listener code for node activation, if necessary
        // NOTE: do not update property window here, this will lead to a loop.
    }

    fun toggleInspectorWindow() {
        toggleSidebar()
    }

    @Suppress("UNUSED_PARAMETER")
    fun setInspectorWindowVisibility(visible: Boolean) {
//        inspector.setVisible(visible);
//        if( visible )
//            mainSplitPane.setDividerLocation(getWindowWidth()/4 * 3);
//        else
//            mainSplitPane.setDividerLocation(getWindowWidth());
    }

    @Suppress("UNUSED_PARAMETER")
    fun setInterpreterWindowVisibility(visible: Boolean) {
//        interpreterPane.getComponent().setVisible(visible);
//        if( visible )
//            interpreterSplitPane.setDividerLocation(getWindowHeight()/10 * 6);
//        else
//            interpreterSplitPane.setDividerLocation(getWindowHeight());
    }

    /**
     * Create an animation thread with the given fps speed and the specified action
     * @param fps frames per second at which this action should be run
     * @param action Runnable that contains code to run fps times per second
     * @return a Future corresponding to the thread
     */
    @Synchronized
    fun animate(fps: Int, action: Runnable): Future<*> {
        // TODO: Make animation speed less laggy and more accurate.
        val delay = 1000 / fps
        val thread = threadService!!.run {
            while (animating) {
                action.run()
                try {
                    Thread.sleep(delay.toLong())
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        animations!!.add(thread)
        animating = true
        return thread
    }

    /**
     * Stop all animations
     */
    @Synchronized
    fun stopAnimation() {
        animating = false
        while (!animations!!.isEmpty()) {
            animations!!.peek().cancel(true)
            animations!!.remove()
        }
    }

    /**
     * Take a screenshot and save it to the default scenery location
     */
    fun takeScreenshot() {
        renderer!!.screenshot()
    }

    /**
     * Take a screenshot and save it to the specified path
     * @param path path for saving the screenshot
     */
    fun takeScreenshot(path: String?) {
        renderer!!.screenshot(path!!, false)
    }

    /**
     * Take a screenshot and return it as an Img
     * @return an Img of type UnsignedByteType
     */
    val screenshot: Img<UnsignedByteType?>?
        get() {
            val screenshot = getSceneryRenderer()!!.requestScreenshot()
            val image = BufferedImage(screenshot.width, screenshot.height, BufferedImage.TYPE_4BYTE_ABGR)
            val imgData = (image.raster.dataBuffer as DataBufferByte).data
            System.arraycopy(screenshot.data, 0, imgData, 0, screenshot.data!!.size)
            var img: Img<UnsignedByteType?>? = null
            try {
                val tmpFile = File.createTempFile("sciview-", "-tmp.png")
                ImageIO.write(image, "png", tmpFile)
                @Suppress("UNCHECKED_CAST")
                img = io!!.open(tmpFile.absolutePath) as Img<UnsignedByteType?>
                tmpFile.delete()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return img
        }

    /**
     * Take a screenshot and return it as an Img
     * @return an Img of type UnsignedByteType
     */
    val aRGBScreenshot: Img<ARGBType>
        get() {
            val screenshot = screenshot
            return Utils.convertToARGB(screenshot)
        }

    /**
     * @param name The name of the node to find.
     * @return the node object or null, if the node has not been found.
     */
    fun find(name: String): Node? {
        val n = scene.find(name)
        if (n == null) {
            logger.warn("Node with name $name not found.")
        }
        return n
    }

    /**
     * @return an array of all nodes in the scene except Cameras and PointLights
     */
    val sceneNodes: Array<Node>
        get() = getSceneNodes { n: Node? -> n !is Camera && n !is PointLight }

    /**
     * Get a list of nodes filtered by filter predicate
     * @param filter, a predicate that filters the candidate nodes
     * @return all nodes that match the predicate
     */
    fun getSceneNodes(filter: Predicate<in Node>): Array<Node> {
        return scene.children.filter{ filter.test(it) }.toTypedArray()
    }

    /**
     * @return an array of all Node's in the scene
     */
    val allSceneNodes: Array<Node>
        get() = getSceneNodes { _: Node? -> true }

    /**
     * Delete the current active node
     */
    fun deleteActiveNode() {
        deleteNode(activeNode)
    }
    /**
     * Delete a specified node and control whether the event is published
     * @param node node to delete from scene
     * @param activePublish whether the deletion should be published
     */
    /**
     * Delete the specified node, this event is published
     * @param node node to delete from scene
     */
    @JvmOverloads
    fun deleteNode(node: Node?, activePublish: Boolean = true) {
        for (child in node!!.children) {
            deleteNode(child, activePublish)
        }
        objectService?.removeObject(node);
        node.parent?.removeChild(node);
        if (activeNode == node) {
            setActiveNode(null)
        }
        //maintain consistency
        if( activePublish ) {
            eventService?.publish(NodeRemovedEvent(node))
        };
    }

    /**
     * Dispose the current scenery renderer, hub, and other scenery things
     */
    fun dispose() {
        val objs: List<Node> = objectService!!.getObjects(Node::class.java)
        for (obj in objs) {
            objectService.removeObject(obj)
        }
        scijavaContext!!.service(SciViewService::class.java).close(this)
        close()
    }

    override fun close() {
        super.close()
        frame!!.dispose()
    }

    /**
     * Move the current active camera to the specified position
     * @param position position to move the camera to
     */
    fun moveCamera(position: FloatArray) {
        camera!!.position = Vector3f(position[0], position[1], position[2])
    }

    /**
     * Move the current active camera to the specified position
     * @param position position to move the camera to
     */
    fun moveCamera(position: DoubleArray) {
        camera!!.position = Vector3f(position[0].toFloat(), position[1].toFloat(), position[2].toFloat())
    }

    /**
     * Get the current application name
     * @return a String of the application name
     */
    fun getName(): String {
        return applicationName
    }

    /**
     * Add a child to the scene. you probably want addNode
     * @param node node to add as a child to the scene
     */
    fun addChild(node: Node?) {
        scene.addChild(node!!)
    }

    /**
     * Add a Dataset to the scene as a volume. Voxel resolution and name are extracted from the Dataset itself
     * @param image image to add as a volume
     * @return a Node corresponding to the Volume
     */
    fun addVolume(image: Dataset): Node? {
        val voxelDims = FloatArray(image.numDimensions())
        for (d in voxelDims.indices) {
            val inValue = image.axis(d).averageScale(0.0, 1.0)
            if (image.axis(d).unit() == null) voxelDims[d] = inValue.toFloat() else voxelDims[d] = unitService!!.value(inValue, image.axis(d).unit(), axis(d)!!.unit()).toFloat()
        }
        return addVolume(image, voxelDims)
    }

    /**
     * Add a Dataset as a Volume with the specified voxel dimensions
     * @param image image to add as a volume
     * @param voxelDimensions dimensions of voxels in volume
     * @return a Node corresponding to the Volume
     */
    @Suppress("UNCHECKED_CAST")
    fun addVolume(image: Dataset, voxelDimensions: FloatArray): Node? {
        return addVolume<RealType<*>>(image.imgPlus as RandomAccessibleInterval<RealType<*>>, image.name,
                *voxelDimensions)
    }

//    /**
//     * Add a RandomAccessibleInterval to the image
//     * @param image image to add as a volume
//     * @param name name of image
//     * @param extra, kludge argument to prevent matching issues
//     * @param <T> pixel type of image
//     * @return a Node corresponding to the volume
//    </T> */
//    fun <T : RealType<T>?> addVolume(image: RandomAccessibleInterval<T>, name: String?, extra: String?): Node {
//        return addVolume(image, name, 1f, 1f, 1f)
//    }

    /**
     * Add a RandomAccessibleInterval to the image
     * @param image image to add as a volume
     * @param <T> pixel type of image
     * @return a Node corresponding to the volume
    </T> */
    fun <T : RealType<T>> addVolume(image: RandomAccessibleInterval<T>, name: String?): Node? {
        return addVolume(image, name, 1f, 1f, 1f)
    }

    /**
     * Add a RandomAccessibleInterval to the image
     * @param image image to add as a volume
     * @param <T> pixel type of image
     * @return a Node corresponding to the volume
    </T> */
    fun <T : RealType<T>> addVolume(image: RandomAccessibleInterval<T>, voxelDimensions: FloatArray): Node? {
        return addVolume(image, "volume", *voxelDimensions)
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image
     * @param <T>
     * @return a Node corresponding to the Volume
    </T> */
    @Suppress("UNCHECKED_CAST")
    @Throws(Exception::class)
    fun <T : RealType<T>?> addVolume(image: IterableInterval<T>): Node? {
        return if (image is RandomAccessibleInterval<*>) {
            addVolume(image as RandomAccessibleInterval<RealType<*>>, "Volume")
        } else {
            throw Exception("Unsupported Volume type:$image")
        }
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image image to add as a volume
     * @param name name of image
     * @param <T> pixel type of image
     * @return a Node corresponding to the Volume
    </T> */
    @Suppress("UNCHECKED_CAST")
    @Throws(Exception::class)
    fun <T : RealType<T>?> addVolume(image: IterableInterval<T>, name: String?): Node? {
        return if (image is RandomAccessibleInterval<*>) {
            addVolume(image as RandomAccessibleInterval<RealType<*>>, name, 1f, 1f, 1f)
        } else {
            throw Exception("Unsupported Volume type:$image")
        }
    }

    /**
     * Set the colormap using an ImageJ LUT name
     * @param n node to apply colormap to
     * @param lutName name of LUT according to imagej LUTService
     */
    fun setColormap(n: Node, lutName: String?) {
        try {
            setColormap(n, lutService!!.loadLUT(lutService.findLUTs()[lutName]))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Set the ColorMap of node n to the supplied colorTable
     * @param n node to apply colortable to
     * @param colorTable ColorTable to use
     */
    fun setColormap(n: Node, colorTable: ColorTable) {
        val copies = 16
        val byteBuffer = ByteBuffer.allocateDirect(
                4 * colorTable.length * copies) // Num bytes * num components * color map length * height of color map texture
        val tmp = ByteArray(4 * colorTable.length)
        for (k in 0 until colorTable.length) {
            for (c in 0 until colorTable.componentCount) {
                // TODO this assumes numBits is 8, could be 16
                tmp[4 * k + c] = colorTable[c, k].toByte()
            }
            if (colorTable.componentCount == 3) {
                tmp[4 * k + 3] = 255.toByte()
            }
        }
        for (i in 0 until copies) {
            byteBuffer.put(tmp)
        }
        byteBuffer.flip()
        n.metadata["sciviewColormap"] = colorTable
        if (n is Volume) {
            n.colormap = Colormap.fromColorTable(colorTable)
            n.dirty = true
            n.needsUpdate = true
        }
    }

    /**
     * Adss a SourceAndConverter to the scene.
     *
     * @param sac The SourceAndConverter to add
     * @param name Name of the dataset
     * @param voxelDimensions Array with voxel dimensions.
     * @param <T> Type of the dataset.
     * @return THe node corresponding to the volume just added.
    </T> */
    fun <T : RealType<T>> addVolume(sac: SourceAndConverter<T>,
                                    numTimepoints: Int,
                                    name: String?,
                                    vararg voxelDimensions: Float): Node? {
        val sources: MutableList<SourceAndConverter<T>> = ArrayList()
        sources.add(sac)
        return addVolume(sources, numTimepoints, name, *voxelDimensions)
    }

    /**
     * Add an IterableInterval to the image with the specified voxelDimensions and name
     * This version of addVolume does most of the work
     * @param image image to add as a volume
     * @param name name of image
     * @param voxelDimensions dimensions of voxel in volume
     * @param <T> pixel type of image
     * @return a Node corresponding to the Volume
    </T> */
    fun <T : RealType<T>> addVolume(image: RandomAccessibleInterval<T>, name: String?,
                                    vararg voxelDimensions: Float): Node? {
        //log.debug( "Add Volume " + name + " image: " + image );
        val dimensions = LongArray(image.numDimensions())
        image.dimensions(dimensions)
        val minPt = LongArray(image.numDimensions())

        // Get type at min point
        val imageRA = image.randomAccess()
        image.min(minPt)
        imageRA.setPosition(minPt)
        val voxelType = imageRA.get()!!.createVariable()
        val converterSetups: ArrayList<ConverterSetup?> = ArrayList<ConverterSetup?>()
        val stacks = AxisOrder.splitInputStackIntoSourceStacks(image, AxisOrder.getAxisOrder(AxisOrder.DEFAULT, image, false))
        val sourceTransform = AffineTransform3D()
        val sources: ArrayList<SourceAndConverter<T>> = ArrayList<SourceAndConverter<T>>()
        var numTimepoints = 1
        for (stack in stacks) {
            var s: Source<T>
            if (stack.numDimensions() > 3) {
                numTimepoints = (stack.max(3) + 1).toInt()
                s = RandomAccessibleIntervalSource4D(stack, voxelType, sourceTransform, name)
            } else {
                s = RandomAccessibleIntervalSource(stack, voxelType, sourceTransform, name)
            }
            val source = BigDataViewer.wrapWithTransformedSource(
                    SourceAndConverter(s, BigDataViewer.createConverterToARGB(voxelType)))
            converterSetups.add(BigDataViewer.createConverterSetup(source, setupId.getAndIncrement()))
            sources.add(source)
        }
        val v = addVolume(sources, numTimepoints, name, *voxelDimensions)
        v?.metadata?.set("RandomAccessibleInterval", image)
        return v
    }

    /**
     * Adds a SourceAndConverter to the scene.
     *
     * This method actually instantiates the volume.
     *
     * @param sources The list of SourceAndConverter to add
     * @param name Name of the dataset
     * @param voxelDimensions Array with voxel dimensions.
     * @param <T> Type of the dataset.
     * @return THe node corresponding to the volume just added.
    </T> */
    @Suppress("UNCHECKED_CAST")
    fun <T : NumericType<T>> addVolume(sources: List<SourceAndConverter<T>>,
                                       converterSetups: ArrayList<ConverterSetup>?,
                                       numTimepoints: Int,
                                       name: String?,
                                       vararg voxelDimensions: Float): Node? {
        var timepoints = numTimepoints
        var cacheControl: CacheControl? = null

//        RandomAccessibleInterval<T> image =
//                ((RandomAccessibleIntervalSource4D) sources.get(0).getSpimSource()).
//                .getSource(0, 0);
        val image = sources[0].spimSource.getSource(0, 0)
        if (image is VolatileView<*, *>) {
            val viewData = (image as VolatileView<T, Volatile<T>?>).volatileViewData
            cacheControl = viewData.cacheControl
        }
        val dimensions = LongArray(image.numDimensions())
        image.dimensions(dimensions)
        val minPt = LongArray(image.numDimensions())

        // Get type at min point
        val imageRA = image.randomAccess()
        image.min(minPt)
        imageRA.setPosition(minPt)
        val voxelType = imageRA.get()!!.createVariable() as T
        println("addVolume " + image.numDimensions() + " interval " + image as Interval)

        //int numTimepoints = 1;
        if (image.numDimensions() > 3) {
            timepoints = image.dimension(3).toInt()
        }
        val ds = RAISource<T>(voxelType, sources, converterSetups!!, timepoints, cacheControl)
        val options = VolumeViewerOptions()
        val v: Volume = RAIVolume(ds, options, hub)
        v.name = name!!
        v.metadata["sources"] = sources
        v.metadata["VoxelDimensions"] = voxelDimensions
        val tf = v.transferFunction
        val rampMin = 0f
        val rampMax = 0.1f
        tf.clear()
        tf.addControlPoint(0.0f, 0.0f)
        tf.addControlPoint(rampMin, 0.0f)
        tf.addControlPoint(1.0f, rampMax)
        val bg = BoundingGrid()
        bg.node = v
        return addNode(v)
    }

    /**
     * Block while predicate is true
     *
     * @param predicate predicate function that returns true as long as this function should block
     * @param waitTime wait time before predicate re-evaluation
     */
    private fun blockWhile(predicate: Function<SciView, Boolean>, waitTime: Int) {
        while (predicate.apply(this)) {
            try {
                Thread.sleep(waitTime.toLong())
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Adds a SourceAndConverter to the scene.
     *
     * @param sources The list of SourceAndConverter to add
     * @param name Name of the dataset
     * @param voxelDimensions Array with voxel dimensions.
     * @param <T> Type of the dataset.
     * @return THe node corresponding to the volume just added.
    </T> */
    fun <T : RealType<T>> addVolume(sources: List<SourceAndConverter<T>>,
                                    numTimepoints: Int,
                                    name: String?,
                                    vararg voxelDimensions: Float): Node? {
        var setupId = 0
        val converterSetups = ArrayList<ConverterSetup>()
        for (source in sources) {
            converterSetups.add(BigDataViewer.createConverterSetup(source, setupId++))
        }
        return addVolume(sources, converterSetups, numTimepoints, name, *voxelDimensions)
    }

    /**
     * Update a volume with the given IterableInterval.
     * This method actually populates the volume
     * @param image image to update into volume
     * @param name name of image
     * @param voxelDimensions dimensions of voxel in volume
     * @param v existing volume to update
     * @param <T> pixel type of image
     * @return a Node corresponding to the input volume
    </T> */
    @Suppress("UNCHECKED_CAST")
    fun <T : RealType<T>> updateVolume(image: IterableInterval<T>, name: String,
                                       voxelDimensions: FloatArray, v: Volume): Node {
        val sacs = v.metadata["sources"] as List<SourceAndConverter<T>>?
        val source = sacs!![0].spimSource.getSource(0, 0) // hard coded to timepoint and mipmap 0
        val sCur = Views.iterable(source).cursor()
        val iCur = image.cursor()
        while (sCur.hasNext()) {
            sCur.fwd()
            iCur.fwd()
            sCur.get()!!.set(iCur.get())
        }
        v.name = name
        v.metadata["VoxelDimensions"] = voxelDimensions
        v.volumeManager.notifyUpdate(v)
        v.volumeManager.requestRepaint()
        //v.getCacheControls().clear();
        //v.setDirty( true );
        v.needsUpdate = true
        //v.setNeedsUpdateWorld( true );
        return v
    }

    /**
     *
     * @return whether PushMode is currently active
     */
    fun getPushMode(): Boolean {
        return renderer!!.pushMode
    }

    /**
     * Set the status of PushMode, which only updates the render panel when there is a change in the scene
     * @param push true if push mode should be used
     * @return current PushMode status
     */
    fun setPushMode(push: Boolean): Boolean {
        renderer!!.pushMode = push
        return renderer!!.pushMode
    }

    protected fun finalize() {
        stopAnimation()
    }

    fun getScenerySettings(): Settings {
        return settings
    }

    fun getSceneryStatistics(): Statistics {
        return stats
    }

    fun getSceneryRenderer(): Renderer? {
        return renderer
    }

    /**
     * Enable VR rendering
     */
    fun toggleVRRendering() {
        vrActive = !vrActive
        val cam = scene.activeObserver as? DetachedHeadCamera ?: return
        var ti: TrackerInput? = null
        var hmdAdded = false
        if (!hub.has(SceneryElement.HMDInput)) {
            try {
                val hmd = OpenVRHMD(false, true)
                if (hmd.initializedAndWorking()) {
                    hub.add(SceneryElement.HMDInput, hmd)
                    ti = hmd
                } else {
                    logger.warn("Could not initialise VR headset, just activating stereo rendering.")
                }
                hmdAdded = true
            } catch (e: Exception) {
                logger.error("Could not add OpenVRHMD: $e")
            }
        } else {
            ti = hub.getWorkingHMD()
        }
        if (vrActive && ti != null) {
            cam.tracker = ti
        } else {
            cam.tracker = null
        }
        renderer!!.pushMode = false
        // we need to force reloading the renderer as the HMD might require device or instance extensions
        if (renderer is VulkanRenderer && hmdAdded) {
            replaceRenderer((renderer as VulkanRenderer).javaClass.simpleName, true, true)
            (renderer as VulkanRenderer).toggleVR()
            while (!(renderer as VulkanRenderer).initialized /* || !getRenderer().getFirstImageReady()*/) {
                logger.debug("Waiting for renderer reinitialisation")
                try {
                    Thread.sleep(200)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
            }
        } else {
            renderer!!.toggleVR()
        }
    }

    /**
     * Set the rotation of Node N by generating a quaternion from the supplied arguments
     * @param n node to set rotation for
     * @param x x coord of rotation quat
     * @param y y coord of rotation quat
     * @param z z coord of rotation quat
     * @param w w coord of rotation quat
     */
    fun setRotation(n: Node, x: Float, y: Float, z: Float, w: Float) {
        n.rotation = Quaternionf(x, y, z, w)
    }

    fun setScale(n: Node, x: Float, y: Float, z: Float) {
        n.scale = Vector3f(x, y, z)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setColor(n: Node, x: Float, y: Float, z: Float, w: Float) {
        val col = Vector3f(x, y, z)
        n.material.ambient = col
        n.material.diffuse = col
        n.material.specular = col
    }

    fun setPosition(n: Node, x: Float, y: Float, z: Float) {
        n.position = Vector3f(x, y, z)
    }

    fun addWindowListener(wl: WindowListener?) {
        frame!!.addWindowListener(wl)
    }

    override fun axis(i: Int): CalibratedAxis? {
        return axes[i]
    }

    override fun axes(calibratedAxes: Array<CalibratedAxis>) {
        axes = calibratedAxes
    }

    override fun setAxis(calibratedAxis: CalibratedAxis, i: Int) {
        axes[i] = calibratedAxis
    }

    override fun realMin(i: Int): Double {
        return Double.NEGATIVE_INFINITY
    }

    override fun realMin(doubles: DoubleArray) {
        for (i in doubles.indices) {
            doubles[i] = Double.NEGATIVE_INFINITY
        }
    }

    override fun realMin(realPositionable: RealPositionable) {
        for (i in 0 until realPositionable.numDimensions()) {
            realPositionable.move(Double.NEGATIVE_INFINITY, i)
        }
    }

    override fun realMax(i: Int): Double {
        return Double.POSITIVE_INFINITY
    }

    override fun realMax(doubles: DoubleArray) {
        for (i in doubles.indices) {
            doubles[i] = Double.POSITIVE_INFINITY
        }
    }

    override fun realMax(realPositionable: RealPositionable) {
        for (i in 0 until realPositionable.numDimensions()) {
            realPositionable.move(Double.POSITIVE_INFINITY, i)
        }
    }

    override fun numDimensions(): Int {
        return axes.size
    }

    fun setActiveObserver(screenshotCam: Camera?) {
        scene.activeObserver = screenshotCam
    }

    fun getActiveObserver(): Camera? {
        return scene.activeObserver
    }

    inner class TransparentSlider : JSlider() {
        override fun paintComponent(g: Graphics) {
            val g2d = g.create() as Graphics2D
            g2d.color = background
            g2d.composite = AlphaComposite.SrcOver.derive(0.9f)
            g2d.fillRect(0, 0, width, height)
            g2d.dispose()
            super.paintComponent(g)
        }

        init {
            // Important, we taking over the filling of the
            // component...
            isOpaque = false
            background = Color.DARK_GRAY
            foreground = Color.LIGHT_GRAY
        }
    }

    internal inner class showHelpDisplay : ClickBehaviour {
        override fun click(x: Int, y: Int) {
            scijavaContext?.getService(CommandService::class.java)?.run(Help::class.java, true)
        }
    }

    /**
     * Return a list of all nodes that match a given predicate function
     * @param nodeMatchPredicate, returns true if a node is a match
     * @return list of nodes that match the predicate
     */
    fun findNodes(nodeMatchPredicate: Function1<Node, Boolean>): List<Node> {
        return scene.discover(scene, nodeMatchPredicate, false)
    }

    /*
     * Convenience function for getting a string of info about a Node
     */
    fun nodeInfoString(n: Node): String {
        return "Node name: " + n.name + " Node type: " + n.nodeType + " To String: " + n
    }

    companion object {
        //bounds for the controls
        @JvmField
        val FPSSPEED_MINBOUND_SLOW = 0.01f
        @JvmField
        val FPSSPEED_MAXBOUND_SLOW = 30.0f
        @JvmField
        val FPSSPEED_MINBOUND_FAST = 0.2f
        @JvmField
        val FPSSPEED_MAXBOUND_FAST = 600f
        @JvmField
        val FPSSPEED_MINBOUND_VERYFAST = 10f
        @JvmField
        val FPSSPEED_MAXBOUND_VERYFAST = 2000f

        @JvmField
        val MOUSESPEED_MINBOUND = 0.1f
        @JvmField
        val MOUSESPEED_MAXBOUND = 3.0f
        @JvmField
        val MOUSESCROLL_MINBOUND = 0.3f
        @JvmField
        val MOUSESCROLL_MAXBOUND = 10.0f

        @JvmField
        val DEFAULT_COLOR = Colors.LIGHTGRAY

        /**
         * Utility function to generate GLVector in cases like usage from Python
         * @param x x coord
         * @param y y coord
         * @param z z coord
         * @return a GLVector of x,y,z
         */
        fun getGLVector(x: Float, y: Float, z: Float): GLVector {
            return GLVector(x, y, z)
        }

        /**
         * Static launching method
         *
         * @return a newly created SciView
         */
        @JvmStatic
        @Throws(Exception::class)
        fun create(): SciView {
            xinitThreads()
            FlatLightLaf.install()
            try {
                UIManager.setLookAndFeel(FlatLightLaf())
            } catch (ex: Exception) {
                System.err.println("Failed to initialize Flat Light LaF, falling back to Swing default.")
            }
            System.setProperty("scijava.log.level:sc.iview", "debug")
            val context = Context(ImageJService::class.java, SciJavaService::class.java, SCIFIOService::class.java, ThreadService::class.java)
            val sciViewService = context.service(SciViewService::class.java)
            return sciViewService.orCreateActiveSciView
        }

        /**
         * Static launching method
         * DEPRECATED use SciView.create() instead
         *
         * @return a newly created SciView
         */
        @Deprecated("")
        @Throws(Exception::class)
        fun createSciView(): SciView {
            return create()
        }
    }
}
/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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

import cleargl.GLTypeEnum
import cleargl.GLVector
import com.jogamp.opengl.math.Quaternion
import coremem.enums.NativeTypeEnum
import graphics.scenery.*
import graphics.scenery.Box
import graphics.scenery.BufferUtils.Companion.allocateFloat
import graphics.scenery.BufferUtils.Companion.allocateInt
import graphics.scenery.backends.Renderer
import graphics.scenery.backends.Renderer.Companion.createRenderer
import graphics.scenery.backends.opengl.OpenGLRenderer
import graphics.scenery.backends.vulkan.VulkanRenderer
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerInput
import graphics.scenery.controls.behaviours.ArcballCameraControl
import graphics.scenery.controls.behaviours.FPSCameraControl
import graphics.scenery.controls.behaviours.MovementCommand
import graphics.scenery.controls.behaviours.SelectCommand
import graphics.scenery.utils.ExtractsNatives
import graphics.scenery.utils.ExtractsNatives.Companion.getPlatform
import graphics.scenery.utils.LogbackUtils.Companion.setLogLevel
import graphics.scenery.utils.SceneryJPanel
import graphics.scenery.utils.SceneryPanel
import graphics.scenery.utils.Statistics
import graphics.scenery.volumes.TransferFunction.Companion.ramp
import graphics.scenery.volumes.Volume
import graphics.scenery.volumes.bdv.BDVVolume
import io.scif.SCIFIOService
import net.imagej.Dataset
import net.imagej.ImageJService
import net.imagej.axis.CalibratedAxis
import net.imagej.axis.DefaultAxisType
import net.imagej.axis.DefaultLinearAxis
import net.imagej.interval.CalibratedRealInterval
import net.imagej.lut.LUTService
import net.imagej.mesh.Mesh
import net.imagej.ops.OpService
import net.imagej.units.UnitService
import net.imglib2.*
import net.imglib2.display.ColorTable
import net.imglib2.img.Img
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.type.numeric.integer.UnsignedShortType
import net.imglib2.type.numeric.real.FloatType
import net.imglib2.type.volatiles.VolatileFloatType
import net.imglib2.type.volatiles.VolatileUnsignedByteType
import net.imglib2.type.volatiles.VolatileUnsignedShortType
import net.imglib2.view.Views
import org.scijava.Context
import org.scijava.`object`.ObjectService
import org.scijava.display.Display
import org.scijava.display.DisplayService
import org.scijava.event.EventHandler
import org.scijava.event.EventService
import org.scijava.io.IOService
import org.scijava.log.LogLevel
import org.scijava.log.LogService
import org.scijava.menu.MenuService
import org.scijava.plugin.Parameter
import org.scijava.service.SciJavaService
import org.scijava.thread.ThreadService
import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.swing.menu.SwingJMenuBarCreator
import org.scijava.util.ColorRGB
import org.scijava.util.Colors
import org.scijava.util.VersionUtils
import sc.iview.commands.view.NodePropertyEditor
import sc.iview.controls.behaviours.CameraTranslateControl
import sc.iview.controls.behaviours.NodeTranslateControl
import sc.iview.event.NodeActivatedEvent
import sc.iview.event.NodeAddedEvent
import sc.iview.event.NodeChangedEvent
import sc.iview.event.NodeRemovedEvent
import sc.iview.process.MeshConverter
import sc.iview.ui.ContextPopUp
import sc.iview.ui.REPLPane
import sc.iview.vector.ClearGLVector3
import sc.iview.vector.Vector3
import tpietzsch.example2.VolumeViewerOptions
import java.awt.*
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.io.*
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Future
import java.util.function.Function
import java.util.function.Predicate
import java.util.function.Supplier
import java.util.stream.Collectors
import javax.imageio.ImageIO
import javax.script.ScriptException
import javax.swing.*

// we suppress unused warnings here because @Parameter-annotated fields
// get updated automatically by SciJava.
class SciView : SceneryBase, CalibratedRealInterval<CalibratedAxis> {
    private val sceneryPanel = arrayOf<SceneryPanel?>(null)
    /**
     * Mouse controls for FPS movement and Arcball rotation
     */
    var targetArcball: ArcballCameraControl? = null
        protected set
    protected var fpsControl: FPSCameraControl? = null
    /*
     * Return the default floor object
     *//*
     * Set the default floor object
     */
    /**
     * The floor that orients the user in the scene
     */
    lateinit var floor: Node
    protected var vrActive = false
    /*
     * Return the current camera that is rendering the scene
     */
    /**
     * The primary camera/observer in the scene
     */
    var camera: Camera? = null
    /**
     * Geometry/Image information of scene
     */
    private var axes: Array<CalibratedAxis> = arrayOf(
            DefaultLinearAxis(DefaultAxisType("X", true), "um", 1.0),
            DefaultLinearAxis(DefaultAxisType("Y", true), "um", 1.0),
            DefaultLinearAxis(DefaultAxisType("Z", true), "um", 1.0)
    )
    @Parameter
    private val log: LogService? = null
    @Parameter
    private val menus: MenuService? = null
    @Parameter
    private val io: IOService? = null
    @Parameter
    private val ops: OpService? = null
    @Parameter
    private val eventService: EventService? = null
    @Parameter
    private val displayService: DisplayService? = null
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
     * @return a Node corresponding to the currently active node
     */
    /**
     * This tracks the actively selected Node in the scene
     */
    var activeNode: Node? = null
        private set
    /**
     * Speeds for input controls
     */
    private var fpsScrollSpeed = 0.05f
    private var mouseSpeedMult = 0.25f
    /*
     * Return the SciJava Display that contains SciView
     *//*
     * Set the SciJava Display
     */
    var display: Display<*>? = null
    private var splashLabel: JLabel? = null
    /**
     * Return the current SceneryJPanel. This is necessary for custom context menus
     * @return
     */
    var sceneryJPanel: SceneryJPanel? = null
        private set
    private var mainSplitPane: JSplitPane? = null
    private var inspector: JSplitPane? = null
    private var interpreterSplitPane: JSplitPane? = null
    private var interpreterPane: REPLPane? = null
    private var nodePropertyEditor: NodePropertyEditor? = null
    private var lights: ArrayList<PointLight>? = null
    private var controlStack: Stack<HashMap<String, Any>>? = null
    private var frame: JFrame? = null
    private val notAbstractNode: Predicate<in Node> = Predicate { node: Node -> !(node is Camera || node is Light || node === floor) }
    var isClosed = false
        private set
    private val notAbstractBranchingFunction = Function { node: Node -> node.children.stream().filter(notAbstractNode).collect(Collectors.toList()) }

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
     * This pushes the current input setup onto a stack that allows them to be restored with restoreControls
     */
    fun stashControls() {
        val controlState = HashMap<String, Any>()
        controlStack!!.push(controlState)
    }

    /**
     * This pops/restores the previously stashed controls. Emits a warning if there are no stashed controls
     */
    fun restoreControls() {
        val controlState = controlStack!!.pop()
        // This isnt how it should work
        setObjectSelectionMode()
        resetFPSInputs()
    }

    /**
     * Place the camera such that all objects in the scene are within the field of view
     */
    fun fitCameraToScene() {
        centerOnNode(scene)
    }

    /**
     * Reset the scene to initial conditions
     */
    fun reset() { // Initialize the 3D axes
        // Remove everything except camera
        val toRemove = getSceneNodes(Predicate { n: Node? -> n !is Camera })
        for (n in toRemove) {
            deleteNode(n, false)
        }
        // Add initial objects
        val tetrahedron = arrayOfNulls<GLVector>(4)
        tetrahedron[0] = GLVector(1.0f, 0f, -1.0f / Math.sqrt(2.0).toFloat())
        tetrahedron[1] = GLVector(-1.0f, 0f, -1.0f / Math.sqrt(2.0).toFloat())
        tetrahedron[2] = GLVector(0.0f, 1.0f, 1.0f / Math.sqrt(2.0).toFloat())
        tetrahedron[3] = GLVector(0.0f, -1.0f, 1.0f / Math.sqrt(2.0).toFloat())
        lights = ArrayList()
        for (i in 0..3) { // TODO allow # initial lights to be customizable?
            val light = PointLight(150.0f)
            light.position = tetrahedron[i]!!.times(25.0f)
            light.emissionColor = GLVector(1.0f, 1.0f, 1.0f)
            light.intensity = 1.0f
            lights!!.add(light)
            scene.addChild(light)
        }
        val cam: Camera?
        if (camera == null) {
            cam = DetachedHeadCamera()
            camera = cam
            scene.addChild(cam)
        } else {
            cam = camera
        }
        cam!!.position = GLVector(0.0f, 5.0f, 5.0f)
        cam.perspectiveCamera(50.0f, windowWidth.toFloat(), windowHeight.toFloat(), 0.1f, 1000.0f)
        cam.active = true
        floor = Box(GLVector(500f, 0.2f, 500f))
        floor.name = "Floor"
        floor.position = GLVector(0f, -1f, 0f)
        floor.material.diffuse = GLVector(1.0f, 1.0f, 1.0f)
        scene.addChild(floor)
    }

    /**
     * Initialization of SWING and scenery. Also triggers an initial population of lights/camera in the scene
     */
    override fun init() { // Darcula dependency went missing from maven repo, factor it out
//        if(Boolean.parseBoolean(System.getProperty("sciview.useDarcula", "false"))) {
//            try {
//                BasicLookAndFeel darcula = new DarculaLaf();
//                UIManager.setLookAndFeel(darcula);
//            } catch (Exception e) {
//                getLogger().info("Could not load Darcula Look and Feel");
//            }
//        }
        log!!.level = LogLevel.WARN
        setLogLevel(null, System.getProperty("scenery.LogLevel", "info"))
        // determine imagej-launcher version and to disable Vulkan if XInitThreads() fix
// is not deployed
        try {
            val launcherClass = Class.forName("net.imagej.launcher.ClassLauncher")
            var versionString = VersionUtils.getVersion(launcherClass)
            if (versionString != null && getPlatform() === ExtractsNatives.Platform.LINUX) {
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
        } catch (cnfe: ClassNotFoundException) { // Didn't find the launcher, so we're probably good.
            logger.info("imagej-launcher not found, not touching renderer preferences.")
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
        //        frame.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent componentEvent) {
//                super.componentResized(componentEvent);
//                panel.setSize(componentEvent.getComponent().getWidth(), componentEvent.getComponent().getHeight());
//            }
//        });
        val splashImage: BufferedImage
        splashImage = try {
            ImageIO.read(this.javaClass.getResourceAsStream("sciview-logo.png"))
        } catch (e: IOException) {
            logger.warn("Could not read splash image 'sciview-logo.png'")
            BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        }
        val sceneryVersion = SceneryBase::class.java.getPackage().implementationVersion
        val sciviewVersion = SciView::class.java.getPackage().implementationVersion
        val versionString: String
        versionString = if (sceneryVersion == null || sciviewVersion == null) {
            "sciview / scenery"
        } else {
            "sciview $sciviewVersion / scenery $sceneryVersion"
        }
        logger.info("This is $versionString")
        splashLabel = JLabel("\n\n" + versionString,
                ImageIcon(splashImage.getScaledInstance(500, 200, Image.SCALE_SMOOTH)),
                SwingConstants.CENTER)
        splashLabel!!.background = Color(50, 48, 47)
        splashLabel!!.foreground = Color(78, 76, 75)
        splashLabel!!.isOpaque = true
        splashLabel!!.verticalTextPosition = JLabel.BOTTOM
        splashLabel!!.horizontalTextPosition = JLabel.CENTER
        p.layout = OverlayLayout(p)
        p.background = Color(50, 48, 47)
        p.add(sceneryJPanel, BorderLayout.CENTER)
        sceneryJPanel!!.isVisible = true
        nodePropertyEditor!!.component // Initialize node property panel
        val inspectorTree = nodePropertyEditor!!.tree
        inspectorTree.toggleClickCount = 0 // This disables expanding menus on double click
        val inspectorProperties = nodePropertyEditor!!.props
        inspector = JSplitPane(JSplitPane.VERTICAL_SPLIT,  //
                JScrollPane(inspectorTree),
                JScrollPane(inspectorProperties))
        inspector!!.dividerLocation = windowHeight / 3
        inspector!!.isContinuousLayout = true
        inspector!!.border = BorderFactory.createEmptyBorder()
        inspector!!.dividerSize = 1
        // We need to get the surface scale here before initialising scenery's renderer, as
// the information is needed already at initialisation time.
        val dt = frame!!.graphicsConfiguration.defaultTransform
        val surfaceScale = GLVector(dt.scaleX.toFloat(), dt.scaleY.toFloat())
        settings.set("Renderer.SurfaceScale", surfaceScale)
        mainSplitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT,  //
                p,
                inspector
        )
        mainSplitPane!!.dividerLocation = windowWidth / 3 * 2
        mainSplitPane!!.border = BorderFactory.createEmptyBorder()
        mainSplitPane!!.dividerSize = 1
        interpreterPane = REPLPane(scijavaContext)
        interpreterPane!!.component.border = BorderFactory.createEmptyBorder()
        interpreterSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT,  //
                mainSplitPane,
                interpreterPane!!.component)
        interpreterSplitPane!!.dividerLocation = windowHeight / 10 * 7
        interpreterSplitPane!!.border = BorderFactory.createEmptyBorder()
        interpreterSplitPane!!.dividerSize = 1
        initializeInterpreter()
        //frame.add(mainSplitPane, BorderLayout.CENTER);
        frame!!.add(interpreterSplitPane, BorderLayout.CENTER)
        val sciView = this
        frame!!.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                logger.debug("Closing SciView window.")
                close()
                scijavaContext!!.service(SciViewService::class.java).close(sciView)
                isClosed = true
            }
        })
        frame!!.glassPane = splashLabel
        frame!!.glassPane.isVisible = true
        //            frame.getGlassPane().setBackground(new java.awt.Color(50, 48, 47, 255));
        frame!!.isVisible = true
        sceneryPanel[0] = sceneryJPanel
        renderer = createRenderer(hub, applicationName, scene,
                windowWidth, windowHeight,
                sceneryPanel[0])
        // Enable push rendering by default
        renderer!!.pushMode = true
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
            frame!!.glassPane.isVisible = false
            logger.info("Done initializing SciView")
            // subscribe to Node{Added, Removed, Changed} events
            eventService!!.subscribe(this)
        }
        // install hook to keep inspector updated on external changes (scripting, etc)
        scene.onNodePropertiesChanged["updateInspector"] = { node: Node ->
            if (node === nodePropertyEditor!!.currentNode) {
                nodePropertyEditor!!.updateProperties(node)
            }
            null
        }
    }

    private fun initializeInterpreter() {
        var startupCode: String? = ""
        startupCode = Scanner(SciView::class.java.getResourceAsStream("startup.py"), "UTF-8").useDelimiter("\\A").next()
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

    /*
     * Center the camera on the scene such that all objects are within the field of view
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
        if (n.boundingBox == null && n.children.size == 0) {
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

    /*
     * Center the camera on the specified Node
     */
/*
     * Center the camera on the specified Node
     */
    @JvmOverloads
    fun centerOnNode(currentNode: Node?, branchFunction: Function<Node, List<Node>> = notAbstractBranchingFunction) {
        if (currentNode == null) return
        val bb = getSubgraphBoundingBox(currentNode, branchFunction) ?: return
        //log.debug("Centering on: " + currentNode + " bb: " + bb.getMin() + " to " + bb.getMax());
        camera!!.target = bb.getBoundingSphere().origin
        camera!!.targeted = true
        // Set forward direction to point from camera at active node
        val forward: GLVector = bb.getBoundingSphere().origin.minus(camera!!.position).normalize().times(-1f)
        val distance = (bb.getBoundingSphere().radius / Math.tan(camera!!.fov / 360 * Math.PI)).toFloat()
        // Solve for the proper rotation
        val rotation = Quaternion().setLookAt(forward.toFloatArray(),
                GLVector(0.0f, 1.0f, 0.0f).toFloatArray(),
                GLVector(1.0f, 0.0f, 0.0f).toFloatArray(),
                GLVector(0.0f, 1.0f, 0.0f).toFloatArray(),
                GLVector(0.0f, 0.0f, 1.0f).toFloatArray())
        camera!!.rotation = rotation.invert().normalize()
        camera!!.position = bb.getBoundingSphere().origin.plus(camera!!.forward.times(distance * -1))
        camera!!.dirty = true
        camera!!.needsUpdate = true
    }

    //log.debug( "FPS scroll speed: " + fpsScrollSpeed );
    var fPSSpeed: Float
        get() = fpsScrollSpeed
        set(newspeed) {
            var newspeed = newspeed
            if (newspeed < 0.30f) newspeed = 0.3f else if (newspeed > 30.0f) newspeed = 30.0f
            fpsScrollSpeed = newspeed
            //log.debug( "FPS scroll speed: " + fpsScrollSpeed );
        }

    //log.debug( "Mouse speed: " + mouseSpeedMult );
    var mouseSpeed: Float
        get() = mouseSpeedMult
        set(newspeed) {
            var newspeed = newspeed
            if (newspeed < 0.30f) newspeed = 0.3f else if (newspeed > 3.0f) newspeed = 3.0f
            mouseSpeedMult = newspeed
            //log.debug( "Mouse speed: " + mouseSpeedMult );
        }

    /*
     * Reset the input handler to first-person-shooter (FPS) style controls
     */
    fun resetFPSInputs() {
        val h = inputHandler
        if (h == null) {
            logger.error("InputHandler is null, cannot change bindings.")
            return
        }
        h.addBehaviour("move_forward_scroll",
                MovementCommand("move_forward", "forward", { scene.findObserver() },
                        fPSSpeed))
        h.addBehaviour("move_forward",
                MovementCommand("move_forward", "forward", { scene.findObserver() },
                        fPSSpeed))
        h.addBehaviour("move_back",
                MovementCommand("move_back", "back", { scene.findObserver() },
                        fPSSpeed))
        h.addBehaviour("move_left",
                MovementCommand("move_left", "left", { scene.findObserver() },
                        fPSSpeed))
        h.addBehaviour("move_right",
                MovementCommand("move_right", "right", { scene.findObserver() },
                        fPSSpeed))
        h.addBehaviour("move_up",
                MovementCommand("move_up", "up", { scene.findObserver() },
                        fPSSpeed))
        h.addBehaviour("move_down",
                MovementCommand("move_down", "down", { scene.findObserver() },
                        fPSSpeed))
        h.addKeyBinding("move_forward_scroll", "scroll")
    }

    fun setObjectSelectionMode() {
        val selectAction = { result: Scene.RaycastResult, x: Int, y: Int ->
            val matches = result.matches
            if (!matches.isEmpty()) {
                setActiveNode(matches[0].node)
                nodePropertyEditor!!.trySelectNode(activeNode)
                log!!.info("Selected node: " + activeNode!!.name + " at " + x + "," + y)
                // Setup the context menu for this node
                val menu = ContextPopUp(matches[0].node)
                menu.show(sceneryJPanel, x, y)
            }
        }
        setObjectSelectionMode(selectAction)
    }

    /*
     * Set the action used during object selection
     */
    fun setObjectSelectionMode(selectAction: Function3<Scene.RaycastResult, Int, Int, Unit>) {
        val h = inputHandler
        val ignoredObjects: MutableList<Class<*>> = ArrayList()
        ignoredObjects.add(BoundingGrid::class.java)
        if (h == null) {
            logger.error("InputHandler is null, cannot change object selection mode.")
            return
        }
        h.addBehaviour("object_selection_mode",
                SelectCommand("objectSelector", renderer!!, scene,
                        { scene.findObserver() }, true, ignoredObjects,
                        selectAction))
        h.addKeyBinding("object_selection_mode", "double-click button1")
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
        // TODO: Maybe get rid of this?
        h.useDefaultBindings("")
        // Mouse controls
        setObjectSelectionMode()
        val nodeTranslate = NodeTranslateControl(this, 0.0005f)
        h.addBehaviour("mouse_control_nodetranslate", nodeTranslate)
        h.addKeyBinding("mouse_control_nodetranslate", "ctrl button1")
        h.addBehaviour("scroll_nodetranslate", nodeTranslate)
        h.addKeyBinding("scroll_nodetranslate", "ctrl scroll")
        h.addBehaviour("move_up_slow", MovementCommand("move_up", "up", { scene.findObserver() }, fpsScrollSpeed))
        h.addBehaviour("move_down_slow", MovementCommand("move_down", "down", { scene.findObserver() }, fpsScrollSpeed))
        h.addBehaviour("move_up_fast", MovementCommand("move_up", "up", { scene.findObserver() }, 1.0f))
        h.addBehaviour("move_down_fast", MovementCommand("move_down", "down", { scene.findObserver() }, 1.0f))
        h.addKeyBinding("move_up_slow", "X")
        h.addKeyBinding("move_down_slow", "C")
        h.addKeyBinding("move_up_fast", "shift X")
        h.addKeyBinding("move_down_fast", "shift C")
        enableArcBallControl()
        enableFPSControl()
        // Extra keyboard controls
        h.addBehaviour("show_help", showHelpDisplay())
        h.addKeyBinding("show_help", "U")
        h.addBehaviour("enable_decrease", enableDecrease())
        h.addKeyBinding("enable_decrease", "M")
        h.addBehaviour("enable_increase", enableIncrease())
        h.addKeyBinding("enable_increase", "N")
        //float veryFastSpeed = getScene().getMaximumBoundingBox().getBoundingSphere().getRadius()/50f;
        val veryFastSpeed = 100f
        h.addBehaviour("move_forward_veryfast", MovementCommand("move_forward", "forward", { scene.findObserver() }, veryFastSpeed))
        h.addBehaviour("move_back_veryfast", MovementCommand("move_back", "back", { scene.findObserver() }, veryFastSpeed))
        h.addBehaviour("move_left_veryfast", MovementCommand("move_left", "left", { scene.findObserver() }, veryFastSpeed))
        h.addBehaviour("move_right_veryfast", MovementCommand("move_right", "right", { scene.findObserver() }, veryFastSpeed))
        h.addBehaviour("move_up_veryfast", MovementCommand("move_up", "up", { scene.findObserver() }, veryFastSpeed))
        h.addBehaviour("move_down_veryfast", MovementCommand("move_down", "down", { scene.findObserver() }, veryFastSpeed))
        h.addKeyBinding("move_forward_veryfast", "ctrl shift W")
        h.addKeyBinding("move_back_veryfast", "ctrl shift S")
        h.addKeyBinding("move_left_veryfast", "ctrl shift A")
        h.addKeyBinding("move_right_veryfast", "ctrl shift D")
        h.addKeyBinding("move_up_veryfast", "ctrl shift X")
        h.addKeyBinding("move_down_veryfast", "ctrl shift C")
    }

    /*
     * Change the control mode to circle around the active object in an arcball
     */
    private fun enableArcBallControl() {
        val h = inputHandler
        if (h == null) {
            logger.error("InputHandler is null, cannot setup arcball.")
            return
        }
        val target: GLVector
        target = if (activeNode == null) {
            GLVector(0.0f, 0.0f, 0.0f)
        } else {
            activeNode!!.position
        }
        var mouseSpeed = 0.25f
        mouseSpeed = mouseSpeed
        val cameraSupplier = Supplier { scene.findObserver() }
        targetArcball = ArcballCameraControl("mouse_control_arcball", cameraSupplier,
                renderer!!.window.width,
                renderer!!.window.height, target)
        targetArcball!!.maximumDistance = Float.MAX_VALUE
        targetArcball!!.mouseSpeedMultiplier = mouseSpeed
        targetArcball!!.scrollSpeedMultiplier = 0.05f
        targetArcball!!.distance = camera!!.position.minus(target).magnitude()
        // FIXME: Swing seems to have issues with shift-scroll actions, so we change
//  this to alt-scroll here for the moment.
        h.addBehaviour("mouse_control_arcball", targetArcball!!)
        h.addKeyBinding("mouse_control_arcball", "shift button1")
        h.addBehaviour("scroll_arcball", targetArcball!!)
        h.addKeyBinding("scroll_arcball", "shift scroll")
    }

    private fun enableTeleportControl() {
        val h = inputHandler
        if (h == null) {
            logger.error("InputHandler is null, cannot setup arcball.")
            return
        }
        val target: GLVector
        target = if (activeNode == null) {
            GLVector(0.0f, 0.0f, 0.0f)
        } else {
            activeNode!!.position
        }
        var mouseSpeed = 0.25f
        mouseSpeed = mouseSpeed
        val cameraSupplier = Supplier { scene.findObserver() }
        targetArcball = ArcballCameraControl("mouse_control_arcball", cameraSupplier,
                renderer!!.window.width,
                renderer!!.window.height, target)
        targetArcball!!.maximumDistance = Float.MAX_VALUE
        targetArcball!!.mouseSpeedMultiplier = mouseSpeed
        targetArcball!!.scrollSpeedMultiplier = 0.05f
        targetArcball!!.distance = camera!!.position.minus(target).magnitude()
        // FIXME: Swing seems to have issues with shift-scroll actions, so we change
//  this to alt-scroll here for the moment.
        h.addBehaviour("mouse_control_arcball", targetArcball!!)
        h.addKeyBinding("mouse_control_arcball", "shift button1")
        h.addBehaviour("scroll_arcball", targetArcball!!)
        h.addKeyBinding("scroll_arcball", "shift scroll")
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
        val cameraSupplier = Supplier { scene.findObserver() }
        fpsControl = FPSCameraControl("mouse_control", cameraSupplier, renderer!!.window.width,
                renderer!!.window.height)
        h.addBehaviour("mouse_control", fpsControl!!)
        h.addKeyBinding("mouse_control", "button1")
        h.addBehaviour("mouse_control_cameratranslate", CameraTranslateControl(this, 0.002f))
        h.addKeyBinding("mouse_control_cameratranslate", "button2")
        resetFPSInputs()
    }
    /**
     * Add a box at the specified position with specified size, color, and normals on the inside/outside
     * @param position
     * @param size
     * @param color
     * @param inside
     * @return the Node corresponding to the box
     */
    /**
     * Add a box at the specified position and with the specified size
     * @param position
     * @param size
     * @return the Node corresponding to the box
     */
    /**
     * Add a box at the specific position and unit size
     * @param position
     * @return the Node corresponding to the box
     */
    /**
     * Add a box to the scene with default parameters
     * @return the Node corresponding to the box
     */
    @JvmOverloads
    fun addBox(position: Vector3? = ClearGLVector3(0.0f, 0.0f, 0.0f), size: Vector3? = ClearGLVector3(1.0f, 1.0f, 1.0f), color: ColorRGB? = DEFAULT_COLOR,
               inside: Boolean = false): Node { // TODO: use a material from the current palate by default
        val boxmaterial = Material()
        boxmaterial.ambient = GLVector(1.0f, 0.0f, 0.0f)
        boxmaterial.diffuse = Utils.convertToGLVector(color)
        boxmaterial.specular = GLVector(1.0f, 1.0f, 1.0f)
        val box = Box(ClearGLVector3.convert(size), inside)
        box.material = boxmaterial
        box.position = ClearGLVector3.convert(position)
        return addNode(box)
    }
    /**
     * Add a sphere at the specified positoin with a given radius and color
     * @param position
     * @param radius
     * @param color
     * @return  the Node corresponding to the sphere
     */
    /**
     * Add a sphere at the specified position with a given radius
     * @param position
     * @param radius
     * @return the Node corresponding to the sphere
     */
    /**
     * Add a unit sphere at the origin
     * @return the Node corresponding to the sphere
     */
    @JvmOverloads
    fun addSphere(position: Vector3? = ClearGLVector3(0.0f, 0.0f, 0.0f), radius: Float = 1f, color: ColorRGB? = DEFAULT_COLOR): Node {
        val material = Material()
        material.ambient = GLVector(1.0f, 0.0f, 0.0f)
        material.diffuse = Utils.convertToGLVector(color)
        material.specular = GLVector(1.0f, 1.0f, 1.0f)
        val sphere = Sphere(radius, 20)
        sphere.material = material
        sphere.position = ClearGLVector3.convert(position)
        return addNode(sphere)
    }

    /**
     * Add a Cylinder at the given position with radius, height, and number of faces/segments
     * @param position
     * @param radius
     * @param height
     * @param num_segments
     * @return  the Node corresponding to the cylinder
     */
    fun addCylinder(position: Vector3?, radius: Float, height: Float, num_segments: Int): Node {
        val cyl = Cylinder(radius, height, num_segments)
        cyl.position = ClearGLVector3.convert(position)
        return addNode(cyl)
    }

    /**
     * Add a Cone at the given position with radius, height, and number of faces/segments
     * @param position
     * @param radius
     * @param height
     * @param num_segments
     * @return  the Node corresponding to the cone
     */
    fun addCone(position: Vector3?, radius: Float, height: Float, num_segments: Int): Node {
        val cone = Cone(radius, height, num_segments, GLVector(0.0f, 0.0f, 1.0f))
        cone.position = ClearGLVector3.convert(position)
        return addNode(cone)
    }
    /**
     * Add a line from start to stop with the given color
     * @param start
     * @param stop
     * @param color
     * @return the Node corresponding to the line
     */
    /**
     * Add a line from start to stop
     * @param start
     * @param stop
     * @return  the Node corresponding to the line
     */
    /**
     * Add a Line from 0,0,0 to 1,1,1
     * @return  the Node corresponding to the line
     */
    @JvmOverloads
    fun addLine(start: Vector3 = ClearGLVector3(0.0f, 0.0f, 0.0f), stop: Vector3 = ClearGLVector3(1.0f, 1.0f, 1.0f), color: ColorRGB? = DEFAULT_COLOR): Node {
        return addLine(arrayOf(start, stop), color, 0.1)
    }

    /**
     * Add a multi-segment line that goes through the supplied points with a single color and edge width
     * @param points
     * @param color
     * @param edgeWidth
     * @return the Node corresponding to the line
     */
    fun addLine(points: Array<Vector3>, color: ColorRGB?, edgeWidth: Double): Node {
        val material = Material()
        material.ambient = GLVector(1.0f, 1.0f, 1.0f)
        material.diffuse = Utils.convertToGLVector(color)
        material.specular = GLVector(1.0f, 1.0f, 1.0f)
        val line = Line(points.size)
        for (pt in points) {
            line.addPoint(ClearGLVector3.convert(pt))
        }
        line.edgeWidth = edgeWidth.toFloat()
        line.material = material
        line.position = ClearGLVector3.convert(points[0])
        return addNode(line)
    }

    /**
     * Add a PointLight source at the origin
     * @return a Node corresponding to the PointLight
     */
    fun addPointLight(): Node {
        val material = Material()
        material.ambient = GLVector(1.0f, 0.0f, 0.0f)
        material.diffuse = GLVector(0.0f, 1.0f, 0.0f)
        material.specular = GLVector(1.0f, 1.0f, 1.0f)
        val light = PointLight(5.0f)
        light.material = material
        light.position = GLVector(0.0f, 0.0f, 0.0f)
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
            val x: Float = (c.x() + r * Math.cos(if (k == 0) 0.0 else Math.PI * 2 * (k.toFloat() / lights!!.size.toFloat()))).toFloat()
            val z: Float = (c.y() + r * Math.sin(if (k == 0) 0.0 else Math.PI * 2 * (k.toFloat() / lights!!.size.toFloat()))).toFloat()
            light.lightRadius = 2 * r
            light.position = GLVector(x, y, z)
        }
    }

    /**
     * Write a scenery mesh as an stl to the given file
     * @param filename
     * @param scMesh
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
                out.write(("facet normal " + normalsFB.get() + " " + normalsFB.get() + " " + normalsFB.get() +
                        "\n").toByteArray())
                out.write("outer loop\n".toByteArray())
                for (v in 0..2) {
                    out.write(("vertex\t" + verticesFB.get() + " " + verticesFB.get() + " " + verticesFB.get() +
                            "\n").toByteArray())
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
     * @return
     */
    val defaultPointSize: Float
        get() = 0.025f

    /**
     * Create an array of normal vectors from a set of vertices corresponding to triangles
     */
    fun makeNormalsFromVertices(verts: ArrayList<RealPoint>): FloatArray {
        val normals = FloatArray(verts.size) // div3 * 3coords
        var k = 0
        while (k < verts.size) {
            val v1 = GLVector(verts[k].getFloatPosition(0),  //
                    verts[k].getFloatPosition(1),  //
                    verts[k].getFloatPosition(2))
            val v2 = GLVector(verts[k + 1].getFloatPosition(0),
                    verts[k + 1].getFloatPosition(1),
                    verts[k + 1].getFloatPosition(2))
            val v3 = GLVector(verts[k + 2].getFloatPosition(0),
                    verts[k + 2].getFloatPosition(1),
                    verts[k + 2].getFloatPosition(2))
            val a = v2.minus(v1)
            val b = v3.minus(v1)
            val n = a.cross(b).normalized
            normals[k / 3] = n[0]
            normals[k / 3 + 1] = n[1]
            normals[k / 3 + 2] = n[2]
            k += 3
        }
        return normals
    }

    /**
     * Open a file specified by the source path. The file can be anything that SciView knows about: mesh, volume, point cloud
     * @param source
     * @throws IOException
     */
    @Throws(IOException::class)
    fun open(source: String) {
        if (source.endsWith(".xml")) {
            addBDVVolume(source)
            return
        }
        val data = io!!.open(source)
        if (data is Mesh) {
            addMesh(data)
        } else if (data is graphics.scenery.Mesh) {
            addMesh(data)
        } else if (data is PointCloud) {
            addPointCloud(data)
        } else if (data is Dataset) {
            addVolume(data)
        } else if (data is IterableInterval<*>) {
            addVolume(data as IterableInterval<RealType<*>>, source)
        } else if (data is List<*>) {
            val list = data
            require(!list.isEmpty()) { "Data source '$source' appears empty." }
            val element = list[0]
            if (element is RealLocalizable) { // NB: For now, we assume all elements will be RealLocalizable.
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
     * @param points
     * @param name
     * @return
     */
    /**
     * Add the given points to the scene as a PointCloud
     * @param points
     * @return a Node corresponding to the PointCloud
     */
    @JvmOverloads
    fun addPointCloud(points: Collection<RealLocalizable>,
                      name: String? = "PointCloud"): Node {
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
        val vBuffer = allocateFloat(flatVerts.size * 4)
        val nBuffer = allocateFloat(0)
        vBuffer.put(flatVerts)
        vBuffer.flip()
        pointCloud.vertices = vBuffer
        pointCloud.normals = nBuffer
        pointCloud.indices = allocateInt(0)
        pointCloud.setupPointCloud()
        material.ambient = GLVector(1.0f, 1.0f, 1.0f)
        material.diffuse = GLVector(1.0f, 1.0f, 1.0f)
        material.specular = GLVector(1.0f, 1.0f, 1.0f)
        pointCloud.material = material
        pointCloud.position = GLVector(0f, 0f, 0f)
        return addNode(pointCloud)
    }

    /**
     * Add a PointCloud to the scene
     * @param pointCloud
     * @return a Node corresponding to the PointCloud
     */
    fun addPointCloud(pointCloud: PointCloud): Node {
        pointCloud.setupPointCloud()
        pointCloud.material.ambient = GLVector(1.0f, 1.0f, 1.0f)
        pointCloud.material.diffuse = GLVector(1.0f, 1.0f, 1.0f)
        pointCloud.material.specular = GLVector(1.0f, 1.0f, 1.0f)
        pointCloud.position = GLVector(0f, 0f, 0f)
        return addNode(pointCloud)
    }
    /**
     * Add Node n to the scene and set it as the active node/publish it to the event service if activePublish is true
     * @param n
     * @param activePublish
     * @return a Node corresponding to the Node
     */
    /**
     * Add a Node to the scene and publish it to the eventservice
     * @param n
     * @return a Node corresponding to the Node
     */
    @JvmOverloads
    fun addNode(n: Node, activePublish: Boolean = true): Node {
        scene.addChild(n)
        objectService!!.addObject(n)
        if (activePublish) { //            setActiveNode(n);
//            if (floor.getVisible())
//                updateFloorPosition();
            eventService!!.publish(NodeAddedEvent(n))
        }
        return n
    }

    /**
     * Add a scenery Mesh to the scene
     * @param scMesh
     * @return a Node corresponding to the mesh
     */
    fun addMesh(scMesh: graphics.scenery.Mesh): Node {
        val material = Material()
        material.ambient = GLVector(1.0f, 0.0f, 0.0f)
        material.diffuse = GLVector(0.0f, 1.0f, 0.0f)
        material.specular = GLVector(1.0f, 1.0f, 1.0f)
        scMesh.material = material
        scMesh.position = GLVector(0.0f, 0.0f, 0.0f)
        objectService!!.addObject(scMesh)
        return addNode(scMesh)
    }

    /**
     * Add an ImageJ mesh to the scene
     * @param mesh
     * @return a Node corresponding to the mesh
     */
    fun addMesh(mesh: Mesh?): Node {
        val scMesh = MeshConverter.toScenery(mesh)
        return addMesh(scMesh)
    }

    /**
     * [Deprecated: use deleteNode]
     * Remove a Mesh from the scene
     * @param scMesh
     */
    fun removeMesh(scMesh: graphics.scenery.Mesh?) {
        scene.removeChild(scMesh!!)
    }

    /**
     * Set the currently active node
     * @param n
     * @return the currently active node
     */
    fun setActiveNode(n: Node?): Node? {
        if (activeNode === n) return activeNode
        activeNode = n
        targetArcball!!.target = { n?.getMaximumBoundingBox()?.getBoundingSphere()?.origin ?: GLVector(0.0f, 0.0f, 0.0f) }
        eventService!!.publish(NodeActivatedEvent(activeNode))
        return activeNode
    }

    @EventHandler
    protected fun onNodeAdded(event: NodeAddedEvent?) {
        nodePropertyEditor!!.rebuildTree()
    }

    @EventHandler
    protected fun onNodeRemoved(event: NodeRemovedEvent?) {
        nodePropertyEditor!!.rebuildTree()
    }

    @EventHandler
    protected fun onNodeChanged(event: NodeChangedEvent?) {
        nodePropertyEditor!!.rebuildTree()
    }

    @EventHandler
    protected fun onNodeActivated(event: NodeActivatedEvent?) { // TODO: add listener code for node activation, if necessary
// NOTE: do not update property window here, this will lead to a loop.
    }

    fun toggleInspectorWindow() {
        val currentlyVisible = inspector!!.isVisible
        if (currentlyVisible) {
            inspector!!.isVisible = false
            mainSplitPane!!.dividerLocation = windowWidth
        } else {
            inspector!!.isVisible = true
            mainSplitPane!!.dividerLocation = windowWidth / 4 * 3
        }
    }

    fun setInspectorWindowVisibility(visible: Boolean) {
        inspector!!.isVisible = visible
        if (visible) mainSplitPane!!.dividerLocation = windowWidth / 4 * 3 else mainSplitPane!!.dividerLocation = windowWidth
    }

    fun setInterpreterWindowVisibility(visible: Boolean) {
        interpreterPane!!.component.isVisible = visible
        if (visible) interpreterSplitPane!!.dividerLocation = windowHeight / 10 * 6 else interpreterSplitPane!!.dividerLocation = windowHeight
    }

    /**
     * Create an animation thread with the given fps speed and the specified action
     * @param fps
     * @param action
     * @return a Future corresponding to the thread
     */
    @Synchronized
    fun animate(fps: Int, action: Runnable): Future<*> { // TODO: Make animation speed less laggy and more accurate.
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
     * @param path
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
            var tmpFile: File? = null
            try {
                tmpFile = File.createTempFile("sciview-", "-tmp.png")
                ImageIO.write(image, "png", tmpFile)
                img = io!!.open(tmpFile.absolutePath) as Img<UnsignedByteType?>
                tmpFile.delete()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return img
        }

    /**
     * @return an array of all nodes in the scene except Cameras and PointLights
     */
    val sceneNodes: Array<Node>
        get() = getSceneNodes(Predicate { n: Node? -> n !is Camera && n !is PointLight })

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
        get() = getSceneNodes(Predicate { n: Node? -> true })

    /**
     * Delete the current active node
     */
    fun deleteActiveNode() {
        deleteNode(activeNode)
    }
    /**
     * Delete a specified node and control whether the event is published
     * @param node
     * @param activePublish
     */
    /**
     * Delete the specified node, this event is published
     * @param node
     */
    @JvmOverloads
    fun deleteNode(node: Node?, activePublish: Boolean = true) {
        for (child in node!!.children) {
            deleteNode(child, activePublish)
        }
        objectService!!.removeObject(node)
        node.parent!!.removeChild(node)
        if (activePublish) {
            eventService!!.publish(NodeRemovedEvent(node))
            if (activeNode === node) setActiveNode(null)
        }
    }

    /**
     * Dispose the current scenery renderer, hub, and other scenery things
     */
    fun dispose() {
        val objs = objectService!!.getObjects(Node::class.java)
        for (obj in objs) {
            objectService.removeObject(obj)
        }
        scijavaContext!!.service(SciViewService::class.java).close(this)
        close()
    }

    /**
     * Move the current active camera to the specified position
     * @param position
     */
    fun moveCamera(position: FloatArray) {
        camera!!.position = GLVector(position[0], position[1], position[2])
    }

    /**
     * Move the current active camera to the specified position
     * @param position
     */
    fun moveCamera(position: DoubleArray) {
        camera!!.position = GLVector(position[0].toFloat(), position[1].toFloat(), position[2].toFloat())
    }

    /**
     * Get the current application name
     * @return a String of the application name
     */
    fun getName(): String {
        return "SciView"
    }

    /**
     * Add a child to the scene. you probably want addNode
     * @param node
     */
    fun addChild(node: Node?) {
        scene.addChild(node!!)
    }

    /**
     * Add a Dataset to the scene as a volume. Voxel resolution and name are extracted from the Dataset itself
     * @param image
     * @return a Node corresponding to the Volume
     */
    fun addVolume(image: Dataset): Node? {
        val voxelDims = FloatArray(image.numDimensions())
        for (d in voxelDims.indices) {
            val inValue = image.axis(d).averageScale(0.0, 1.0)
            voxelDims[d] = unitService!!.value(inValue, image.axis(d).unit(), axis(d).unit()).toFloat()
        }
        return addVolume(image, voxelDims)
    }

    /**
     * Add a BigDataViewer volume to the scene.
     * @param source, the path to an XML file for BDV style XML/Hdf5
     * @return a Node corresponding to the BDVNode
     */
    fun addBDVVolume(source: String?): Node { //getSettings().set("Renderer.HDR.Exposure", 20.0f);
        val opts = VolumeViewerOptions()
        opts.maxCacheSizeInMB(System.getProperty("scenery.BDVVolume.maxCacheSize", "512").toInt())
        val v = BDVVolume(source!!, opts)
        // TODO: use unitService to set scale
        v.scale = GLVector(0.01f, 0.01f, 0.01f)
        v.boundingBox = v.generateBoundingBox()
        scene.addChild(v)
        setActiveNode(v)
        v.goToTimePoint(0)
        eventService!!.publish(NodeAddedEvent(v))
        return v
    }

    /**
     * Add a Dataset as a Volume with the specified voxel dimensions
     * @param image
     * @param voxelDimensions
     * @return a Node corresponding to the Volume
     */
    fun addVolume(image: Dataset, voxelDimensions: FloatArray): Node {
        return addVolume(Views.flatIterable(image.imgPlus) as IterableInterval<@kotlin.jvm.JvmWildcard RealType<*>>, image.name,
                *voxelDimensions)
    }

    /**
     * Add a RandomAccessibleInterval to the image
     * @param image
     * @param name
     * @param extra, kludge argument to prevent matching issues
     * @param <T>
     * @return a Node corresponding to the volume
    </T> */
    fun <T : RealType<T>> addVolume(image: RandomAccessibleInterval<T>, name: String, extra: String): Node? {
        val pos = longArrayOf(10, 10, 10)
        return addVolume(Views.flatIterable(image), name, 1f, 1f, 1f)
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image
     * @param <T>
     * @return a Node corresponding to the Volume
    </T> */
    fun <T : RealType<T>> addVolume(image: IterableInterval<T>): Node {
        return addVolume(image, "Volume")
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image
     * @param name
     * @param <T>
     * @return a Node corresponding to the Volume
    </T> */
    fun <T : RealType<T>> addVolume(image: IterableInterval<out T>, name: String): Node {
        return addVolume(image, name, 1f, 1f, 1f)
    }

    /**
     * Set the colormap using an ImageJ LUT name
     * @param n
     * @param lutName
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
     * @param n
     * @param colorTable
     */
    fun setColormap(n: Node, colorTable: ColorTable) {
        val copies = 16
        val byteBuffer = ByteBuffer.allocateDirect(
                4 * colorTable.length * copies) // Num bytes * num components * color map length * height of color map texture
        val tmp = ByteArray(4 * colorTable.length)
        for (k in 0 until colorTable.length) {
            for (c in 0 until colorTable.componentCount) { // TODO this assumes numBits is 8, could be 16
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
            n.colormaps["sciviewColormap"] = Volume.Colormap.ColormapBuffer(GenericTexture("colorTable",
                    GLVector(colorTable.length.toFloat(), copies.toFloat(), 1.0f), 4,
                    GLTypeEnum.UnsignedByte,
                    byteBuffer,  // don't repeat the color map
                    TextureRepeatMode.ClampToEdge, TextureRepeatMode.ClampToEdge, TextureRepeatMode.ClampToEdge))
            n.colormap = "sciviewColormap"
            n.dirty = true
            n.needsUpdate = true
        }
    }

    /**
     * Add an IterableInterval to the image with the specified voxelDimensions and name
     * This version of addVolume does most of the work
     * @param image
     * @param name
     * @param voxelDimensions
     * @param <T>
     * @return a Node corresponding to the Volume
    </T> */
    fun <T: RealType<*>> addVolume(image: IterableInterval<out T>, name: String?,
                                   vararg voxelDimensions: Float): Node { //log.debug( "Add Volume " + name + " image: " + image );
        val dimensions = LongArray(3)
        image.dimensions(dimensions)
        val v = Volume()
        scene.addChild(v)
        val voxelType: T = image.firstElement()
        logger.info("Type is $voxelType/${voxelType.javaClass}")
        var minVal: Float
        var maxVal: Float
        when (voxelType) {
            is UnsignedByteType -> {
                minVal = 0f
                maxVal = 255f
            }
            is UnsignedShortType -> {
                minVal = 0f
                maxVal = 65535f
            }
            is FloatType -> {
                minVal = 0f
                maxVal = 1f
            }
            is VolatileUnsignedByteType -> {
                minVal = 0f
                maxVal = 255f
            }
            is VolatileUnsignedShortType -> {
                minVal = 0f
                maxVal = 65535f
            }
            is VolatileFloatType -> {
                minVal = 0f
                maxVal = 1f
            }
            else -> {
                log!!.debug("Type: " + voxelType +
                        " cannot be displayed as a volume. Convert to UnsignedByteType, UnsignedShortType, or FloatType.")
                //return v
                minVal = 0f
                maxVal = 32000f
            }
        }
        updateVolume(image, name, voxelDimensions, v)
        v.trangemin = minVal
        v.trangemax = maxVal
        v.transferFunction = ramp(0.0f, 0.4f)
        try {
            setColormap(v, lutService!!.loadLUT(lutService.findLUTs()["WCIF/ICA.lut"]))
        } catch (e: IOException) {
            e.printStackTrace()
        }
        setActiveNode(v)
        eventService!!.publish(NodeAddedEvent(v))
        objectService!!.addObject(v)
        return v
    }

    /**
     * Update a volume with the given IterableInterval.
     * This method actually populates the volume
     * @param image
     * @param name
     * @param voxelDimensions
     * @param v
     * @param <T>
     * @return a Node corresponding to the input volume
    </T> */
    fun <T: RealType<T>> updateVolume(image: IterableInterval<in T>, name: String?,
                                        voxelDimensions: FloatArray, v: Volume): Node? { //log.debug( "Update Volume" );
        val dimensions = LongArray(3)
        image.dimensions(dimensions)
        val voxelType = image.firstElement()!!
        val bytesPerVoxel = 2//image.firstElement()!!.bitsPerPixel / 8
        var nType: NativeTypeEnum
        nType = if (voxelType is UnsignedByteType || voxelType is VolatileUnsignedByteType) {
            NativeTypeEnum.UnsignedByte
        } else if (voxelType is UnsignedShortType || voxelType is VolatileUnsignedShortType) {
            NativeTypeEnum.UnsignedShort
        } else if (voxelType is FloatType || voxelType is VolatileFloatType) {
            NativeTypeEnum.Float
        } else {
            log!!.debug("Type: " + voxelType +
                    " cannot be displayed as a volume. Convert to UnsignedByteType, UnsignedShortType, or FloatType.")
            return null
        }

        // Make and populate a ByteBuffer with the content of the Dataset
        val byteBuffer = ByteBuffer.allocateDirect(
                (bytesPerVoxel * dimensions[0] * dimensions[1] * dimensions[2]).toInt())
        val cursor = image.cursor()
        while (cursor.hasNext()) {
            cursor.fwd()
            // TODO should we check if volatiles are valid
            when (voxelType) {
                is UnsignedByteType -> {
                    byteBuffer.put((cursor.get() as UnsignedByteType).get().toByte())
                }
                is VolatileUnsignedByteType -> {
                    byteBuffer.put((cursor.get() as VolatileUnsignedByteType).get().get().toByte())
                }
                is UnsignedShortType -> {
                    byteBuffer.putShort(Math.abs((cursor.get() as UnsignedShortType).short.toInt()).toShort())
                }
                is VolatileUnsignedShortType -> {
                    byteBuffer.putShort(Math.abs((cursor.get() as VolatileUnsignedShortType).get().short.toInt()).toShort())
                }
                is FloatType -> {
                    byteBuffer.putFloat((cursor.get() as FloatType).get())
                }
                is VolatileFloatType -> {
                    byteBuffer.putFloat((cursor.get() as VolatileFloatType).get().get())
                }
            }
        }
        byteBuffer.flip()
        v.readFromBuffer(name!!, byteBuffer, dimensions[0], dimensions[1], dimensions[2], voxelDimensions[0],
                voxelDimensions[1], voxelDimensions[2], nType, bytesPerVoxel)
        v.dirty = true
        v.needsUpdate = true
        v.needsUpdateWorld = true
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
     * @param push
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

    fun getSceneryStats(): Statistics {
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
        val r = renderer
        if (r is VulkanRenderer && hmdAdded) {
            replaceRenderer(r.javaClass.getSimpleName(), true, true)
            r.toggleVR()
            while (!renderer!!.initialized || !renderer!!.firstImageReady) {
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
     * @param n
     * @param x
     * @param y
     * @param z
     * @param w
     */
    fun setRotation(n: Node, x: Float, y: Float, z: Float, w: Float) {
        n.rotation = Quaternion(x, y, z, w)
    }

    fun setScale(n: Node, x: Float, y: Float, z: Float) {
        n.scale = GLVector(x, y, z)
    }

    fun setColor(n: Node, x: Float, y: Float, z: Float, w: Float) {
        val col = GLVector(x, y, z, w)
        n.material.ambient = col
        n.material.diffuse = col
        n.material.specular = col
    }

    fun setPosition(n: Node, x: Float, y: Float, z: Float) {
        n.position = GLVector(x, y, z)
    }

    fun addWindowListener(wl: WindowListener?) {
        frame!!.addWindowListener(wl)
    }

    override fun axis(i: Int): CalibratedAxis {
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

    inner class TransparentSlider : JSlider() {
        override fun paintComponent(g: Graphics) {
            val g2d = g.create() as Graphics2D
            g2d.color = background
            g2d.composite = AlphaComposite.SrcOver.derive(0.9f)
            g2d.fillRect(0, 0, width, height)
            g2d.dispose()
            super.paintComponent(g)
        }

        init { // Important, we taking over the filling of the
// component...
            isOpaque = false
            background = Color.DARK_GRAY
            foreground = Color.LIGHT_GRAY
        }
    }

    internal inner class enableIncrease : ClickBehaviour {
        override fun click(x: Int, y: Int) {
            fPSSpeed = fPSSpeed + 0.5f
            mouseSpeed = mouseSpeed + 0.05f
            //log.debug( "Increasing FPS scroll Speed" );
            resetFPSInputs()
        }
    }

    internal inner class enableDecrease : ClickBehaviour {
        override fun click(x: Int, y: Int) {
            fPSSpeed = fPSSpeed - 0.1f
            mouseSpeed = mouseSpeed - 0.05f
            //log.debug( "Decreasing FPS scroll Speed" );
            resetFPSInputs()
        }
    }

    internal inner class showHelpDisplay : ClickBehaviour {
        override fun click(x: Int, y: Int) {
            val helpString = StringBuilder("SciView help:\n\n")
            for (trigger in inputHandler!!.getAllBindings().keys) {
                helpString.append(trigger).append("\t-\t").append(inputHandler!!.getAllBindings()[trigger]).append("\n")
            }
            // HACK: Make the console pop via stderr.
// Later, we will use a nicer dialog box or some such.
            log!!.warn(helpString.toString())
        }
    }

    /**
     * Return a list of all nodes that match a given predicate function
     * @param nodeMatchPredicate, returns true if a node is a match
     * @return
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
        @JvmField val DEFAULT_COLOR = Colors.LIGHTGRAY

        /**
         * Utility function to generate GLVector in cases like usage from Python
         * @param x
         * @param y
         * @param z
         * @return a GLVector of x,y,z
         */
        @JvmStatic fun getGLVector(x: Float, y: Float, z: Float): GLVector {
            return GLVector(x, y, z)
        }

        /**
         * Static launching method
         */
        @JvmStatic fun createSciView(): SciView {
            xinitThreads()
            System.setProperty("scijava.log.level:sc.iview", "debug")
            val context = Context(ImageJService::class.java, SciJavaService::class.java, SCIFIOService::class.java, ThreadService::class.java)
            val sciViewService = context.service(SciViewService::class.java)
            return sciViewService.orCreateActiveSciView
        }
    }
}
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
import bvv.core.VolumeViewerOptions
import dev.dirs.ProjectDirectories
import graphics.scenery.*
import graphics.scenery.Scene.RaycastResult
import graphics.scenery.backends.Renderer
import graphics.scenery.backends.vulkan.VulkanRenderer
import graphics.scenery.controls.InputHandler
import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerInput
import graphics.scenery.primitives.*
import graphics.scenery.proteins.Protein
import graphics.scenery.proteins.RibbonDiagram
import graphics.scenery.utils.ExtractsNatives
import graphics.scenery.utils.ExtractsNatives.Companion.getPlatform
import graphics.scenery.utils.LogbackUtils
import graphics.scenery.utils.SceneryPanel
import graphics.scenery.utils.Statistics
import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.Colormap
import graphics.scenery.volumes.RAIVolume
import graphics.scenery.volumes.Volume
import graphics.scenery.volumes.Volume.Companion.fromXML
import graphics.scenery.volumes.Volume.Companion.setupId
import graphics.scenery.volumes.Volume.VolumeDataSource.RAISource
import io.scif.SCIFIOService
import io.scif.services.DatasetIOService
import net.imagej.Dataset
import net.imagej.ImageJService
import net.imagej.axis.CalibratedAxis
import net.imagej.axis.DefaultAxisType
import net.imagej.axis.DefaultLinearAxis
import net.imagej.interval.CalibratedRealInterval
import net.imagej.lut.LUTService
import net.imagej.mesh.Mesh
import net.imagej.mesh.io.ply.PLYMeshIO
import net.imagej.mesh.io.stl.STLMeshIO
import net.imagej.units.UnitService
import net.imglib2.*
import net.imglib2.display.ColorTable
import net.imglib2.img.Img
import net.imglib2.img.array.ArrayImgs
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.type.numeric.ARGBType
import net.imglib2.type.numeric.NumericType
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.integer.UnsignedByteType
import net.imglib2.view.Views
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.Context
import org.scijava.`object`.ObjectService
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
import org.scijava.util.ColorRGB
import org.scijava.util.Colors
import org.scijava.util.VersionUtils
import sc.iview.event.NodeActivatedEvent
import sc.iview.event.NodeAddedEvent
import sc.iview.event.NodeChangedEvent
import sc.iview.event.NodeRemovedEvent
import sc.iview.process.MeshConverter
import sc.iview.ui.CustomPropertyUI
import sc.iview.ui.MainWindow
import sc.iview.ui.SwingMainWindow
import sc.iview.ui.TaskManager
import java.awt.event.WindowListener
import java.io.IOException
import java.net.URL
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.Future
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Predicate
import java.util.stream.Collectors
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashMap
import javax.swing.JOptionPane
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

    val sceneryPanel = arrayOf<SceneryPanel?>(null)

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

    lateinit var controls: Controls
    val targetArcball: AnimatedCenteringBeforeArcBallControl
        get() = controls.targetArcball

    val currentScene: Scene
        get() = scene

    /**
     * Geometry/Image information of scene
     */
    private lateinit var axes: Array<CalibratedAxis>

    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var menus: MenuService

    @Parameter
    private lateinit var io: IOService

    @Parameter
    private lateinit var eventService: EventService

    @Parameter
    private lateinit var lutService: LUTService

    @Parameter
    private lateinit var threadService: ThreadService

    @Parameter
    private lateinit var objectService: ObjectService

    @Parameter
    private lateinit var unitService: UnitService

    private lateinit var imageToVolumeMap: HashMap<Any, Volume>

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

    /*
     * Return the SciJava Display that contains SciView
     *//*
     * Set the SciJava Display
     */  var display: Display<*>? = null

    /**
     * List of available LUTs for caching
     */
    private var availableLUTs = LinkedHashMap<String, URL>()

    /**
     * Return the current SceneryJPanel. This is necessary for custom context menus
     * @return panel the current SceneryJPanel
     */
    var lights: ArrayList<PointLight>? = null
        private set
    private val notAbstractNode: Predicate<in Node> = Predicate { node: Node -> !(node is Camera || node is Light || node === floor) }
    var isClosed = false
        internal set

    private val notAbstractBranchingFunction = Function { node: Node -> node.children.stream().filter(notAbstractNode).collect(Collectors.toList()) }

    val taskManager = TaskManager()

    // If true, then when a new node is added to the scene, the camera will refocus on this node by default
    var centerOnNewNodes = false

    // If true, then when a new node is added the thread will block until the node is added to the scene. This is required for
    //   centerOnNewNodes
    var blockOnNewNodes = false
    private var headlight: PointLight? = null

    lateinit var mainWindow: MainWindow

    constructor(context: Context) : super("SciView", 1280, 720, false, context) {
        context.inject(this)
    }

    constructor(applicationName: String?, windowWidth: Int, windowHeight: Int) : super(applicationName!!, windowWidth, windowHeight, false)

    /**
     * Toggle video recording with scenery's video recording mechanism
     * Note: this video recording may skip frames because it is asynchronous
     */
    fun toggleRecordVideo() {
        toggleRecordVideo("sciview_" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss")), false)
    }

    /**
     * Toggle video recording with scenery's video recording mechanism
     * Note: this video recording may skip frames because it is asynchronous
     *
     * @param filename destination for saving video
     * @param overwrite should the file be replaced, otherwise a unique incrementing counter will be appended
     */
    fun toggleRecordVideo(filename: String?, overwrite: Boolean) {
        if (renderer is VulkanRenderer)
            (renderer as VulkanRenderer).recordMovie(filename!!, overwrite)
        else
            log.info("This renderer does not support video recording.")
    }

    /**
     * See [Controls.stashControls].
     */
    fun stashControls() {
        controls.stashControls()
    }

    /**
     * See [Controls.restoreControls] and [Controls.stashControls].
     */
    fun restoreControls() {
        controls.restoreControls()
    }

    internal fun setRenderer(newRenderer: Renderer) {
        renderer = newRenderer
    }


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

        imageToVolumeMap = HashMap<Any, Volume>()

        // Setup camera
        if (camera == null) {
            camera = DetachedHeadCamera()
            (camera as DetachedHeadCamera).position = Vector3f(0.0f, 1.65f, 0.0f)
            scene.addChild(camera as DetachedHeadCamera)
        }
        camera!!.spatial().position = Vector3f(0.0f, 1.65f, 5.0f)
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
            light.spatial().position = tetrahedron[i]!!.mul(25.0f)
            light.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
            light.intensity = 1.0f
            lights!!.add(light)
            //camera.addChild( light );
            scene.addChild(light)
        }

        // Make a headlight for the camera
        headlight = PointLight(150.0f)
        headlight!!.spatial().position = Vector3f(0f, 0f, -1f).mul(25.0f)
        headlight!!.emissionColor = Vector3f(1.0f, 1.0f, 1.0f)
        headlight!!.intensity = 0.5f
        headlight!!.name = "headlight"
        val lightSphere = Icosphere(1.0f, 2)
        headlight!!.addChild(lightSphere)
        lightSphere.material().diffuse = headlight!!.emissionColor
        lightSphere.material().specular = headlight!!.emissionColor
        lightSphere.material().ambient = headlight!!.emissionColor
        lightSphere.material().wireframe = true
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
        val logLevel = System.getProperty("scenery.LogLevel", "info")
        log.level = LogLevel.value(logLevel)
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
                if (launcherVersion <= nonWorkingVersion
                        && !java.lang.Boolean.parseBoolean(System.getProperty("sciview.DisableLauncherVersionCheck", "false"))) {
                    throw IllegalStateException("imagej-launcher version is outdated, please update your Fiji installation.")
                } else {
                    logger.info("imagej-launcher version bigger that non-working version ($versionString vs. 4.0.5), all good.")
                }
            }
        } catch (cnfe: ClassNotFoundException) {
            // Didn't find the launcher, so we're probably good.
            logger.info("imagej-launcher not found, not touching renderer preferences.")
        }

        animations = LinkedList()
        mainWindow = SwingMainWindow(this)
        controls = Controls(this)

        imageToVolumeMap = HashMap<Any, Volume>()
    }

    fun toggleSidebar(): Boolean {
        return mainWindow.toggleSidebar()

    }

    private fun initializeInterpreter() {
        mainWindow.initializeInterpreter()
    }

    /*
     * Completely close the SciView window + cleanup
     */
    fun closeWindow() {
        mainWindow.close()
        dispose()
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
    val sceneryInputHandler: InputHandler?
        get() = inputHandler

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
        if (branches.isEmpty()) {
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
            log.info("Cannot center on null node.")
            return
        }

        //center the on the same spot as ArcBall does
        centerOnPosition(currentNode.getMaximumBoundingBox().getBoundingSphere().origin)
    }

    /**
     * Center the camera on the specified Node
     */
    fun centerOnPosition(currentPos: Vector3f?) {
        controls.centerOnPosition(currentPos)
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
        return controls.getFPSSpeedSlow()
    }

    fun getFPSSpeedFast(): Float {
        return controls.getFPSSpeedFast()
    }

    fun getFPSSpeedVeryFast(): Float {
        return controls.getFPSSpeedVeryFast()
    }

    fun getMouseSpeed(): Float {
        return controls.getMouseSpeed()
    }

    fun getMouseScrollSpeed(): Float {
        return controls.getMouseScrollSpeed()
    }

    //a couple of setters with scene sensible boundary checks
    fun setFPSSpeedSlow(slowSpeed: Float) {
        controls.setFPSSpeedSlow(slowSpeed)
    }

    fun setFPSSpeedFast(fastSpeed: Float) {
        controls.setFPSSpeedFast(fastSpeed)
    }

    fun setFPSSpeedVeryFast(veryFastSpeed: Float) {
        controls.setFPSSpeedVeryFast(veryFastSpeed)
    }

    fun setFPSSpeed(newBaseSpeed: Float) {
        controls.setFPSSpeed(newBaseSpeed)
    }

    fun setMouseSpeed(newSpeed: Float) {
        controls.setMouseSpeed(newSpeed)
    }

    fun setMouseScrollSpeed(newSpeed: Float) {
        controls.setMouseScrollSpeed(newSpeed)
    }

    fun setObjectSelectionMode() {
        controls.setObjectSelectionMode()
    }

    /*
     * Set the action used during object selection
     */
    fun setObjectSelectionMode(selectAction: Function3<RaycastResult, Int, Int, Unit>?) {
        controls.setObjectSelectionMode(selectAction)
    }

    fun showContextNodeChooser(x: Int, y: Int) {
        mainWindow.showContextNodeChooser(x,y)
    }

    /*
     * Initial configuration of the scenery InputHandler
     * This is automatically called and should not be used directly
     */
    override fun inputSetup() {
        log.info("Running InputSetup")
        controls.inputSetup()
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
    fun addBox(position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f), size: Vector3f = Vector3f(1.0f, 1.0f, 1.0f), color: ColorRGB = DEFAULT_COLOR,
               inside: Boolean = false, block: Box.() -> Unit = {}): Box {
        val box = Box(size, inside)
        box.spatial().position = position
        box.material {
            ambient = Vector3f(1.0f, 0.0f, 0.0f)
            diffuse = Utils.convertToVector3f(color)
            specular = Vector3f(1.0f, 1.0f, 1.0f)
        }
        box.name = generateUniqueName("Box")
        return addNode(box, block = block)
    }

    /**
     * Return a Unique name (in terms of the scene) given a [candidateName].
     * @param candidateName a candidate name
     * @return a unique name based on [candidateName]
     */
    fun generateUniqueName(candidateName: String): String {
        var uniqueName = candidateName
        var counter = 1
        val names = allSceneNodes.map { el -> el.name }
        while (names.contains(uniqueName)) {
            uniqueName = "$candidateName $counter"
            counter++
        }
        return uniqueName
    }

    /**
     * Add a unit sphere at a given [position] with given [radius] and [color].
     * @return the Node corresponding to the sphere
     */
    @JvmOverloads
    fun addSphere(position: Vector3f = Vector3f(0.0f, 0.0f, 0.0f), radius: Float = 1f, color: ColorRGB = DEFAULT_COLOR, block: Sphere.() -> Unit = {}): Sphere {
        val sphere = Sphere(radius, 20)
        sphere.spatial().position = position
        sphere.material {
            ambient = Vector3f(1.0f, 0.0f, 0.0f)
            diffuse = Utils.convertToVector3f(color)
            specular = Vector3f(1.0f, 1.0f, 1.0f)
        }
        sphere.name = generateUniqueName("Sphere")
        return addNode(sphere, block = block)
    }

    /**
     * Add a Cylinder at the given position with radius, height, and number of faces/segments
     * @param position position of the cylinder
     * @param radius radius of the cylinder
     * @param height height of the cylinder
     * @param num_segments number of segments to represent the cylinder
     * @return  the Node corresponding to the cylinder
     */
    fun addCylinder(position: Vector3f, radius: Float, height: Float, color: ColorRGB = DEFAULT_COLOR, num_segments: Int, block: Cylinder.() -> Unit = {}): Cylinder {
        val cyl = Cylinder(radius, height, num_segments)
        cyl.spatial().position = position
        cyl.material {
            ambient = Vector3f(1.0f, 0.0f, 0.0f)
            diffuse = Utils.convertToVector3f(color)
            specular = Vector3f(1.0f, 1.0f, 1.0f)
        }
        cyl.name = generateUniqueName("Cylinder")
        return addNode(cyl, block = block)
    }

    /**
     * Add a Cone at the given position with radius, height, and number of faces/segments
     * @param position position to put the cone
     * @param radius radius of the cone
     * @param height height of the cone
     * @param num_segments number of segments used to represent cone
     * @return  the Node corresponding to the cone
     */
    fun addCone(position: Vector3f, radius: Float, height: Float, color: ColorRGB = DEFAULT_COLOR, num_segments: Int, block: Cone.() -> Unit = {}): Cone {
        val cone = Cone(radius, height, num_segments, Vector3f(0.0f, 0.0f, 1.0f))
        cone.spatial().position = position
        cone.material {
            ambient = Vector3f(1.0f, 0.0f, 0.0f)
            diffuse = Utils.convertToVector3f(color)
            specular = Vector3f(1.0f, 1.0f, 1.0f)
        }
        cone.name = generateUniqueName("Cone")
        return addNode(cone, block = block)
    }

    /**
     * Add a line from start to stop with the given color
     * @param start start position of line
     * @param stop stop position of line
     * @param color color of line
     * @return the Node corresponding to the line
     */
    @JvmOverloads
    fun addLine(start: Vector3f = Vector3f(0.0f, 0.0f, 0.0f), stop: Vector3f = Vector3f(1.0f, 1.0f, 1.0f), color: ColorRGB = DEFAULT_COLOR, block: Line.() -> Unit = {}): Line {
        return addLine(arrayOf(start, stop), color, 0.1, block)
    }

    /**
     * Add a multi-segment line that goes through the supplied points with a single color and edge width
     * @param points points along line including first and terminal points
     * @param color color of line
     * @param edgeWidth width of line segments
     * @return the Node corresponding to the line
     */
    @JvmOverloads
    fun addLine(points: Array<Vector3f>, color: ColorRGB, edgeWidth: Double, block: Line.() -> Unit = {}): Line {
        val line = Line(points.size)
        for (pt in points) {
            line.addPoint(pt)
        }
        line.edgeWidth = edgeWidth.toFloat()
        line.material {
            ambient = Vector3f(1.0f, 1.0f, 1.0f)
            diffuse = Utils.convertToVector3f(color)
            specular = Vector3f(1.0f, 1.0f, 1.0f)
        }
        line.spatial().position = points[0]
        line.name = generateUniqueName("Line")
        return addNode(line, block = block)
    }

    /**
     * Add a PointLight source at the origin
     * @return a Node corresponding to the PointLight
     */
    @JvmOverloads
    fun addPointLight(block: PointLight.() -> Unit = {}): PointLight {
        val light = PointLight(5.0f)
        light.material {
            ambient = Vector3f(1.0f, 0.0f, 0.0f)
            diffuse = Vector3f(0.0f, 1.0f, 0.0f)
            specular = Vector3f(1.0f, 1.0f, 1.0f)
        }
        light.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        lights!!.add(light)
        light.name = generateUniqueName("Point Light")
        return addNode(light, block = block)
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
            light.spatial().position = Vector3f(x, y, z)
        }
    }

    /**
     * Open a file specified by the source path. The file can be anything that SciView knows about: mesh, volume, point cloud
     * @param source string of a data source
     * @throws IOException
     */
    @Suppress("UNCHECKED_CAST")
    @Throws(IOException::class)
    fun open(source: String) {
        var name = source.split("/").last()

        if (source.endsWith(".xml", ignoreCase = true)) {
            val openedNode = fromXML(source, hub, VolumeViewerOptions())
            openedNode.name = generateUniqueName(name)
            addNode(openedNode)
            return
        } else if (source.takeLast(4).equals(".pdb", true)) {
            val protein = Protein.fromFile(source)
            val ribbon = RibbonDiagram(protein)
            ribbon.spatial().position = Vector3f(0f, 0f, 0f)
            ribbon.name = generateUniqueName(name)
            addNode(ribbon)
            return
        } else if (source.endsWith(".stl", ignoreCase = true)) {
            val stlReader = STLMeshIO()
            addMesh(stlReader.open(source), name=name)
            return
        } else if (source.endsWith(".ply", ignoreCase = true)) {
            val plyReader = PLYMeshIO()
            addMesh(plyReader.open(source), name=name)
            return
        }

        val data = io.open(source)
        when (data) {
            is Mesh -> addMesh(data, name=name)
            is PointCloud -> addPointCloud(data, name)
            is graphics.scenery.Mesh -> addMesh(data, name=name)
            is Dataset -> addVolume(data, floatArrayOf(1.0f, 1.0f, 1.0f))
//            is RandomAccessibleInterval<*> -> {
//                val t = data.randomAccess().get()
//                addVolume(data, source, floatArrayOf(1.0f, 1.0f, 1.0f))
//            }
            is List<*> -> {
                val list = data
                require(!list.isEmpty()) { "Data source '$source' appears empty." }
                val element = list[0]
                if (element is RealLocalizable) {
                    // NB: For now, we assume all elements will be RealLocalizable.
                    // Highly likely to be the case, barring antagonistic importers.
                    val points = list as List<RealLocalizable>
                    addPointCloud(points, name)
                } else {
                    val type = if (element == null) "<null>" else element.javaClass.name
                    throw IllegalArgumentException("Data source '" + source +  //
                            "' contains elements of unknown type '" + type + "'")
                }
            }
            else -> {
                val type = if (data == null) "<null>" else data.javaClass.name
                throw IllegalArgumentException("Data source '" + source +  //
                        "' contains data of unknown type '" + type + "'")
            }
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
                      name: String? = "PointCloud",
                      pointSize : Float = 1.0f,
                      block: PointCloud.() -> Unit = {}): PointCloud {
        val flatVerts = FloatArray(points.size * 3)
        var k = 0
        for (point in points) {
            flatVerts[k * 3] = point.getFloatPosition(0)
            flatVerts[k * 3 + 1] = point.getFloatPosition(1)
            flatVerts[k * 3 + 2] = point.getFloatPosition(2)
            k++
        }
        val pointCloud = PointCloud(pointSize, name!!)
        val vBuffer: FloatBuffer = BufferUtils.allocateFloat(flatVerts.size * 4)
        val nBuffer: FloatBuffer = BufferUtils.allocateFloat(0)
        vBuffer.put(flatVerts)
        vBuffer.flip()
        pointCloud.geometry().vertices = vBuffer
        pointCloud.geometry().normals = nBuffer
        pointCloud.geometry().indices = BufferUtils.allocateInt(0)
        pointCloud.spatial().position = Vector3f(0f, 0f, 0f)

        pointCloud.setupPointCloud()
        return addNode(pointCloud, block = block)
    }

    /**
     * Add a PointCloud to the scene
     * @param pointCloud existing PointCloud to add to scene
     * @return a Node corresponding to the PointCloud
     */
    @JvmOverloads
    fun addPointCloud(pointCloud: PointCloud,
                      name: String = "Point Cloud",
                      block: PointCloud.() -> Unit = {}): PointCloud {
        pointCloud.setupPointCloud()
        pointCloud.spatial().position = Vector3f(0f, 0f, 0f)
        pointCloud.name = generateUniqueName(name)
        return addNode(pointCloud, block = block)
    }

    /**
     * Add Node n to the scene and set it as the active node/publish it to the event service if activePublish is true.
     * @param n node to add to scene
     * @param activePublish flag to specify whether the node becomes active *and* is published in the inspector/services
     * @param block an optional code that will be executed as a part of adding the node
     * @param parent optional name of the parent node, default is the scene root
     * @return a Node corresponding to the Node
     */
    @JvmOverloads
    fun <N: Node?> addNode(n: N, activePublish: Boolean = true, block: N.() -> Unit = {}, parent: Node = scene): N {
        n?.let {
            it.block()
            // Ensure name is unique
            n.name = generateUniqueName(n.name)
            parent.addChild(it)
            objectService.addObject(n)
            if (blockOnNewNodes) {
                Utils.blockWhile({ this.find(n.name) == null }, 20)
                //System.out.println("find(name) " + find(n.getName()) );
            }
            // Set new node as active and centered?
            setActiveNode(n)
            if (centerOnNewNodes) {
                centerOnNode(n)
            }
            if (activePublish) {
                eventService.publish(NodeAddedEvent(n))
            }
        }
        return n
    }

    /**
     * Add Node n to the scene and set it as the active node/publish it to the event service if activePublish is true.
     * This is technically only a shortcut method (to the same named method) that has left out the 'block' parameter.
     * @param n node to add to scene
     * @param activePublish flag to specify whether the node becomes active *and* is published in the inspector/services
     * @param parent name of the parent node
     * @return a Node corresponding to the Node
     */
    @JvmOverloads
    fun <N: Node?> addNode(n: N, activePublish: Boolean = true, parent: Node): N {
        return addNode(n, activePublish, {}, parent)
    }

    /**
     * Make a node known to the services.
     * Used for nodes that are not created/added by a SciView controlled process e.g. [BoundingGrid].
     *
     * @param n node to add to publish
     */
    fun <N: Node> publishNode(n: N): N {
        n.let {
            objectService.addObject(n)
            eventService.publish(NodeAddedEvent(n))
            (mainWindow as SwingMainWindow).nodePropertyEditor.updateProperties(n, rebuild = true)
        }
        return n
    }

    /**
     * Add a scenery Mesh to the scene
     * @param scMesh scenery mesh to add to scene
     * @param name the name of the mesh
     * @return a Node corresponding to the mesh
     */
    fun addMesh(scMesh: graphics.scenery.Mesh, name: String ="Mesh"): graphics.scenery.Mesh {
        scMesh.ifMaterial {
            ambient = Vector3f(1.0f, 0.0f, 0.0f)
            diffuse = Vector3f(0.0f, 1.0f, 0.0f)
            specular = Vector3f(1.0f, 1.0f, 1.0f)
        }
        scMesh.spatial().position = Vector3f(0.0f, 0.0f, 0.0f)
        scMesh.name = generateUniqueName(name)
        objectService.addObject(scMesh)
        return addNode(scMesh)
    }

    /**
     * Add a scenery Mesh to the scene
     * @param scMesh scenery mesh to add to scene
     * @return a Node corresponding to the mesh
     */
    fun addMesh(scMesh: graphics.scenery.Mesh): graphics.scenery.Mesh {
        return addMesh(scMesh, "Mesh")
    }


    /**
     * Add an ImageJ mesh to the scene
     * @param mesh net.imagej.mesh to add to scene
     * @param name the name of the mesh
     * @return a Node corresponding to the mesh
     */
    fun addMesh(mesh: Mesh, name: String="Mesh"): graphics.scenery.Mesh {
        val scMesh = MeshConverter.toScenery(mesh)
        return addMesh(scMesh, name=name)
    }

    /**
     * Add an ImageJ mesh to the scene
     * @param mesh net.imagej.mesh to add to scene
     * @return a Node corresponding to the mesh
     */
    fun addMesh(mesh: Mesh): graphics.scenery.Mesh {
        return addMesh(mesh, "Mesh")
    }

    /**
     * [Deprecated: use deleteNode]
     * Remove a Mesh from the scene
     * @param scMesh mesh to remove from scene
     */
    @Deprecated("Please use SciView.deleteNode() instead.", replaceWith = ReplaceWith("SciView.deleteNode()"))
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
        mainWindow.selectNode(activeNode)
        eventService.publish(NodeActivatedEvent(activeNode))
        return activeNode
    }

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeAdded(event: NodeAddedEvent?) {
        mainWindow.rebuildSceneTree()
    }

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeRemoved(event: NodeRemovedEvent?) {
        mainWindow.rebuildSceneTree()
    }

    @Suppress("UNUSED_PARAMETER")
    @EventHandler
    protected fun onNodeChanged(event: NodeChangedEvent?) {
        // TODO: Check if rebuilding the tree is necessary here, otherwise this costs a lot of performance
        //mainWindow.rebuildSceneTree()
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
        val thread = threadService.run {
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
    fun takeScreenshot(path: String?, overwrite: Boolean = false) {
        renderer!!.screenshot(path!!, overwrite = overwrite)
    }

    /**
     * Take a screenshot and return it as an Img
     * @return an Img of type UnsignedByteType
     */
    val screenshot: Img<UnsignedByteType>
        get() {
            val screenshot = getSceneryRenderer()?.requestScreenshot() ?: throw IllegalStateException("No renderer present, cannot create screenshot")
            return ArrayImgs.unsignedBytes(screenshot.data!!, screenshot.width.toLong(), screenshot.height.toLong(), 4L)
        }

    /**
     * Take a screenshot and return it as an Img
     * @return an Img of type UnsignedByteType
     */
    val aRGBScreenshot: Img<ARGBType>
        get() {
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
    fun deleteActiveNode(askUser: Boolean = false) {
        if(askUser && activeNode != null){
            val options = arrayOf("Cancel", "Delete ${activeNode!!.name}")
            val x = JOptionPane.showOptionDialog(
                null, "Please confirm delete of ${activeNode!!.name}? ",
                "Delete confirm",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[1]
            )
            if (x == 0){
                return
            }
        }
        deleteNode(activeNode)
    }

    /**
     * Delete a specified node and control whether the event is published
     * @param node node to delete from scene
     * @param activePublish whether the deletion should be published
     */
    @JvmOverloads
    fun deleteNode(node: Node?, activePublish: Boolean = true) {
        if(node is Volume) {
            node.volumeManager.remove(node)
            val toRemove = ArrayList<Any>()
            for( entry in imageToVolumeMap.entries ) {
                if( entry.value == node ) {
                    toRemove.add(entry.key)
                }
            }
            for(entry in toRemove) {
                imageToVolumeMap.remove(entry)
            }
        }

        for (child in node!!.children) {
            deleteNode(child, activePublish)
        }
        objectService.removeObject(node)
        node.parent?.removeChild(node)
        if (activeNode == node) {
            setActiveNode(null)
        }
        //maintain consistency
        if( activePublish ) {
            eventService.publish(NodeRemovedEvent(node))
        }
    }

    /**
     * Dispose the current scenery renderer, hub, and other scenery things
     */
    fun dispose() {
        val objs: List<Node> = objectService.getObjects(Node::class.java)
        for (obj in objs) {
            deleteNode(obj, activePublish = false)
        }
        scijavaContext!!.service(SciViewService::class.java).close(this)
        close()
        // if scijavaContext was not created by ImageJ, then system exit
        if( objectService.getObjects(Utils.SciviewStandalone::class.java).size > 0 ) {
            log.info("Was running as sciview standalone, shutting down JVM")
            System.exit(0)
        }
    }

    override fun close() {
        super.close()
    }

    /**
     * Move the current active camera to the specified position
     * @param position position to move the camera to
     */
    fun moveCamera(position: FloatArray) {
        camera?.spatial()?.position = Vector3f(position[0], position[1], position[2])
    }

    /**
     * Move the current active camera to the specified position
     * @param position position to move the camera to
     */
    fun moveCamera(position: DoubleArray) {
        camera?.spatial()?.position = Vector3f(position[0].toFloat(), position[1].toFloat(), position[2].toFloat())
    }

    /**
     * Get the current application name
     * @return a String of the application name
     */
    fun getName(): String {
        return applicationName
    }

    /**
     * [Deprecated: use addNode]
     * Add a child to the scene. you probably want addNode
     * @param node node to add as a child to the scene
     */
    @Deprecated("Please use SciView.addNode() instead.", replaceWith = ReplaceWith("SciView.addNode()"))
    fun addChild(node: Node) {
        scene.addChild(node)
    }

    /**
     * Set the colormap using an ImageJ LUT name
     * @param n node to apply colormap to
     * @param lutName name of LUT according to imagej LUTService
     */
    fun setColormap(n: Node, lutName: String) {
        try {
            n.metadata["sciview.colormapName"] = lutName
            setColormap(n, lutService.loadLUT(lutService.findLUTs()[lutName]))
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
            n.geometryOrNull()?.dirty = true
            n.spatial().needsUpdate = true
        }
    }

    /**
     * Adds a SourceAndConverter to the scene.
     *
     * @param sac The SourceAndConverter to add
     * @param name Name of the dataset
     * @param voxelDimensions Array with voxel dimensions.
     * @param <T> Type of the dataset.
     * @return THe node corresponding to the volume just added.
    </T> */
    @JvmOverloads
    fun <T : RealType<T>> addVolume(sac: SourceAndConverter<T>,
                                    numTimepoints: Int,
                                    name: String = "Volume",
                                    voxelDimensions: FloatArray,
                                    block: Volume.() -> Unit = {}): Volume {
        val sources: MutableList<SourceAndConverter<T>> = ArrayList()
        sources.add(sac)

        val v = addVolume(sources, numTimepoints, name, voxelDimensions, block = block)
        imageToVolumeMap[sources] = v
        imageToVolumeMap[sac] = v
        return v
    }

    /**
     * Adds a [Dataset]-backed [Volume] to the scene.
     *
     * @param image The [Dataset] to add.
     * @param voxelDimensions An array containing the relative voxel dimensions of the dataset.
     * @param block A lambda with additional code to execute upon adding the volume.
     * @return The volume node corresponding to the [Dataset]-backed volume that has just been added.
     */
    fun addVolume(
            image: Dataset,
            voxelDimensions: FloatArray? = null,
            block: Volume.() -> Unit = {}
    ): Volume {
        return addVolume(image, "Volume", voxelDimensions, block)
    }

    /**
     * Returns the Dataset's scale/resolution converted to sciview units
     *
     * @param image the [Dataset]
     * @return A [FloatArray] representing the image's 3D scale in sciview space
     */
    fun getSciviewScale(image: Dataset): FloatArray {
        val voxelDims = FloatArray(3)
        var axisNames = arrayOf("X", "Y", "Z")
        for (axisIdx in voxelDims.indices) {
            var d = (0 until image.numDimensions()).filter { idx -> image.axis(idx).type().label == axisNames[axisIdx] }.first()
            val inValue = image.axis(d).averageScale(0.0, 1.0)
            if (image.axis(d).unit() == null || d >= axes.size) {
                voxelDims[axisIdx] = inValue.toFloat()
            } else {
                val imageAxisUnit = image.axis(d).unit().replace("µ", "u")
                val sciviewAxisUnit = axis(axisIdx)!!.unit().replace("µ", "u")

                voxelDims[axisIdx] = unitService.value(inValue, imageAxisUnit, sciviewAxisUnit).toFloat()
            }
        }
        return voxelDims
    }

    /**
     * Adds a [Dataset]-backed [Volume] to the scene.
     *
     * @param image The [Dataset] to add.
     * @param name The name of the [Volume].
     * @param voxelDimensions An array containing the relative voxel dimensions of the dataset.
     * @param block A lambda with additional code to execute upon adding the volume.
     * @return The volume node corresponding to the [Dataset]-backed volume that has just been added.
     */
    fun addVolume(
        image: Dataset,
        name: String = "Volume",
        voxelDimensions: FloatArray? = null,
        block: Volume.() -> Unit = {}
    ): Volume {
        return if(voxelDimensions == null) {
            val voxelDims = getSciviewScale(image)
            addVolume(image.imgPlus, image.name, voxelDims, block)
        } else {
            addVolume(image.imgPlus, image.name, voxelDimensions, block)
        }
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
    @JvmOverloads
    fun <T : RealType<T>> addVolume(
        image: RandomAccessibleInterval<T>,
        name: String = "Volume",
        voxelDimensions: FloatArray = floatArrayOf(1.0f, 1.0f, 1.0f),
        block: Volume.() -> Unit = {}
    ): Volume {
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

        val v = addVolume(sources, numTimepoints, name, voxelDimensions, block = block)
        v.metadata["RandomAccessibleInterval"] = image
        imageToVolumeMap[image] = v
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
    @JvmOverloads
    @Suppress("UNCHECKED_CAST")
    fun <T : RealType<T>> addVolume(sources: List<SourceAndConverter<T>>,
                                    converterSetups: ArrayList<ConverterSetup>,
                                    numTimepoints: Int,
                                    name: String = "Volume",
                                    voxelDimensions: FloatArray,
                                    block: Volume.() -> Unit = {},
                                    colormapName: String = "Fire.lut"): Volume {
        var timepoints = numTimepoints
        var cacheControl: CacheControl? = null

        val image = sources[0].spimSource.getSource(0, 0)
        if (image is VolatileView<*, *>) {
            val viewData = (image as VolatileView<T, Volatile<T>>).volatileViewData
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

        val ds = if(converterSetups != null) {
            RAISource<T>(voxelType, sources, converterSetups, timepoints, cacheControl)
        } else {
            val cs = ArrayList<ConverterSetup>()
            for ((setupId, source) in sources.withIndex()) {
                cs.add(BigDataViewer.createConverterSetup(source, setupId))
            }

            RAISource<T>(voxelType, sources, cs, timepoints, cacheControl)
        }

        val options = VolumeViewerOptions()
        val v: Volume = RAIVolume(ds, options, hub)
        // Note we override scenery's default scale of mm
        // v.pixelToWorldRatio = 0.1f
        v.name = name
        v.metadata["sources"] = sources
        v.metadata["VoxelDimensions"] = voxelDimensions
        v.spatial().scale = Vector3f(voxelDimensions[0], voxelDimensions[1], voxelDimensions[2]) * v.pixelToWorldRatio
        val tf = v.transferFunction
        val rampMin = 0f
        val rampMax = 0.1f
        tf.clear()
        tf.addControlPoint(0.0f, 0.0f)
        tf.addControlPoint(rampMin, 0.0f)
        tf.addControlPoint(1.0f, rampMax)
        val bg = BoundingGrid()
        bg.node = v

        // Set default colormap
        v.metadata["sciview.colormapName"] = colormapName
        v.colormap = Colormap.fromColorTable(getLUT(colormapName))

        imageToVolumeMap[image] = v
        return addNode(v, block = block)
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
    @JvmOverloads
    fun <T : RealType<T>> addVolume(sources: List<SourceAndConverter<T>>,
                                    numTimepoints: Int,
                                    name: String = "Volume",
                                    voxelDimensions: FloatArray,
                                    block: Volume.() -> Unit = {}): Volume {
        var setupId = 0
        val converterSetups = ArrayList<ConverterSetup>()
        for (source in sources) {
            converterSetups.add(BigDataViewer.createConverterSetup(source, setupId++))
        }
        val v = addVolume(sources, converterSetups, numTimepoints, name, voxelDimensions, block = block)
        imageToVolumeMap[sources] = v
        return v
    }

    /**
     * Get the Volume that corresponds to an image if one exists
     * @param image an image of any type (e.g. IterableInterval, RAI, SourceAndConverter)
     * @return a Volume corresponding to the input image
    </T> */
    fun getVolumeFromImage(image: Any): Volume? {
        if( image in imageToVolumeMap )
            return imageToVolumeMap[image]
        return null
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
                                       voxelDimensions: FloatArray, v: Volume): Volume {
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
        v.spatial().needsUpdate = true
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
        n.spatialOrNull()?.rotation = Quaternionf(x, y, z, w)
    }

    fun setScale(n: Node, x: Float, y: Float, z: Float) {
        n.spatialOrNull()?.scale = Vector3f(x, y, z)
    }

    @Suppress("UNUSED_PARAMETER")
    fun setColor(n: Node, x: Float, y: Float, z: Float, w: Float) {
        val col = Vector3f(x, y, z)
        n.ifMaterial {
            ambient = col
            diffuse = col
            specular = col
        }
    }

    fun setPosition(n: Node, x: Float, y: Float, z: Float) {
        n.spatialOrNull()?.position = Vector3f(x, y, z)
    }

    fun addWindowListener(wl: WindowListener?) {
        (mainWindow as? SwingMainWindow)?.addWindowListener(wl)
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

    /**
     * Triggers the inspector tree to be completely rebuilt/refreshed.
     */
    fun requestPropEditorRefresh() {
        eventService.publish(NodeChangedEvent(scene))
    }

    /**
     * Triggers the inspector to rebuild/refresh the given node.
     * @param n Root of the subtree to get rebuilt/refreshed.
     */
    fun requestPropEditorRefresh(n: Node?) {
        eventService.publish(NodeChangedEvent(n))
    }

    fun attachCustomPropertyUIToNode(node: Node, ui: CustomPropertyUI) {
        node.metadata["sciview-inspector-${ui.module.info.name}"] = ui
    }

    fun getAvailableServices() {
        println(scijavaContext!!.serviceIndex)
    }

    /**
     * Return the color table corresponding to the [lutName]
     * @param lutName a String represening an ImageJ style LUT name, like Fire.lut
     * @return a [ColorTable] corresponding to the LUT or null if LUT not available
     */
    fun getLUT(lutName: String = "Fire.lut"): ColorTable {
        try {
            refreshLUTs()
            var lutResult = availableLUTs[lutName]
            return lutService.loadLUT(lutResult)
        } catch (e: IOException) {
            log.error("LUT $lutName not available")
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Refresh the cache of available LUTs fetched from LutService
     */
    private fun refreshLUTs() {
        if(availableLUTs.isEmpty()) {
            availableLUTs.putAll(lutService.findLUTs().entries.sortedBy { it.key }.map { it.key to it.value })
        }
    }

    /**
     * Return a list of available LUTs/colormaps
     * @return a list of LUT names
     */
    fun getAvailableLUTs(): List<String> {
        refreshLUTs()
        return availableLUTs.map {entry -> entry.key}
    }

    /**
     * Return ProjectDirectories for sciview.
     *
     * Use this to find the location of cache, etc.
     *
     * @return a [ProjectDirectories] for sciview
     */
    fun getProjectDirectories(): ProjectDirectories {
        return ProjectDirectories.from("sc", "iview", "sciview")
    }

    companion object {
        //bounds for the controls
        const val FPSSPEED_MINBOUND_SLOW = 0.01f
        const val FPSSPEED_MAXBOUND_SLOW = 30.0f
        const val FPSSPEED_MINBOUND_FAST = 0.2f
        const val FPSSPEED_MAXBOUND_FAST = 600f
        const val FPSSPEED_MINBOUND_VERYFAST = 10f
        const val FPSSPEED_MAXBOUND_VERYFAST = 2000f

        const val MOUSESPEED_MINBOUND = 0.1f
        const val MOUSESPEED_MAXBOUND = 3.0f
        const val MOUSESCROLL_MINBOUND = 0.3f
        const val MOUSESCROLL_MAXBOUND = 10.0f

        @JvmField
        val DEFAULT_COLOR: ColorRGB = Colors.LIGHTGRAY

        /**
         * Static launching method
         *
         * @return a newly created SciView
         */
        @JvmStatic
        @Throws(Exception::class)
        fun create(): SciView {
            xinitThreads()
            val context = Context(ImageJService::class.java, SciJavaService::class.java, SCIFIOService::class.java,
                ThreadService::class.java, ObjectService::class.java, LogService::class.java, MenuService::class.java,
                IOService::class.java, EventService::class.java, LUTService::class.java, UnitService::class.java,
                DatasetIOService::class.java)
            val objectService = context.getService(ObjectService::class.java)
            objectService.addObject(Utils.SciviewStandalone())
            val sciViewService = context.service(SciViewService::class.java)
            return sciViewService.orCreateActiveSciView
        }

        /**
         * Static launching method
         * DEPRECATED use SciView.create() instead
         *
         * @return a newly created SciView
         */
        @Deprecated("Please use SciView.create() instead.", replaceWith = ReplaceWith("SciView.create()"))
        @Throws(Exception::class)
        fun createSciView(): SciView {
            return create()
        }
    }
}

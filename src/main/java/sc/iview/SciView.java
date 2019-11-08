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
package sc.iview;

import cleargl.GLTypeEnum;
import cleargl.GLVector;
import com.bulenkov.darcula.DarculaLaf;
import com.jogamp.opengl.math.Quaternion;
import coremem.enums.NativeTypeEnum;
import graphics.scenery.Box;
import graphics.scenery.*;
import graphics.scenery.backends.RenderedImage;
import graphics.scenery.backends.Renderer;
import graphics.scenery.backends.opengl.OpenGLRenderer;
import graphics.scenery.backends.vulkan.VulkanRenderer;
import graphics.scenery.controls.InputHandler;
import graphics.scenery.controls.OpenVRHMD;
import graphics.scenery.controls.TrackerInput;
import graphics.scenery.controls.behaviours.ArcballCameraControl;
import graphics.scenery.controls.behaviours.FPSCameraControl;
import graphics.scenery.controls.behaviours.MovementCommand;
import graphics.scenery.controls.behaviours.SelectCommand;
import graphics.scenery.utils.*;
import graphics.scenery.volumes.TransferFunction;
import graphics.scenery.volumes.Volume;
import graphics.scenery.volumes.bdv.BDVVolume;
import io.scif.SCIFIOService;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.imagej.Dataset;
import net.imagej.ImageJService;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.DefaultAxisType;
import net.imagej.axis.DefaultLinearAxis;
import net.imagej.interval.CalibratedRealInterval;
import net.imagej.lut.LUTService;
import net.imagej.ops.OpService;
import net.imagej.units.UnitService;
import net.imglib2.Cursor;
import net.imglib2.*;
import net.imglib2.display.ColorTable;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.type.volatiles.VolatileFloatType;
import net.imglib2.type.volatiles.VolatileUnsignedByteType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.io.IOService;
import org.scijava.log.LogLevel;
import org.scijava.log.LogService;
import org.scijava.menu.MenuService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.service.SciJavaService;
import org.scijava.thread.ThreadService;
import org.scijava.ui.UIService;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.swing.menu.SwingJMenuBarCreator;
import org.scijava.util.ColorRGB;
import org.scijava.util.Colors;
import org.scijava.util.VersionUtils;
import sc.iview.commands.view.NodePropertyEditor;
import sc.iview.controls.behaviours.CameraTranslateControl;
import sc.iview.controls.behaviours.NodeTranslateControl;
import sc.iview.event.NodeActivatedEvent;
import sc.iview.event.NodeAddedEvent;
import sc.iview.event.NodeChangedEvent;
import sc.iview.event.NodeRemovedEvent;
import sc.iview.process.MeshConverter;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;
import tpietzsch.example2.VolumeViewerOptions;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.basic.BasicLookAndFeel;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// we suppress unused warnings here because @Parameter-annotated fields
// get updated automatically by SciJava.
@SuppressWarnings({"unused", "WeakerAccess"})
public class SciView extends SceneryBase implements CalibratedRealInterval<CalibratedAxis> {

    public static final ColorRGB DEFAULT_COLOR = Colors.LIGHTGRAY;
    private final SceneryPanel[] sceneryPanel = { null };
    /**
     * Mouse controls for FPS movement and Arcball rotation
     */
    protected ArcballCameraControl targetArcball;
    protected FPSCameraControl fpsControl;
    /**
     * The floor that orients the user in the scene
     */
    protected Node floor;
    protected boolean vrActive = false;
    /**
     * The primary camera/observer in the scene
     */
    Camera camera = null;
    /**
     * Geometry/Image information of scene
     */
    private CalibratedAxis[] axes;

    @Parameter
    private LogService log;
    @Parameter
    private MenuService menus;
    @Parameter
    private IOService io;
    @Parameter
    private OpService ops;
    @Parameter
    private EventService eventService;
    @Parameter
    private DisplayService displayService;
    @Parameter
    private LUTService lutService;
    @Parameter
    private ThreadService threadService;
    @Parameter
    private ObjectService objectService;
    @Parameter
    private UnitService unitService;
    /**
     * Queue keeps track of the currently running animations
     **/
    private Queue<Future> animations;
    /**
     * Animation pause tracking
     **/
    private boolean animating;
    /**
     * This tracks the actively selected Node in the scene
     */
    private Node activeNode = null;
    /**
     * Speeds for input controls
     */
    private float fpsScrollSpeed = 0.05f;
    private float mouseSpeedMult = 0.25f;
    private Display<?> scijavaDisplay;
    private JLabel splashLabel;
    private SceneryJPanel panel;
    private JSplitPane mainSplitPane;
    private JSplitPane inspector;
    private NodePropertyEditor nodePropertyEditor;
    private ArrayList<PointLight> lights;
    private Stack<HashMap<String, Object>> controlStack;
    private JFrame frame;
    private Predicate<? super Node> notAbstractNode = (Predicate<Node>) node -> !( (node instanceof Camera) || (node instanceof Light) || (node==getFloor()));
    private boolean isClosed = false;
    private Function<Node,List<Node>> notAbstractBranchingFunction = node -> node.getChildren().stream().filter(notAbstractNode).collect(Collectors.toList());

    public SciView( Context context ) {
        super( "SciView", 1280, 720, false, context );
        context.inject( this );
    }

    public SciView( String applicationName, int windowWidth, int windowHeight ) {
        super( applicationName, windowWidth, windowHeight, false );
    }

    public boolean isClosed() {
        return isClosed;
    }

    public InputHandler publicGetInputHandler() {
        return getInputHandler();
    }

    /**
     * Toggle video recording with scenery's video recording mechanism
     * Note: this video recording may skip frames because it is asynchronous
     */
    public void toggleRecordVideo() {
        if( getRenderer() instanceof  OpenGLRenderer )
            ((OpenGLRenderer)getRenderer()).recordMovie();
        else
            ((VulkanRenderer)getRenderer()).recordMovie();
    }

    /**
     * This pushes the current input setup onto a stack that allows them to be restored with restoreControls
     */
    public void stashControls() {
        HashMap<String, Object> controlState = new HashMap<String, Object>();
        controlStack.push(controlState);
    }

    /**
     * This pops/restores the previously stashed controls. Emits a warning if there are no stashed controls
     */
    public void restoreControls() {
        HashMap<String, Object> controlState = controlStack.pop();

        // This isnt how it should work
        setObjectSelectionMode();
        resetFPSInputs();
    }

    /**
     * Place the camera such that all objects in the scene are within the field of view
     */
    public void fitCameraToScene() {
        centerOnNode(getScene());
    }

    /**
     * Reset the scene to initial conditions
     */
    public void reset() {
        // Initialize the 3D axes
        axes = new CalibratedAxis[3];

        axes[0] = new DefaultLinearAxis(new DefaultAxisType("X", true), "um", 1);
        axes[1] = new DefaultLinearAxis(new DefaultAxisType("Y", true), "um", 1);
        axes[2] = new DefaultLinearAxis(new DefaultAxisType("Z", true), "um", 1);

        // Remove everything except camera
        Node[] toRemove = getSceneNodes( n -> !( n instanceof Camera ) );
        for( Node n : toRemove ) {
            deleteNode(n, false);
        }

        // Add initial objects
        GLVector[] tetrahedron = new GLVector[4];
        tetrahedron[0] = new GLVector( 1.0f, 0f, -1.0f/(float)Math.sqrt(2.0f) );
        tetrahedron[1] = new GLVector( -1.0f,0f,-1.0f/(float)Math.sqrt(2.0) );
        tetrahedron[2] = new GLVector( 0.0f,1.0f,1.0f/(float)Math.sqrt(2.0) );
        tetrahedron[3] = new GLVector( 0.0f,-1.0f,1.0f/(float)Math.sqrt(2.0) );

        lights = new ArrayList<PointLight>();

        for( int i = 0; i < 4; i++ ) {// TODO allow # initial lights to be customizable?
            PointLight light = new PointLight(150.0f);
            light.setPosition( tetrahedron[i].times(25.0f) );
            light.setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
            light.setIntensity( 1.0f );
            lights.add( light );
            getScene().addChild( light );
        }

        Camera cam;
        if( getCamera() == null ) {
            cam = new DetachedHeadCamera();
            this.camera = cam;
            getScene().addChild( cam );
        } else {
            cam = getCamera();
        }
        cam.setPosition( new GLVector( 0.0f, 5.0f, 5.0f ) );
        cam.perspectiveCamera( 50.0f, getWindowWidth(), getWindowHeight(), 0.1f, 1000.0f );
        cam.setActive( true );

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setName( "Floor" );
        floor.setPosition( new GLVector( 0f, -1f, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );
    }

    /**
     * Initialization of SWING and scenery. Also triggers an initial population of lights/camera in the scene
     */
    @SuppressWarnings("restriction") @Override public void init() {
        if(Boolean.parseBoolean(System.getProperty("sciview.useDarcula", "false"))) {
            try {
                BasicLookAndFeel darcula = new DarculaLaf();
                UIManager.setLookAndFeel(darcula);
            } catch (Exception e) {
                getLogger().info("Could not load Darcula Look and Feel");
            }
        }

        log.setLevel(LogLevel.WARN);

        LogbackUtils.setLogLevel(null, System.getProperty("scenery.LogLevel", "info"));

        // determine imagej-launcher version and to disable Vulkan if XInitThreads() fix
        // is not deployed
        try {
            final Class<?> launcherClass = Class.forName("net.imagej.launcher.ClassLauncher");
            String versionString = VersionUtils.getVersion(launcherClass);

            if (versionString != null && ExtractsNatives.Companion.getPlatform() == ExtractsNatives.Platform.LINUX) {
                versionString = versionString.substring(0, 5);

                final Version launcherVersion = new Version(versionString);
                final Version nonWorkingVersion = new Version("4.0.5");

                if (launcherVersion.compareTo(nonWorkingVersion) <= 0
                        && !Boolean.parseBoolean(System.getProperty("sciview.DisableLauncherVersionCheck", "false"))) {
                    getLogger().info("imagej-launcher version smaller or equal to non-working version (" + versionString + " vs. 4.0.5), disabling Vulkan as rendering backend. Disable check by setting 'scenery.DisableLauncherVersionCheck' system property to 'true'.");
                    System.setProperty("scenery.Renderer", "OpenGLRenderer");
                } else {
                    getLogger().info("imagej-launcher version bigger that non-working version (" + versionString + " vs. 4.0.5), all good.");
                }
            }
        } catch (ClassNotFoundException cnfe) {
            // Didn't find the launcher, so we're probably good.
            getLogger().info("imagej-launcher not found, not touching renderer preferences.");
        }

        int x, y;

        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            x = screenSize.width/2 - getWindowWidth()/2;
            y = screenSize.height/2 - getWindowHeight()/2;
        } catch(HeadlessException e) {
            x = 10;
            y = 10;
        }

        frame = new JFrame("SciView");
        frame.setLayout(new BorderLayout(0, 0));
        frame.setSize(getWindowWidth(), getWindowHeight());
        frame.setLocation(x, y);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        nodePropertyEditor = new NodePropertyEditor( this );

        final JPanel p = new JPanel(new BorderLayout(0, 0));
        panel = new SceneryJPanel();
        JPopupMenu.setDefaultLightWeightPopupEnabled(false);
        final JMenuBar swingMenuBar = new JMenuBar();
        new SwingJMenuBarCreator().createMenus(menus.getMenu("SciView"), swingMenuBar);
        frame.setJMenuBar(swingMenuBar);

//        frame.addComponentListener(new ComponentAdapter() {
//            @Override
//            public void componentResized(ComponentEvent componentEvent) {
//                super.componentResized(componentEvent);
//                panel.setSize(componentEvent.getComponent().getWidth(), componentEvent.getComponent().getHeight());
//            }
//        });

        BufferedImage splashImage;
        try {
            splashImage = ImageIO.read(this.getClass().getResourceAsStream("sciview-logo.png"));
        } catch (IOException e) {
            getLogger().warn("Could not read splash image 'sciview-logo.png'");
            splashImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        }

        final String sceneryVersion = SceneryBase.class.getPackage().getImplementationVersion();
        final String sciviewVersion = SciView.class.getPackage().getImplementationVersion();
        final String versionString;

        if(sceneryVersion == null || sciviewVersion == null) {
            versionString = "";
        } else {
            versionString = "\n\nsciview " + sciviewVersion + " / scenery " + sceneryVersion;
        }

        splashLabel = new JLabel(versionString,
                new ImageIcon(splashImage.getScaledInstance(500, 200, java.awt.Image.SCALE_SMOOTH)),
                SwingConstants.CENTER);
        splashLabel.setBackground(new java.awt.Color(50, 48, 47));
        splashLabel.setForeground(new java.awt.Color(78, 76, 75));
        splashLabel.setOpaque(true);
        splashLabel.setVerticalTextPosition(JLabel.BOTTOM);
        splashLabel.setHorizontalTextPosition(JLabel.CENTER);

        p.setLayout(new OverlayLayout(p));
        p.setBackground(new java.awt.Color(50, 48, 47));
        p.add(panel, BorderLayout.CENTER);
        panel.setVisible(true);

        nodePropertyEditor.getComponent(); // Initialize node property panel

        JTree inspectorTree = nodePropertyEditor.getTree();
        inspectorTree.setToggleClickCount(0);// This disables expanding menus on double click
        JPanel inspectorProperties = nodePropertyEditor.getProps();

        inspector = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
                new JScrollPane( inspectorTree ),
                new JScrollPane( inspectorProperties ));
        inspector.setDividerLocation( getWindowHeight() / 3 );
        inspector.setContinuousLayout(true);
        inspector.setBorder(BorderFactory.createEmptyBorder());

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, //
                p,
                inspector
        );
        mainSplitPane.setDividerLocation( getWindowWidth()/3 * 2 );
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder());

        frame.add(mainSplitPane, BorderLayout.CENTER);

        SciView sciView = this;
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                getLogger().debug("Closing SciView window.");
                close();
                getScijavaContext().service(SciViewService.class).close(sciView);
                isClosed = true;
            }
        });

        frame.setGlassPane(splashLabel);
        frame.getGlassPane().setVisible(true);
//            frame.getGlassPane().setBackground(new java.awt.Color(50, 48, 47, 255));
        frame.setVisible(true);

        sceneryPanel[0] = panel;

        setRenderer( Renderer.createRenderer( getHub(), getApplicationName(), getScene(),
                getWindowWidth(), getWindowHeight(),
                sceneryPanel[0]) );

        // Enable push rendering by default
        getRenderer().setPushMode( true );

        getHub().add( SceneryElement.Renderer, getRenderer() );

        reset();

        animations = new LinkedList<>();
        controlStack = new Stack<>();

        SwingUtilities.invokeLater(() -> {
            try {
                while (!getSceneryRenderer().getFirstImageReady()) {
                    getLogger().debug("Waiting for renderer initialisation");
                    Thread.sleep(300);
                }

                Thread.sleep(200);
            } catch (InterruptedException e) {
                getLogger().error("Renderer construction interrupted.");
            }

            nodePropertyEditor.rebuildTree();
            frame.getGlassPane().setVisible(false);
            getLogger().info("Done initializing SciView");

            // subscribe to Node{Added, Removed, Changed} events
            eventService.subscribe(this);
        });

        // install hook to keep inspector updated on external changes (scripting, etc)
        getScene().getOnNodePropertiesChanged().put("updateInspector",
                node -> {
                    if( node == nodePropertyEditor.getCurrentNode() ) {
                        nodePropertyEditor.updateProperties(node);
                    }
                    return null;
                });
    }

    /*
     * Completely close the SciView window + cleanup
     */
    public void closeWindow() {
        frame.dispose();
    }

    /*
     * Return the default floor object
     */
    public Node getFloor() {
        return floor;
    }

    /*
     * Set the default floor object
     */
    public void setFloor( Node n ) {
        floor = n;
    }

    /*
     * Return true if the scene has been initialized
     */
    public boolean isInitialized() {
        return sceneInitialized();
    }

    /*
     * Return the current camera that is rendering the scene
     */
    public Camera getCamera() {
        return camera;
    }

    /*
     * Return the SciJava Display that contains SciView
     */
    public Display<?> getDisplay() {
        return scijavaDisplay;
    }

    /*
     * Set the SciJava Display
     */
    public void setDisplay( Display<?> display ) {
        scijavaDisplay = display;
    }

    /*
     * Center the camera on the scene such that all objects are within the field of view
     */
    public void centerOnScene() {
        centerOnNode(getScene());
    }

    /*
     * Get the InputHandler that is managing mouse, input, VR controls, etc.
     */
    public InputHandler getSceneryInputHandler() {
        return getInputHandler();
    }

    /*
     * Return a bounding box around a subgraph of the scenegraph
     */
    public OrientedBoundingBox getSubgraphBoundingBox( Node n ) {
        Function<Node,List<Node>> predicate = node -> node.getChildren();
        return getSubgraphBoundingBox(n,predicate);
    }

    /*
     * Return a bounding box around a subgraph of the scenegraph
     */
    public OrientedBoundingBox getSubgraphBoundingBox( Node n, Function<Node,List<Node>> branchFunction ) {
        if(n.getBoundingBox() == null && n.getChildren().size() == 0) {
            return n.getMaximumBoundingBox().asWorld();
        }

        List<Node> branches = branchFunction.apply(n);
        if( branches.size() == 0 ) {
            if( n.getBoundingBox() == null )
                return null;
            else
                return n.getBoundingBox().asWorld();
        }

        OrientedBoundingBox bb = n.getMaximumBoundingBox();
        for( Node c : branches ){
            OrientedBoundingBox cBB = getSubgraphBoundingBox(c, branchFunction);
            if( cBB != null )
                bb = bb.expand(bb, cBB);
        }
        return bb;
    }

    /*
     * Center the camera on the specified Node
     */
    public void centerOnNode( Node currentNode ) {
        centerOnNode(currentNode,notAbstractBranchingFunction);
    }

    /*
     * Center the camera on the specified Node
     */
    public void centerOnNode( Node currentNode, Function<Node,List<Node>> branchFunction ) {
        if( currentNode == null ) return;

        OrientedBoundingBox bb = getSubgraphBoundingBox(currentNode, branchFunction);
        //log.debug("Centering on: " + currentNode + " bb: " + bb.getMin() + " to " + bb.getMax());
        if( bb == null ) return;

        getCamera().setTarget( bb.getBoundingSphere().getOrigin() );
        getCamera().setTargeted( true );

        // Set forward direction to point from camera at active node
        GLVector forward = bb.getBoundingSphere().getOrigin().minus( getCamera().getPosition() ).normalize().times( -1 );

        float distance = (float) (bb.getBoundingSphere().getRadius() / Math.tan( getCamera().getFov() / 360 * java.lang.Math.PI ));

        // Solve for the proper rotation
        Quaternion rotation = new Quaternion().setLookAt( forward.toFloatArray(),
                                                          new GLVector(0,1,0).toFloatArray(),
                                                          new GLVector(1,0,0).toFloatArray(),
                                                          new GLVector( 0,1,0).toFloatArray(),
                                                          new GLVector( 0, 0, 1).toFloatArray() );

        getCamera().setRotation( rotation.invert().normalize() );
        getCamera().setPosition( bb.getBoundingSphere().getOrigin().plus( getCamera().getForward().times( distance * -1 ) ) );

        getCamera().setDirty(true);
        getCamera().setNeedsUpdate(true);
    }

    public float getFPSSpeed() {
        return fpsScrollSpeed;
    }

    public void setFPSSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 30.0f ) newspeed = 30.0f;
        fpsScrollSpeed = newspeed;
        //log.debug( "FPS scroll speed: " + fpsScrollSpeed );
    }

    public float getMouseSpeed() {
        return mouseSpeedMult;
    }

    public void setMouseSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 3.0f ) newspeed = 3.0f;
        mouseSpeedMult = newspeed;
        //log.debug( "Mouse speed: " + mouseSpeedMult );
    }

    /*
     * Reset the input handler to first-person-shooter (FPS) style controls
     */
    public void resetFPSInputs() {
        InputHandler h = getInputHandler();
        if(h == null) {
            getLogger().error("InputHandler is null, cannot change bindings.");
            return;
        }

        h.addBehaviour( "move_forward_scroll",
                                        new MovementCommand( "move_forward", "forward", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        h.addBehaviour( "move_forward",
                                        new MovementCommand( "move_forward", "forward", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        h.addBehaviour( "move_back",
                                        new MovementCommand( "move_back", "back", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        h.addBehaviour( "move_left",
                                        new MovementCommand( "move_left", "left", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        h.addBehaviour( "move_right",
                                        new MovementCommand( "move_right", "right", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        h.addBehaviour( "move_up",
                                        new MovementCommand( "move_up", "up", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        h.addBehaviour( "move_down",
                                        new MovementCommand( "move_down", "down", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );

        h.addKeyBinding( "move_forward_scroll", "scroll" );
    }

    public void setObjectSelectionMode() {
        Function1<? super List<Scene.RaycastResult>, Unit> selectAction = nearest -> {
            if( !nearest.isEmpty() ) {
                setActiveNode( nearest.get( 0 ).getNode() );
                nodePropertyEditor.trySelectNode( getActiveNode() );
                log.debug( "Selected node: " + getActiveNode().getName() );
            }
            return Unit.INSTANCE;
        };
        setObjectSelectionMode(selectAction);
    }

    /*
     * Set the action used during object selection
     */
    public void setObjectSelectionMode(Function1<? super List<Scene.RaycastResult>, Unit> selectAction) {
        final InputHandler h = getInputHandler();
        List<Class<?>> ignoredObjects = new ArrayList<>();
        ignoredObjects.add( BoundingGrid.class );

        if(h == null) {
            getLogger().error("InputHandler is null, cannot change object selection mode.");
            return;
        }
        h.addBehaviour( "object_selection_mode",
                                        new SelectCommand( "objectSelector", getRenderer(), getScene(),
                                                           () -> getScene().findObserver(), false, ignoredObjects,
                                                           selectAction ) );
        h.addKeyBinding( "object_selection_mode", "double-click button1" );
    }

    /*
     * Initial configuration of the scenery InputHandler
     * This is automatically called and should not be used directly
     */
    @Override public void inputSetup() {
        final InputHandler h = getInputHandler();
        if(h == null) {
            getLogger().error("InputHandler is null, cannot run input setup.");
            return;
        }

        // TODO: Maybe get rid of this?
        h.useDefaultBindings( "" );

        // Mouse controls
        setObjectSelectionMode();
        NodeTranslateControl nodeTranslate = new NodeTranslateControl(this, 0.0005f);
        h.addBehaviour( "mouse_control_nodetranslate", nodeTranslate );
        h.addKeyBinding( "mouse_control_nodetranslate", "ctrl button1" );
        h.addBehaviour( "scroll_nodetranslate", nodeTranslate );
        h.addKeyBinding( "scroll_nodetranslate", "ctrl scroll" );

        h.addBehaviour("move_up_slow", new MovementCommand("move_up", "up", () -> getScene().findObserver(), fpsScrollSpeed ) );
        h.addBehaviour("move_down_slow", new MovementCommand("move_down", "down", () -> getScene().findObserver(), fpsScrollSpeed ) );
        h.addBehaviour("move_up_fast", new MovementCommand("move_up", "up", () -> getScene().findObserver(), 1.0f ) );
        h.addBehaviour("move_down_fast", new MovementCommand("move_down", "down", () -> getScene().findObserver(), 1.0f ) );

        h.addKeyBinding("move_up_slow", "X");
        h.addKeyBinding("move_down_slow", "C");
        h.addKeyBinding("move_up_fast", "shift X");
        h.addKeyBinding("move_down_fast", "shift C");

        enableArcBallControl();
        enableFPSControl();

        // Extra keyboard controls
        h.addBehaviour( "show_help", new showHelpDisplay() );
        h.addKeyBinding( "show_help", "U" );

        h.addBehaviour( "enable_decrease", new enableDecrease() );
        h.addKeyBinding( "enable_decrease", "M" );

        h.addBehaviour( "enable_increase", new enableIncrease() );
        h.addKeyBinding( "enable_increase", "N" );

        //float veryFastSpeed = getScene().getMaximumBoundingBox().getBoundingSphere().getRadius()/50f;
        float veryFastSpeed = 100f;
        h.addBehaviour("move_forward_veryfast", new MovementCommand("move_forward", "forward", () -> getScene().findObserver(), veryFastSpeed));
        h.addBehaviour("move_back_veryfast", new MovementCommand("move_back", "back", () -> getScene().findObserver(), veryFastSpeed));
        h.addBehaviour("move_left_veryfast", new MovementCommand("move_left", "left", () -> getScene().findObserver(), veryFastSpeed));
        h.addBehaviour("move_right_veryfast", new MovementCommand("move_right", "right", () -> getScene().findObserver(), veryFastSpeed));
        h.addBehaviour("move_up_veryfast", new MovementCommand("move_up", "up", () -> getScene().findObserver(), veryFastSpeed));
        h.addBehaviour("move_down_veryfast", new MovementCommand("move_down", "down", () -> getScene().findObserver(), veryFastSpeed));

        h.addKeyBinding("move_forward_veryfast", "ctrl shift W");
        h.addKeyBinding("move_back_veryfast", "ctrl shift S");
        h.addKeyBinding("move_left_veryfast", "ctrl shift A");
        h.addKeyBinding("move_right_veryfast", "ctrl shift D");
        h.addKeyBinding("move_up_veryfast", "ctrl shift X");
        h.addKeyBinding("move_down_veryfast", "ctrl shift C");

    }

    /*
     * Change the control mode to circle around the active object in an arcball
     */
    private void enableArcBallControl() {
        final InputHandler h = getInputHandler();
        if(h == null) {
            getLogger().error("InputHandler is null, cannot setup arcball.");
            return;
        }

        GLVector target;
        if( getActiveNode() == null ) {
            target = new GLVector( 0, 0, 0 );
        } else {
            target = getActiveNode().getPosition();
        }

        float mouseSpeed = 0.25f;
        mouseSpeed = getMouseSpeed();

        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        targetArcball = new ArcballCameraControl( "mouse_control_arcball", cameraSupplier,
                                                  getRenderer().getWindow().getWidth(),
                                                  getRenderer().getWindow().getHeight(), target );
        targetArcball.setMaximumDistance( Float.MAX_VALUE );
        targetArcball.setMouseSpeedMultiplier( mouseSpeed );
        targetArcball.setScrollSpeedMultiplier( 0.05f );
        targetArcball.setDistance( getCamera().getPosition().minus( target ).magnitude() );

        // FIXME: Swing seems to have issues with shift-scroll actions, so we change
		//  this to alt-scroll here for the moment.
        h.addBehaviour( "mouse_control_arcball", targetArcball );
        h.addKeyBinding( "mouse_control_arcball", "shift button1" );
        h.addBehaviour( "scroll_arcball", targetArcball );
        h.addKeyBinding( "scroll_arcball", "shift scroll" );
    }

    /*
     * Enable FPS style controls
     */
    private void enableFPSControl() {
        final InputHandler h = getInputHandler();
        if(h == null) {
            getLogger().error("InputHandler is null, cannot setup fps control.");
            return;
        }

        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        fpsControl = new FPSCameraControl( "mouse_control", cameraSupplier, getRenderer().getWindow().getWidth(),
                                           getRenderer().getWindow().getHeight() );

        h.addBehaviour( "mouse_control", fpsControl );
        h.addKeyBinding( "mouse_control", "button1" );

        h.addBehaviour( "mouse_control_cameratranslate", new CameraTranslateControl( this, 0.002f ) );
        h.addKeyBinding( "mouse_control_cameratranslate", "button2" );

        resetFPSInputs();
    }

    /**
     * Add a box to the scene with default parameters
     * @return the Node corresponding to the box
     */
    public Node addBox() {
        return addBox( new ClearGLVector3( 0.0f, 0.0f, 0.0f ) );
    }

    /**
     * Add a box at the specific position and unit size
     * @param position
     * @return the Node corresponding to the box
     */
    public Node addBox( Vector3 position ) {
        return addBox( position, new ClearGLVector3( 1.0f, 1.0f, 1.0f ) );
    }

    /**
     * Add a box at the specified position and with the specified size
     * @param position
     * @param size
     * @return the Node corresponding to the box
     */
    public Node addBox( Vector3 position, Vector3 size ) {
        return addBox( position, size, DEFAULT_COLOR, false );
    }

    /**
     * Add a box at the specified position with specified size, color, and normals on the inside/outside
     * @param position
     * @param size
     * @param color
     * @param inside
     * @return the Node corresponding to the box
     */
    public Node addBox( final Vector3 position, final Vector3 size, final ColorRGB color,
                                         final boolean inside ) {
        // TODO: use a material from the current palate by default
        final Material boxmaterial = new Material();
        boxmaterial.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        boxmaterial.setDiffuse( Utils.convertToGLVector( color ) );
        boxmaterial.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        final Box box = new Box( ClearGLVector3.convert( size ), inside );
        box.setMaterial( boxmaterial );
        box.setPosition( ClearGLVector3.convert( position ) );

        return addNode( box );
    }

    /**
     * Add a unit sphere at the origin
     * @return the Node corresponding to the sphere
     */
    public Node addSphere() {
        return addSphere( new ClearGLVector3( 0.0f, 0.0f, 0.0f ), 1 );
    }

    /**
     * Add a sphere at the specified position with a given radius
     * @param position
     * @param radius
     * @return the Node corresponding to the sphere
     */
    public Node addSphere( Vector3 position, float radius ) {
        return addSphere( position, radius, DEFAULT_COLOR );
    }

    /**
     * Add a sphere at the specified positoin with a given radius and color
     * @param position
     * @param radius
     * @param color
     * @return  the Node corresponding to the sphere
     */
    public Node addSphere( final Vector3 position, final float radius, final ColorRGB color ) {
        final Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( Utils.convertToGLVector( color ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        final Sphere sphere = new Sphere( radius, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( ClearGLVector3.convert( position ) );

        return addNode( sphere );
    }

    /**
     * Add a Cylinder at the given position with radius, height, and number of faces/segments
     * @param position
     * @param radius
     * @param height
     * @param num_segments
     * @return  the Node corresponding to the cylinder
     */
    public Node addCylinder( final Vector3 position, final float radius, final float height, final int num_segments ) {
        final Cylinder cyl = new Cylinder( radius, height, num_segments );
        cyl.setPosition( ClearGLVector3.convert( position ) );
        return addNode( cyl );
    }

    /**
     * Add a Cone at the given position with radius, height, and number of faces/segments
     * @param position
     * @param radius
     * @param height
     * @param num_segments
     * @return  the Node corresponding to the cone
     */
    public Node addCone( final Vector3 position, final float radius, final float height, final int num_segments ) {
        final Cone cone = new Cone( radius, height, num_segments, new GLVector(0,0,1) );
        cone.setPosition( ClearGLVector3.convert( position ) );
        return addNode( cone );
    }

    /**
     * Add a Line from 0,0,0 to 1,1,1
     * @return  the Node corresponding to the line
     */
    public Node addLine() {
        return addLine( new ClearGLVector3( 0.0f, 0.0f, 0.0f ), new ClearGLVector3( 1.0f, 1.0f, 1.0f ) );
    }

    /**
     * Add a line from start to stop
     * @param start
     * @param stop
     * @return  the Node corresponding to the line
     */
    public Node addLine( Vector3 start, Vector3 stop ) {
        return addLine( start, stop, DEFAULT_COLOR );
    }

    /**
     * Add a line from start to stop with the given color
     * @param start
     * @param stop
     * @param color
     * @return the Node corresponding to the line
     */
    public Node addLine( Vector3 start, Vector3 stop, ColorRGB color ) {
        return addLine( new Vector3[] { start, stop }, color, 0.1f );
    }

    /**
     * Add a multi-segment line that goes through the supplied points with a single color and edge width
     * @param points
     * @param color
     * @param edgeWidth
     * @return the Node corresponding to the line
     */
    public Node addLine( final Vector3[] points, final ColorRGB color, final double edgeWidth ) {
        final Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        material.setDiffuse( Utils.convertToGLVector( color ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        final Line line = new Line( points.length );
        for( final Vector3 pt : points ) {
            line.addPoint( ClearGLVector3.convert( pt ) );
        }

        line.setEdgeWidth( ( float ) edgeWidth );

        line.setMaterial( material );
        line.setPosition( ClearGLVector3.convert( points[0] ) );

        return addNode( line );
    }

    /**
     * Add a PointLight source at the origin
     * @return a Node corresponding to the PointLight
     */
    public Node addPointLight() {
        final Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new GLVector( 0.0f, 1.0f, 0.0f ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        final PointLight light = new PointLight( 5.0f );
        light.setMaterial( material );
        light.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );
        lights.add(light);

        return addNode( light );
    }

    /**
     * Position all lights that were initialized by default around the scene in a circle at Y=0
     */
    public void surroundLighting() {
        OrientedBoundingBox bb = getSubgraphBoundingBox(getScene(), notAbstractBranchingFunction);
        OrientedBoundingBox.BoundingSphere boundingSphere = bb.getBoundingSphere();
        // Choose a good y-position, then place lights around the cross-section through this plane
        float y = 0;
        GLVector c = boundingSphere.getOrigin();
        float r = boundingSphere.getRadius();
        for( int k = 0; k < lights.size(); k++ ) {
            PointLight light = lights.get(k);
            float x = (float) (c.x() + r * Math.cos( k == 0 ? 0 : Math.PI * 2 * ((float)k / (float)lights.size()) ));
            float z = (float) (c.y() + r * Math.sin( k == 0 ? 0 : Math.PI * 2 * ((float)k / (float)lights.size()) ));
            light.setLightRadius( 2 * r );
            light.setPosition( new GLVector( x, y, z ) );
        }
    }

    /**
     * Write a scenery mesh as an stl to the given file
     * @param filename
     * @param scMesh
     */
    public void writeSCMesh( String filename, Mesh scMesh ) {
        File f = new File( filename );
        BufferedOutputStream out;
        try {
            out = new BufferedOutputStream( new FileOutputStream( f ) );
            out.write( "solid STL generated by FIJI\n".getBytes() );

            FloatBuffer normalsFB = scMesh.getNormals();
            FloatBuffer verticesFB = scMesh.getVertices();

            while( verticesFB.hasRemaining() && normalsFB.hasRemaining() ) {
                out.write( ( "facet normal " + normalsFB.get() + " " + normalsFB.get() + " " + normalsFB.get() +
                             "\n" ).getBytes() );
                out.write( "outer loop\n".getBytes() );
                for( int v = 0; v < 3; v++ ) {
                    out.write( ( "vertex\t" + verticesFB.get() + " " + verticesFB.get() + " " + verticesFB.get() +
                                 "\n" ).getBytes() );
                }
                out.write( "endloop\n".getBytes() );
                out.write( "endfacet\n".getBytes() );
            }
            out.write( "endsolid vcg\n".getBytes() );
            out.close();
        } catch( FileNotFoundException e ) {
            e.printStackTrace();
        } catch( IOException e ) {
            e.printStackTrace();
        }

    }

    /**
     * Return the default point size to use for point clouds
     * @return
     */
    public float getDefaultPointSize() {
        return 0.025f;
    }

    /**
     * Create an array of normal vectors from a set of vertices corresponding to triangles
     */
    public float[] makeNormalsFromVertices( ArrayList<RealPoint> verts ) {
        float[] normals = new float[verts.size()];// div3 * 3coords

        for( int k = 0; k < verts.size(); k += 3 ) {
            GLVector v1 = new GLVector( verts.get( k ).getFloatPosition( 0 ), //
                                        verts.get( k ).getFloatPosition( 1 ), //
                                        verts.get( k ).getFloatPosition( 2 ) );
            GLVector v2 = new GLVector( verts.get( k + 1 ).getFloatPosition( 0 ),
                                        verts.get( k + 1 ).getFloatPosition( 1 ),
                                        verts.get( k + 1 ).getFloatPosition( 2 ) );
            GLVector v3 = new GLVector( verts.get( k + 2 ).getFloatPosition( 0 ),
                                        verts.get( k + 2 ).getFloatPosition( 1 ),
                                        verts.get( k + 2 ).getFloatPosition( 2 ) );
            GLVector a = v2.minus( v1 );
            GLVector b = v3.minus( v1 );
            GLVector n = a.cross( b ).getNormalized();
            normals[k / 3] = n.get( 0 );
            normals[k / 3 + 1] = n.get( 1 );
            normals[k / 3 + 2] = n.get( 2 );
        }
        return normals;
    }

    /**
     * Open a file specified by the source path. The file can be anything that SciView knows about: mesh, volume, point cloud
     * @param source
     * @throws IOException
     */
    public void open( final String source ) throws IOException {
        if(source.endsWith(".xml")) {
            addBDVVolume(source);
            return;
        }

        final Object data = io.open( source );
        if( data instanceof net.imagej.mesh.Mesh ) addMesh( ( net.imagej.mesh.Mesh ) data );
        else if( data instanceof graphics.scenery.Mesh ) addMesh( ( graphics.scenery.Mesh ) data );
        else if( data instanceof graphics.scenery.PointCloud ) addPointCloud( ( graphics.scenery.PointCloud ) data );
        else if( data instanceof Dataset ) addVolume( ( Dataset ) data );
        else if( data instanceof IterableInterval ) addVolume( ( ( IterableInterval ) data ), source );
        else if( data instanceof List ) {
            final List<?> list = ( List<?> ) data;
            if( list.isEmpty() ) {
                throw new IllegalArgumentException( "Data source '" + source + "' appears empty." );
            }
            final Object element = list.get( 0 );
            if( element instanceof RealLocalizable ) {
                // NB: For now, we assume all elements will be RealLocalizable.
                // Highly likely to be the case, barring antagonistic importers.
                @SuppressWarnings("unchecked") final List<? extends RealLocalizable> points = ( List<? extends RealLocalizable> ) list;
                addPointCloud( points, source );
            } else {
                final String type = element == null ? "<null>" : element.getClass().getName();
                throw new IllegalArgumentException( "Data source '" + source + //
                                                    "' contains elements of unknown type '" + type + "'" );
            }
        } else {
            final String type = data == null ? "<null>" : data.getClass().getName();
            throw new IllegalArgumentException( "Data source '" + source + //
                                                "' contains data of unknown type '" + type + "'" );
        }
    }

    /**
     * Add the given points to the scene as a PointCloud
     * @param points
     * @return a Node corresponding to the PointCloud
     */
    public Node addPointCloud( Collection<? extends RealLocalizable> points ) {
        return addPointCloud( points, "PointCloud" );
    }

    /**
     * Add the given points to the scene as a PointCloud with a given name
     * @param points
     * @param name
     * @return
     */
    public Node addPointCloud( final Collection<? extends RealLocalizable> points,
                                                final String name ) {
        final float[] flatVerts = new float[points.size() * 3];
        int k = 0;
        for( final RealLocalizable point : points ) {
            flatVerts[k * 3] = point.getFloatPosition( 0 );
            flatVerts[k * 3 + 1] = point.getFloatPosition( 1 );
            flatVerts[k * 3 + 2] = point.getFloatPosition( 2 );
            k++;
        }

        final PointCloud pointCloud = new PointCloud( getDefaultPointSize(), name );
        final Material material = new Material();
        final FloatBuffer vBuffer = BufferUtils.allocateFloat( flatVerts.length * 4 );
        final FloatBuffer nBuffer = BufferUtils.allocateFloat( 0 );

        vBuffer.put( flatVerts );
        vBuffer.flip();

        pointCloud.setVertices( vBuffer );
        pointCloud.setNormals( nBuffer );
        pointCloud.setIndices( BufferUtils.allocateInt( 0 ) );
        pointCloud.setupPointCloud();
        material.setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        material.setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.setMaterial( material );
        pointCloud.setPosition( new GLVector( 0f, 0f, 0f ) );

        return addNode( pointCloud );
    }

    /**
     * Add a PointCloud to the scene
     * @param pointCloud
     * @return a Node corresponding to the PointCloud
     */
    public Node addPointCloud( final PointCloud pointCloud ) {
        pointCloud.setupPointCloud();
        pointCloud.getMaterial().setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.setPosition( new GLVector( 0f, 0f, 0f ) );

        return addNode( pointCloud );
    }

    /**
     * Add a Node to the scene and publish it to the eventservice
     * @param n
     * @return a Node corresponding to the Node
     */
    public Node addNode( final Node n ) {
        return addNode(n, true);
    }

    /**
     * Add Node n to the scene and set it as the active node/publish it to the event service if activePublish is true
     * @param n
     * @param activePublish
     * @return a Node corresponding to the Node
     */
    public Node addNode( final Node n, final boolean activePublish ) {
        getScene().addChild( n );

        objectService.addObject(n);

        if( activePublish ) {
//            setActiveNode(n);
//            if (floor.getVisible())
//                updateFloorPosition();
            eventService.publish(new NodeAddedEvent(n));
        }
        return n;
    }

    /**
     * Add a scenery Mesh to the scene
     * @param scMesh
     * @return a Node corresponding to the mesh
     */
    public Node addMesh( final Mesh scMesh ) {
        final Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new GLVector( 0.0f, 1.0f, 0.0f ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );

        objectService.addObject(scMesh);

        return addNode( scMesh );
    }

    /**
     * Add an ImageJ mesh to the scene
     * @param mesh
     * @return a Node corresponding to the mesh
     */
    public Node addMesh( net.imagej.mesh.Mesh mesh ) {
        Mesh scMesh = MeshConverter.toScenery( mesh );

        return addMesh( scMesh );
    }

    /**
     * [Deprecated: use deleteNode]
     * Remove a Mesh from the scene
     * @param scMesh
     */
    public void removeMesh( Mesh scMesh ) {
        getScene().removeChild( scMesh );
    }

    /**
     * @return a Node corresponding to the currently active node
     */
    public Node getActiveNode() {
        return activeNode;
    }

    /**
     * Set the currently active node
     * @param n
     * @return the currently active node
     */
    public Node setActiveNode( Node n ) {
        if( activeNode == n ) return activeNode;
        activeNode = n;
        targetArcball.setTarget( n == null ? () -> new GLVector( 0, 0, 0 ) : () -> n.getMaximumBoundingBox().getBoundingSphere().getOrigin());
        eventService.publish( new NodeActivatedEvent( activeNode ) );

        return activeNode;
    }

    @EventHandler
    protected void onNodeAdded(NodeAddedEvent event) {
        nodePropertyEditor.rebuildTree();
    }

    @EventHandler
    protected void onNodeRemoved(NodeRemovedEvent event) {
        nodePropertyEditor.rebuildTree();
    }

    @EventHandler
    protected void onNodeChanged(NodeChangedEvent event) {
    	nodePropertyEditor.rebuildTree();
    }

    @EventHandler
    protected void onNodeActivated(NodeActivatedEvent event) {
        // TODO: add listener code for node activation, if necessary
        // NOTE: do not update property window here, this will lead to a loop.
    }

    public void toggleInspectorWindow()
    {
        boolean currentlyVisible = inspector.isVisible();
        if(currentlyVisible) {
            inspector.setVisible(false);
            mainSplitPane.setDividerLocation(getWindowWidth());
        }
        else {
            inspector.setVisible(true);
            mainSplitPane.setDividerLocation(getWindowWidth()/4 * 3);
        }

    }

    /**
     * Create an animation thread with the given fps speed and the specified action
     * @param fps
     * @param action
     * @return a Future corresponding to the thread
     */
    public synchronized Future<?> animate(int fps, Runnable action ) {
        // TODO: Make animation speed less laggy and more accurate.
        final int delay = 1000 / fps;
        Future<?> thread = threadService.run(() -> {
            while (animating) {
                action.run();
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        animations.add( thread );
        animating = true;
        return thread;
    }

    /**
     * Stop all animations
     */
    public synchronized void stopAnimation() {
        animating = false;
        while( !animations.isEmpty() ) {
            animations.peek().cancel( true );
            animations.remove();
        }
    }

    /**
     * Take a screenshot and save it to the default scenery location
     */
    public void takeScreenshot() {
        getRenderer().screenshot();
    }

    /**
     * Take a screenshot and save it to the specified path
     * @param path
     */
    public void takeScreenshot( String path ) {
        getRenderer().screenshot( path, false );
    }

    /**
     * Take a screenshot and return it as an Img
     * @return an Img of type UnsignedByteType
     */
    public Img<UnsignedByteType> getScreenshot() {
        RenderedImage screenshot = getSceneryRenderer().requestScreenshot();

        BufferedImage image = new BufferedImage(screenshot.getWidth(), screenshot.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        byte[] imgData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        System.arraycopy(screenshot.getData(), 0, imgData, 0, screenshot.getData().length);

        Img<UnsignedByteType> img = null;
        File tmpFile = null;
        try {
            tmpFile = File.createTempFile("sciview-", "-tmp.png");
            ImageIO.write(image, "png", tmpFile);
            img = (Img<UnsignedByteType>)io.open(tmpFile.getAbsolutePath());
            tmpFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return img;
    }

    /**
     * @return an array of all nodes in the scene except Cameras and PointLights
     */
    public Node[] getSceneNodes() {
        return getSceneNodes( n -> !( n instanceof Camera ) && !( n instanceof PointLight  ) );
    }

    /**
     * Get a list of nodes filtered by filter predicate
     * @param filter, a predicate that filters the candidate nodes
     * @return all nodes that match the predicate
     */
    public Node[] getSceneNodes( Predicate<? super Node> filter ) {
        return getScene().getChildren().stream().filter( filter ).toArray( Node[]::new );
    }

    /**
     * @return an array of all Node's in the scene
     */
    public Node[] getAllSceneNodes() {
        return getSceneNodes( n -> true );
    }

    /**
     * Delete the current active node
     */
    public void deleteActiveNode() {
        deleteNode( getActiveNode() );
    }

    /**
     * Delete the specified node, this event is published
     * @param node
     */
    public void deleteNode( Node node ) {
        deleteNode( node, true );
    }

    /**
     * Delete a specified node and control whether the event is published
     * @param node
     * @param activePublish
     */
    public void deleteNode( Node node, boolean activePublish ) {
        for( Node child : node.getChildren() ) {
            deleteNode(child, activePublish);
        }

        node.getParent().removeChild( node );
        if( activePublish ) {
            eventService.publish(new NodeRemovedEvent(node));
            if (activeNode == node) setActiveNode(null);
        }
    }

    /**
     * Dispose the current scenery renderer, hub, and other scenery things
     */
    public void dispose() {
        this.close();
    }

    /**
     * Move the current active camera to the specified position
     * @param position
     */
    public void moveCamera( float[] position ) {
        getCamera().setPosition( new GLVector( position[0], position[1], position[2] ) );
    }

    /**
     * Move the current active camera to the specified position
     * @param position
     */
    public void moveCamera( double[] position ) {
        getCamera().setPosition( new GLVector( ( float ) position[0], ( float ) position[1], ( float ) position[2] ) );
    }

    /**
     * Get the current application name
     * @return a String of the application name
     */
    public String getName() {
        return getApplicationName();
    }

    /**
     * Add a child to the scene. you probably want addNode
     * @param node
     */
    public void addChild( Node node ) {
        getScene().addChild( node );
    }

    /**
     * Add a Dataset to the scene as a volume. Voxel resolution and name are extracted from the Dataset itself
     * @param image
     * @return a Node corresponding to the Volume
     */
    public Node addVolume( Dataset image ) {

        float[] voxelDims = new float[image.numDimensions()];
        for( int d = 0; d < voxelDims.length; d++ ) {
            double inValue = image.axis(d).averageScale(0, 1);
            voxelDims[d] = (float) unitService.value( inValue, image.axis(d).unit(), axis(d).unit() );
        }

        return addVolume( image, voxelDims );
    }

    /**
     * Add a BigDataViewer volume to the scene.
     * @param source, the path to an XML file for BDV style XML/Hdf5
     * @return a Node corresponding to the BDVNode
     */
    public Node addBDVVolume( String source ) {
        //getSettings().set("Renderer.HDR.Exposure", 20.0f);

        final VolumeViewerOptions opts = new VolumeViewerOptions();
        opts.maxCacheSizeInMB(Integer.parseInt(System.getProperty("scenery.BDVVolume.maxCacheSize", "512")));
        final BDVVolume v = new BDVVolume(source, opts);

        // TODO: use unitService to set scale
        v.setScale(new GLVector(0.01f, 0.01f, 0.01f));
        v.setBoundingBox(v.generateBoundingBox());

        getScene().addChild(v);
        setActiveNode(v);
        v.goToTimePoint(0);

		eventService.publish( new NodeAddedEvent( v ) );

        return v;
    }

    /**
     * Add a Dataset as a Volume with the specified voxel dimensions
     * @param image
     * @param voxelDimensions
     * @return a Node corresponding to the Volume
     */
    @SuppressWarnings({ "rawtypes", "unchecked" }) public Node addVolume( Dataset image, float[] voxelDimensions ) {
        return addVolume( ( IterableInterval ) Views.flatIterable( image.getImgPlus() ), image.getName(),
                          voxelDimensions );
    }

    /**
     * Add a RandomAccessibleInterval to the image
     * @param image
     * @param name
     * @param extra, kludge argument to prevent matching issues
     * @param <T>
     * @return a Node corresponding to the volume
     */
    public <T extends RealType<T>> Node addVolume( RandomAccessibleInterval<T> image, String name, String extra ) {
        long[] pos = new long[]{10, 10, 10};

        return addVolume( Views.flatIterable(image), name, 1, 1, 1 );
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image
     * @param <T>
     * @return a Node corresponding to the Volume
     */
    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image ) {
        return addVolume( image, "Volume" );
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image
     * @param name
     * @param <T>
     * @return a Node corresponding to the Volume
     */
    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image, String name ) {
        return addVolume( image, name, 1, 1, 1 );
    }

    /**
     * Set the ColorMap of node n to the supplied colorTable
     * @param n
     * @param colorTable
     */
    public void setColormap( Node n, ColorTable colorTable ) {
        final int copies = 16;

        final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                4 * colorTable.getLength() * copies );// Num bytes * num components * color map length * height of color map texture

        final byte[] tmp = new byte[4 * colorTable.getLength()];
        for( int k = 0; k < colorTable.getLength(); k++ ) {
            for( int c = 0; c < colorTable.getComponentCount(); c++ ) {
                // TODO this assumes numBits is 8, could be 16
                tmp[4 * k + c] = ( byte ) colorTable.get( c, k );
            }

            if( colorTable.getComponentCount() == 3 ) {
                tmp[4 * k + 3] = (byte)255;
            }
        }

        for( int i = 0; i < copies; i++ ) {
            byteBuffer.put(tmp);
        }

        byteBuffer.flip();

        n.getMetadata().put("sciviewColormap", colorTable);

        if(n instanceof Volume) {
            ((Volume) n).getColormaps().put("sciviewColormap",
                    new Volume.Colormap.ColormapBuffer(new GenericTexture("colorTable",
                    new GLVector(colorTable.getLength(), copies, 1.0f), 4,
                    GLTypeEnum.UnsignedByte,
                    byteBuffer,
                    // don't repeat the color map
                    TextureRepeatMode.ClampToEdge, TextureRepeatMode.ClampToEdge, TextureRepeatMode.ClampToEdge)));
            ((Volume) n).setColormap("sciviewColormap");
            n.setDirty(true);
            n.setNeedsUpdate(true);
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
     */
    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image, String name,
                                                                    float... voxelDimensions ) {
        //log.debug( "Add Volume " + name + " image: " + image );

        long[] dimensions = new long[3];
        image.dimensions( dimensions );

        Volume v = new Volume();

        getScene().addChild( v );

        @SuppressWarnings("unchecked") Class<T> voxelType = ( Class<T> ) image.firstElement().getClass();
        float minVal, maxVal;

        if( voxelType == UnsignedByteType.class ) {
            minVal = 0;
            maxVal = 255;
        } else if( voxelType == UnsignedShortType.class ) {
            minVal = 0;
            maxVal = 65535;
        } else if( voxelType == FloatType.class ) {
            minVal = 0;
            maxVal = 1;
        } else if( voxelType == VolatileUnsignedByteType.class ) {
            minVal = 0;
            maxVal = 255;
        } else if( voxelType == VolatileUnsignedShortType.class ) {
            minVal = 0;
            maxVal = 65535;
        } else if( voxelType == VolatileFloatType.class ) {
            minVal = 0;
            maxVal = 1;
        } else {
            log.debug( "Type: " + voxelType +
                       " cannot be displayed as a volume. Convert to UnsignedByteType, UnsignedShortType, or FloatType." );
            return null;
        }

        updateVolume( image, name, voxelDimensions, v );

        v.setTrangemin( minVal );
        v.setTrangemax( maxVal );
        v.setTransferFunction(TransferFunction.ramp(0.0f, 0.4f));

        try {
            setColormap( v, lutService.loadLUT( lutService.findLUTs().get( "WCIF/ICA.lut" ) ) );
        } catch( IOException e ) {
            e.printStackTrace();
        }

        setActiveNode( v );
        eventService.publish( new NodeAddedEvent( v ) );

        objectService.addObject( v );

        return v;
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
     */
    public <T extends RealType<T>> Node updateVolume( IterableInterval<T> image, String name,
                                                                       float[] voxelDimensions, Volume v ) {
        //log.debug( "Update Volume" );

        long[] dimensions = new long[3];
        image.dimensions( dimensions );

        @SuppressWarnings("unchecked") Class<T> voxelType = ( Class<T> ) image.firstElement().getClass();
        int bytesPerVoxel = image.firstElement().getBitsPerPixel() / 8;
        NativeTypeEnum nType;

        if( voxelType == UnsignedByteType.class || voxelType == VolatileUnsignedByteType.class ) {
            nType = NativeTypeEnum.UnsignedByte;
        } else if( voxelType == UnsignedShortType.class || voxelType == VolatileUnsignedShortType.class ) {
            nType = NativeTypeEnum.UnsignedShort;
        } else if( voxelType == FloatType.class || voxelType == VolatileFloatType.class ) {
            nType = NativeTypeEnum.Float;
        } else {
            log.debug( "Type: " + voxelType +
                       " cannot be displayed as a volume. Convert to UnsignedByteType, UnsignedShortType, or FloatType." );
            return null;
        }

        // Make and populate a ByteBuffer with the content of the Dataset
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                ( int ) ( bytesPerVoxel * dimensions[0] * dimensions[1] * dimensions[2] ) );
        Cursor<T> cursor = image.cursor();

        while( cursor.hasNext() ) {
            cursor.fwd();
            // TODO should we check if volatiles are valid
            if( voxelType == UnsignedByteType.class ) {
                byteBuffer.put( ( byte ) ( ( ( UnsignedByteType ) cursor.get() ).get() ) );
            } else if( voxelType == VolatileUnsignedByteType.class ) {
                byteBuffer.put( ( byte ) ( ( ( VolatileUnsignedByteType ) cursor.get() ).get().get() ) );
            } else if( voxelType == UnsignedShortType.class ) {
                byteBuffer.putShort( ( short ) Math.abs( ( ( UnsignedShortType ) cursor.get() ).getShort() ) );
            } else if( voxelType == VolatileUnsignedShortType.class ) {
                byteBuffer.putShort( ( short ) Math.abs( ( ( VolatileUnsignedShortType ) cursor.get() ).get().getShort() ) );
            } else if( voxelType == FloatType.class ) {
                byteBuffer.putFloat( ( ( FloatType ) cursor.get() ).get() );
            } else if( voxelType == VolatileFloatType.class ) {
                byteBuffer.putFloat( ( ( VolatileFloatType ) cursor.get() ).get().get() );
            }
        }
        byteBuffer.flip();

        v.readFromBuffer( name, byteBuffer, dimensions[0], dimensions[1], dimensions[2], voxelDimensions[0],
                          voxelDimensions[1], voxelDimensions[2], nType, bytesPerVoxel );

        v.setDirty( true );
        v.setNeedsUpdate( true );
        v.setNeedsUpdateWorld( true );

        return v;
    }

    /**
     *
     * @return whether PushMode is currently active
     */
    public boolean getPushMode() {
        return getRenderer().getPushMode();
    }

    /**
     * Set the status of PushMode, which only updates the render panel when there is a change in the scene
     * @param push
     * @return current PushMode status
     */
    public boolean setPushMode( boolean push ) {
        getRenderer().setPushMode( push );
        return getRenderer().getPushMode();
    }
    
    public ArcballCameraControl getTargetArcball() {
        return targetArcball;
    }

    @Override
    protected void finalize() {
        stopAnimation();
    }

    public Settings getScenerySettings() {
        return this.getSettings();
    }

    public Statistics getSceneryStats() {
        return this.getStats();
    }

    public Renderer getSceneryRenderer() {
        return this.getRenderer();
    }

    /**
     * Enable VR rendering
     */
    public void toggleVRRendering() {
        vrActive = !vrActive;
        Camera cam = getScene().getActiveObserver();
        if(!(cam instanceof DetachedHeadCamera)) {
            return;
        }

        TrackerInput ti = null;
        boolean hmdAdded = false;

        if (!getHub().has(SceneryElement.HMDInput)) {
            try {
                final OpenVRHMD hmd = new OpenVRHMD(false, true);

                if(hmd.initializedAndWorking()) {
                    getHub().add(SceneryElement.HMDInput, hmd);
                    ti = hmd;
                } else {
                    getLogger().warn("Could not initialise VR headset, just activating stereo rendering.");
                }

                hmdAdded = true;
            } catch (Exception e) {
                getLogger().error("Could not add OpenVRHMD: " + e.toString());
            }
        } else {
            ti = getHub().getWorkingHMD();
        }

        if(vrActive && ti != null) {
            ((DetachedHeadCamera) cam).setTracker(ti);
        } else {
            ((DetachedHeadCamera) cam).setTracker(null);
        }

        getRenderer().setPushMode(false);
        // we need to force reloading the renderer as the HMD might require device or instance extensions
        if(getRenderer() instanceof VulkanRenderer && hmdAdded) {
            replaceRenderer(getRenderer().getClass().getSimpleName(), true, true);
            getRenderer().toggleVR();

            while(!getRenderer().getInitialized() || !getRenderer().getFirstImageReady()) {
                getLogger().debug("Waiting for renderer reinitialisation");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            getRenderer().toggleVR();
        }

    }

    /**
     * Utility function to generate GLVector in cases like usage from Python
     * @param x
     * @param y
     * @param z
     * @return a GLVector of x,y,z
     */
    static public GLVector getGLVector(float x, float y, float z) {
        return new GLVector(x, y, z);
    }

    /**
     * Set the rotation of Node N by generating a quaternion from the supplied arguments
     * @param n
     * @param x
     * @param y
     * @param z
     * @param w
     */
    public void setRotation(Node n, float x, float y, float z, float w) {
        n.setRotation(new Quaternion(x,y,z,w));
    }

    public void setScale(Node n, float x, float y, float z) {
        n.setScale(new GLVector(x,y,z));
    }

    public void setColor(Node n, float x, float y, float z, float w) {
        GLVector col = new GLVector(x, y, z, w);
        n.getMaterial().setAmbient(col);
        n.getMaterial().setDiffuse(col);
        n.getMaterial().setSpecular(col);
    }

    public void setPosition(Node n, float x, float y, float z) {
        n.setPosition(new GLVector(x,y,z));
    }

    public void addWindowListener(WindowListener wl) {
        frame.addWindowListener(wl);
    }

    @Override
    public CalibratedAxis axis(int i) {
        return axes[i];
    }

    @Override
    public void axes(CalibratedAxis[] calibratedAxes) {
        axes = calibratedAxes;
    }

    @Override
    public void setAxis(CalibratedAxis calibratedAxis, int i) {
        axes[i] = calibratedAxis;
    }

    @Override
    public double realMin(int i) {
        return Double.NEGATIVE_INFINITY;
    }

    @Override
    public void realMin(double[] doubles) {
        for( int i = 0; i < doubles.length; i++ ) {
            doubles[i] = Double.NEGATIVE_INFINITY;
        }
    }

    @Override
    public void realMin(RealPositionable realPositionable) {
        for( int i = 0; i < realPositionable.numDimensions(); i++ ) {
            realPositionable.move(Double.NEGATIVE_INFINITY, i);
        }
    }

    @Override
    public double realMax(int i) {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public void realMax(double[] doubles) {
        for( int i = 0; i < doubles.length; i++ ) {
            doubles[i] = Double.POSITIVE_INFINITY;
        }
    }

    @Override
    public void realMax(RealPositionable realPositionable) {
        for( int i = 0; i < realPositionable.numDimensions(); i++ ) {
            realPositionable.move(Double.POSITIVE_INFINITY, i);
        }
    }

    @Override
    public int numDimensions() {
        return axes.length;
    }

    public class TransparentSlider extends JSlider {

        public TransparentSlider() {
            // Important, we taking over the filling of the
            // component...
            setOpaque(false);
            setBackground(java.awt.Color.DARK_GRAY);
            setForeground(java.awt.Color.LIGHT_GRAY);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setColor(getBackground());
            g2d.setComposite(AlphaComposite.SrcOver.derive(0.9f));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.dispose();

            super.paintComponent(g);
        }

    }

    class enableIncrease implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            setFPSSpeed( getFPSSpeed() + 0.5f );
            setMouseSpeed( getMouseSpeed() + 0.05f );

            //log.debug( "Increasing FPS scroll Speed" );

            resetFPSInputs();
        }
    }

    class enableDecrease implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            setFPSSpeed( getFPSSpeed() - 0.1f );
            setMouseSpeed( getMouseSpeed() - 0.05f );

            //log.debug( "Decreasing FPS scroll Speed" );

            resetFPSInputs();
        }
    }

    class showHelpDisplay implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            String helpString = "SciView help:\n\n";
            for( InputTrigger trigger : getInputHandler().getAllBindings().keySet() ) {
                helpString += trigger + "\t-\t" + getInputHandler().getAllBindings().get( trigger ) + "\n";
            }
            // HACK: Make the console pop via stderr.
            // Later, we will use a nicer dialog box or some such.
            log.warn( helpString );
        }
    }

    /*
     * Convenience function for getting a string of info about a Node
     */
    public String nodeInfoString(Node n) {
        return "Node name: " + n.getName() + " Node type: " + n.getNodeType() + " To String: " + n;
    }

    /**
     * Static launching method
     */
    public static SciView createSciView() {
        SceneryBase.xinitThreads();

        System.setProperty( "scijava.log.level:sc.iview", "debug" );
        Context context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class, ThreadService.class);

        SciViewService sciViewService = context.service( SciViewService.class );
        SciView sciView = sciViewService.getOrCreateActiveSciView();

        return sciView;
    }
}

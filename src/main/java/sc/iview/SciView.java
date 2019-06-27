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
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.imagej.Dataset;
import net.imagej.lut.LUTService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.display.ColorTable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.event.EventHandler;
import org.scijava.event.EventService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuService;
import org.scijava.object.ObjectService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
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
import java.awt.image.BufferedImage;
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
public class SciView extends SceneryBase {

    public static final ColorRGB DEFAULT_COLOR = Colors.LIGHTGRAY;

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
     * Mouse controls for FPS movement and Arcball rotation
     */
    protected ArcballCameraControl targetArcball;
    protected FPSCameraControl fpsControl;

    /**
     * The primary camera/observer in the scene
     */
    Camera camera = null;

    /**
     * Speeds for input controls
     */
    private float fpsScrollSpeed = 3.0f;

    private float mouseSpeedMult = 0.25f;

    private Display<?> scijavaDisplay;

    /**
     * The floor that orients the user in the scene
     */
    protected Node floor;

    private JLabel splashLabel;
    private SceneryJPanel panel;
    private JSplitPane mainSplitPane;
    private final SceneryPanel[] sceneryPanel = { null };
    private JSplitPane inspector;
    private NodePropertyEditor nodePropertyEditor;
    private ArrayList<PointLight> lights;
    private Stack<HashMap<String, Object>> controlStack;

    private Predicate<? super Node> notAbstractNode = new Predicate<Node>() {
        @Override
        public boolean test(Node node) {
            return !( (node instanceof Camera) || (node instanceof Light) || (node==getFloor()));
        }
    };

    public SciView( Context context ) {
        super( "SciView", 1280, 720, false, context );
        context.inject( this );
    }

    public SciView( String applicationName, int windowWidth, int windowHeight ) {
        super( applicationName, windowWidth, windowHeight, false );
    }

    public InputHandler publicGetInputHandler() {
        return getInputHandler();
    }

    public void toggleRecordVideo() {
        if( getRenderer() instanceof  OpenGLRenderer )
            ((OpenGLRenderer)getRenderer()).recordMovie();
        else
            ((VulkanRenderer)getRenderer()).recordMovie();
    }

    public void stashControls() {
        // This pushes the current input setup onto a stack that allows them to be restored with restoreControls
        HashMap<String, Object> controlState = new HashMap<String, Object>();
        controlStack.push(controlState);
    }

    public void restoreControls() {
        // This pops/restores the previously stashed controls. Emits a warning if there are no stashed controlls
        HashMap<String, Object> controlState = controlStack.pop();

        // This isnt how it should work
        setObjectSelectionMode();
        resetFPSInputs();
    }

    public void fitCameraToScene() {
        centerOnNode(getScene());
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

    @SuppressWarnings("restriction") @Override public void init() {
        if(Boolean.parseBoolean(System.getProperty("sciview.useDarcula", "false"))) {
            try {
                BasicLookAndFeel darcula = new DarculaLaf();
                UIManager.setLookAndFeel(darcula);
            } catch (Exception e) {
                getLogger().info("Could not load Darcula Look and Feel");
            }
        }

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

        JFrame frame = new JFrame("SciView");
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

        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                getLogger().debug("Closing SciView window.");
                close();
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

        Camera cam = new DetachedHeadCamera();
        cam.setPosition( new GLVector( 0.0f, 5.0f, 5.0f ) );
        cam.perspectiveCamera( 50.0f, getWindowWidth(), getWindowHeight(), 0.1f, 1000.0f );
        //cam.setTarget( new GLVector( 0, 0, 0 ) );
        //cam.setTargeted( true );
        cam.setActive( true );
        getScene().addChild( cam );
        this.camera = cam;

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setName( "Floor" );
        floor.setPosition( new GLVector( 0f, -1f, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

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
//        getScene().getOnNodePropertiesChanged().put("updateInspector",
//                node -> {
//                    if( node == nodePropertyEditor.getCurrentNode() ) {
//                        nodePropertyEditor.updateProperties(node, true);
//                    }
//                    return null;
//                });
    }

    public void setFloor( Node n ) {
        floor = n;
    }

    public Node getFloor() {
        return floor;
    }

    private float getFloorY() {
        return floor.getPosition().y();
    }

    private void setFloorY(float new_pos ) {
        float temp_pos = new_pos;
        if( temp_pos < -100f ) temp_pos = -100f;
        else if( new_pos > 5f ) temp_pos = 5f;
        floor.getPosition().set( 1, temp_pos );
    }

    public boolean isInitialized() {
        return sceneInitialized();
    }

    public Camera getCamera() {
        return camera;
    }

    public void setDisplay( Display<?> display ) {
        scijavaDisplay = display;
    }

    public Display<?> getDisplay() {
        return scijavaDisplay;
    }

    public void centerOnScene() {
        centerOnNode(getScene());
    }

    public InputHandler getSceneryInputHandler() {
        return getInputHandler();
    }

    public OrientedBoundingBox getSubgraphBoundingBox( Node n ) {
        Function<Node,List<Node>> predicate = node -> node.getChildren();
        return getSubgraphBoundingBox(n,predicate);
    }

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


    private Function<Node,List<Node>> notAbstractBranchingFunction = node -> node.getChildren().stream().filter(notAbstractNode).collect(Collectors.toList());

    public void centerOnNode( Node currentNode ) {
        centerOnNode(currentNode,notAbstractBranchingFunction);
    }

    public void centerOnNode( Node currentNode, Function<Node,List<Node>> branchFunction ) {
        if( currentNode == null ) return;

        OrientedBoundingBox bb = getSubgraphBoundingBox(currentNode, branchFunction);
        //System.out.println("Centering on: " + currentNode + " bb: " + bb.getMin() + " to " + bb.getMax());
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

    public void setFPSSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 30.0f ) newspeed = 30.0f;
        fpsScrollSpeed = newspeed;
        //log.debug( "FPS scroll speed: " + fpsScrollSpeed );
    }

    public float getFPSSpeed() {
        return fpsScrollSpeed;
    }

    public void setMouseSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 3.0f ) newspeed = 3.0f;
        mouseSpeedMult = newspeed;
        //log.debug( "Mouse speed: " + mouseSpeedMult );
    }

    public float getMouseSpeed() {
        return mouseSpeedMult;
    }

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

        h.addBehaviour( "mouse_control_arcball", targetArcball );
        h.addKeyBinding( "mouse_control_arcball", "shift button1" );
        h.addBehaviour( "scroll_arcball", targetArcball );
        h.addKeyBinding( "scroll_arcball", "shift scroll" );
    }

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

    public Node addBox() {
        return addBox( new ClearGLVector3( 0.0f, 0.0f, 0.0f ) );
    }

    public Node addBox( Vector3 position ) {
        return addBox( position, new ClearGLVector3( 1.0f, 1.0f, 1.0f ) );
    }

    public Node addBox( Vector3 position, Vector3 size ) {
        return addBox( position, size, DEFAULT_COLOR, false );
    }

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


    public Node addSphere() {
        return addSphere( new ClearGLVector3( 0.0f, 0.0f, 0.0f ), 1 );
    }

    public Node addSphere( Vector3 position, float radius ) {
        return addSphere( position, radius, DEFAULT_COLOR );
    }

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

    public Node addCylinder( final Vector3 position, final float radius, final float height, final int num_segments ) {
        final Cylinder cyl = new Cylinder( radius, height, num_segments );
        cyl.setPosition( ClearGLVector3.convert( position ) );
        return addNode( cyl );
    }

    public Node addCone( final Vector3 position, final float radius, final float height, final int num_segments ) {
        final Cone cone = new Cone( radius, height, num_segments, new GLVector(0,0,1) );
        cone.setPosition( ClearGLVector3.convert( position ) );
        return addNode( cone );
    }

    public Node addLine() {
        return addLine( new ClearGLVector3( 0.0f, 0.0f, 0.0f ), new ClearGLVector3( 0.0f, 0.0f, 0.0f ) );
    }

    public Node addLine( Vector3 start, Vector3 stop ) {
        return addLine( start, stop, DEFAULT_COLOR );
    }

    public Node addLine( Vector3 start, Vector3 stop, ColorRGB color ) {
        return addLine( new Vector3[] { start, stop }, color, 0.1f );
    }

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

    public float getDefaultPointSize() {
        return 0.025f;
    }

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

    public Node addPointCloud( Collection<? extends RealLocalizable> points ) {
        return addPointCloud( points, "PointCloud" );
    }

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

    public Node addPointCloud( final PointCloud pointCloud ) {
        pointCloud.setupPointCloud();
        pointCloud.getMaterial().setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.setPosition( new GLVector( 0f, 0f, 0f ) );

        return addNode( pointCloud );
    }

    public Node addNode( final Node n ) {
        return addNode(n, true);
    }

    public Node addNode( final Node n, final boolean activePublish ) {
        getScene().addChild( n );
        if( activePublish ) {
//            setActiveNode(n);
//            if (floor.getVisible())
//                updateFloorPosition();
            eventService.publish(new NodeAddedEvent(n));
        }
        return n;
    }

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

    public Node addMesh( net.imagej.mesh.Mesh mesh ) {
        Mesh scMesh = MeshConverter.toScenery( mesh );

        return addMesh( scMesh );
    }

    public void removeMesh( Mesh scMesh ) {
        getScene().removeChild( scMesh );
    }

    public Node getActiveNode() {
        return activeNode;
    }

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

    public synchronized void stopAnimation() {
        animating = false;
        while( !animations.isEmpty() ) {
            animations.peek().cancel( true );
            animations.remove();
        }
    }

    public void takeScreenshot() {
        getRenderer().screenshot();
    }

    public void takeScreenshot( String path ) {
        getRenderer().screenshot( path, false );
    }

    public Node[] getSceneNodes() {
        return getSceneNodes( n -> !( n instanceof Camera ) && !( n instanceof PointLight  ) );
    }

    public Node[] getSceneNodes( Predicate<? super Node> filter ) {
        return getScene().getChildren().stream().filter( filter ).toArray( Node[]::new );
    }

    public Node[] getAllSceneNodes() {
        return getSceneNodes( n -> true );
    }

    public void deleteActiveNode() {
        deleteNode( getActiveNode() );
    }

    public void deleteNode( Node node ) {
        deleteNode( node, true );
    }

    public void deleteNode( Node node, boolean activePublish ) {
        node.getParent().removeChild( node );
        if( activePublish ) {
            eventService.publish(new NodeRemovedEvent(node));
            if (activeNode == node) setActiveNode(null);
        }
    }

    public void dispose() {
        this.close();
    }

    public void moveCamera( float[] position ) {
        getCamera().setPosition( new GLVector( position[0], position[1], position[2] ) );
    }

    public void moveCamera( double[] position ) {
        getCamera().setPosition( new GLVector( ( float ) position[0], ( float ) position[1], ( float ) position[2] ) );
    }

    public String getName() {
        return getApplicationName();
    }

    public void addChild( Node node ) {
        getScene().addChild( node );
    }

    public Node addVolume( Dataset image ) {
        float[] voxelDims = new float[image.numDimensions()];
        for( int d = 0; d < voxelDims.length; d++ ) {
            voxelDims[d] = ( float ) image.axis( d ).averageScale( 0, 1 );
        }
        return addVolume( image, voxelDims );
    }

    public Node addBDVVolume( String source ) {
        //getSettings().set("Renderer.HDR.Exposure", 20.0f);

        final VolumeViewerOptions opts = new VolumeViewerOptions();
        opts.maxCacheSizeInMB(Integer.parseInt(System.getProperty("scenery.BDVVolume.maxCacheSize", "512")));
        final BDVVolume v = new BDVVolume(source, opts);
        v.setScale(new GLVector(0.01f, 0.01f, 0.01f));
        v.setBoundingBox(v.generateBoundingBox());

        getScene().addChild(v);
        setActiveNode(v);
        v.goToTimePoint(0);

		eventService.publish( new NodeAddedEvent( v ) );

        return v;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) public Node addVolume( Dataset image,
                                                                                           float[] voxelDimensions ) {
        return addVolume( ( IterableInterval ) Views.flatIterable( image.getImgPlus() ), image.getName(),
                          voxelDimensions );
    }

    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image ) {
        return addVolume( image, "Volume" );
    }

    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image, String name ) {
        return addVolume( image, name, 1, 1, 1 );
    }

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
                    false, false, false)));
            ((Volume) n).setColormap("sciviewColormap");
        }
    }

    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image, String name,
                                                                    float... voxelDimensions ) {
        //log.debug( "Add Volume" );

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

        return v;
    }

    public <T extends RealType<T>> Node updateVolume( IterableInterval<T> image, String name,
                                                                       float[] voxelDimensions, Volume v ) {
        //log.debug( "Update Volume" );

        long[] dimensions = new long[3];
        image.dimensions( dimensions );

        @SuppressWarnings("unchecked") Class<T> voxelType = ( Class<T> ) image.firstElement().getClass();
        int bytesPerVoxel = image.firstElement().getBitsPerPixel() / 8;
        NativeTypeEnum nType;

        if( voxelType == UnsignedByteType.class ) {
            nType = NativeTypeEnum.UnsignedByte;
        } else if( voxelType == UnsignedShortType.class ) {
            nType = NativeTypeEnum.UnsignedShort;
        } else if( voxelType == FloatType.class ) {
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
            if( voxelType == UnsignedByteType.class ) {
                byteBuffer.put( ( byte ) ( ( ( UnsignedByteType ) cursor.get() ).get() ) );
            } else if( voxelType == UnsignedShortType.class ) {
                byteBuffer.putShort( ( short ) Math.abs( ( ( UnsignedShortType ) cursor.get() ).getShort() ) );
            } else if( voxelType == FloatType.class ) {
                byteBuffer.putFloat( ( ( FloatType ) cursor.get() ).get() );
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

    public boolean getPushMode() {
        return getRenderer().getPushMode();
    }

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

    private void updateFloorPosition() {
        // Lower the floor below the active node, as needed.
        final Node currentNode = getActiveNode();
        if( currentNode != null ) {
            while( currentNode.getDirty() || currentNode.getNeedsUpdate() ) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            floor.setPosition( new GLVector( 0f, java.lang.Math.min(getFloor().getPosition().y(),getActiveNode().getMaximumBoundingBox().getMin().y()), 0f ) );
        }
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

    protected boolean vrActive = false;

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
}

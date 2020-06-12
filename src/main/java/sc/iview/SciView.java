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
package sc.iview;

import bdv.BigDataViewer;
import bdv.cache.CacheControl;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.AxisOrder;
import bdv.util.RandomAccessibleIntervalSource;
import bdv.util.RandomAccessibleIntervalSource4D;
import bdv.util.volatiles.VolatileView;
import bdv.util.volatiles.VolatileViewData;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import cleargl.GLVector;
import com.formdev.flatlaf.FlatLightLaf;
import com.intellij.ui.tabs.JBTabsPosition;
import com.intellij.ui.tabs.TabInfo;
import com.intellij.ui.tabs.impl.JBEditorTabs;
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
import graphics.scenery.volumes.Colormap;
import graphics.scenery.volumes.RAIVolume;
import graphics.scenery.volumes.TransferFunction;
import graphics.scenery.volumes.Volume;
import io.scif.SCIFIOService;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function3;
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
import net.imglib2.RandomAccess;
import net.imglib2.*;
import net.imglib2.display.ColorTable;
import net.imglib2.img.Img;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.Platform;
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
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.swing.menu.SwingJMenuBarCreator;
import org.scijava.util.ColorRGB;
import org.scijava.util.Colors;
import org.scijava.util.VersionUtils;
import sc.iview.commands.view.NodePropertyEditor;
import sc.iview.controls.behaviours.CameraTranslateControl;
import sc.iview.controls.behaviours.NodeTranslateControl;
import sc.iview.controls.behaviours.NodeRotateControl;
import sc.iview.event.NodeActivatedEvent;
import sc.iview.event.NodeAddedEvent;
import sc.iview.event.NodeChangedEvent;
import sc.iview.event.NodeRemovedEvent;
import sc.iview.process.MeshConverter;
import sc.iview.ui.ContextPopUpNodeChooser;
import sc.iview.ui.REPLPane;
import sc.iview.vector.JOMLVector3;
import sc.iview.vector.Vector3;
import tpietzsch.example2.VolumeViewerOptions;

import javax.imageio.ImageIO;
import javax.script.ScriptException;
import javax.swing.*;
import java.awt.Image;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.*;
import java.net.URL;
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

/**
 * Main SciView class.
 *
 * @author Kyle Harrington
 */
// we suppress unused warnings here because @Parameter-annotated fields
// get updated automatically by SciJava.
@SuppressWarnings({"unused", "WeakerAccess"})
public class SciView extends SceneryBase implements CalibratedRealInterval<CalibratedAxis> {

    public static final ColorRGB DEFAULT_COLOR = Colors.LIGHTGRAY;
    private final SceneryPanel[] sceneryPanel = { null };
    /**
     * Mouse controls for FPS movement and Arcball rotation
     */
    protected AnimatedCenteringBeforeArcBallControl targetArcball;
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
    private float fpsSpeedSlow     = 0.05f;
    private float fpsSpeedFast     = 1.0f;
    private float fpsSpeedVeryFast = 50.0f;
    private float mouseSpeedMult   = 0.25f;

    private Display<?> scijavaDisplay;
    private SplashLabel splashLabel;
    private SceneryJPanel panel;
    private JSplitPane mainSplitPane;
    private JSplitPane inspector;
    private JSplitPane interpreterSplitPane;
    private REPLPane interpreterPane;
    private NodePropertyEditor nodePropertyEditor;
    private ArrayList<PointLight> lights;
    private Stack<HashMap<String, Object>> controlStack;
    private JFrame frame;
    private Predicate<? super Node> notAbstractNode = (Predicate<Node>) node -> !( (node instanceof Camera) || (node instanceof Light) || (node==getFloor()));
    private boolean isClosed = false;
    private Function<Node,List<Node>> notAbstractBranchingFunction = node -> node.getChildren().stream().filter(notAbstractNode).collect(Collectors.toList());

    // If true, then when a new node is added to the scene, the camera will refocus on this node by default
    private boolean centerOnNewNodes;

    // If true, then when a new node is added the thread will block until the node is added to the scene. This is required for
    //   centerOnNewNodes
    private boolean blockOnNewNodes;
    private PointLight headlight;

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
     * Toggle video recording with scenery's video recording mechanism
     * Note: this video recording may skip frames because it is asynchronous
     *
     * @param filename destination for saving video
     * @param overwrite should the file be replaced, otherwise a unique incrementing counter will be appended
     */
    public void toggleRecordVideo(String filename, boolean overwrite) {
        if( getRenderer() instanceof  OpenGLRenderer )
            ((OpenGLRenderer)getRenderer()).recordMovie(filename, overwrite);
        else
            ((VulkanRenderer)getRenderer()).recordMovie(filename, overwrite);
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
        resetFPSBehaviours();
    }

    public ArrayList<PointLight> getLights() {
        return lights;
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

        // Setup camera
        Camera cam;
        if( getCamera() == null ) {
            cam = new DetachedHeadCamera();
            this.camera = cam;
            cam.setPosition(new Vector3f(0.0f, 1.65f, 0.0f));
            getScene().addChild( cam );
        } else {
            cam = getCamera();
        }
        cam.setPosition( new Vector3f( 0.0f, 1.65f, 5.0f ) );
        cam.perspectiveCamera( 50.0f, getWindowWidth(), getWindowHeight(), 0.1f, 1000.0f );

        // Setup lights
        Vector3f[] tetrahedron = new Vector3f[4];
        tetrahedron[0] = new Vector3f( 1.0f, 0f, -1.0f/(float)Math.sqrt(2.0f) );
        tetrahedron[1] = new Vector3f( -1.0f,0f,-1.0f/(float)Math.sqrt(2.0) );
        tetrahedron[2] = new Vector3f( 0.0f,1.0f,1.0f/(float)Math.sqrt(2.0) );
        tetrahedron[3] = new Vector3f( 0.0f,-1.0f,1.0f/(float)Math.sqrt(2.0) );

        lights = new ArrayList<>();

        for( int i = 0; i < 4; i++ ) {// TODO allow # initial lights to be customizable?
            PointLight light = new PointLight(150.0f);
            light.setPosition( tetrahedron[i].mul(25.0f) );
            light.setEmissionColor( new Vector3f( 1.0f, 1.0f, 1.0f ) );
            light.setIntensity( 1.0f );
            lights.add( light );
            //camera.addChild( light );
            getScene().addChild( light );
        }

        // Make a headlight for the camera
        headlight = new PointLight(150.0f);
        headlight.setPosition( new Vector3f(0f, 0f, -1f).mul(25.0f) );
        headlight.setEmissionColor( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        headlight.setIntensity( 0.5f );
        headlight.setName("headlight");


        Icosphere lightSphere = new Icosphere(1.0f, 2);
        headlight.addChild(lightSphere);
        lightSphere.getMaterial().setDiffuse(headlight.getEmissionColor());
        lightSphere.getMaterial().setSpecular(headlight.getEmissionColor());
        lightSphere.getMaterial().setAmbient(headlight.getEmissionColor());
        lightSphere.getMaterial().setWireframe(true);
        lightSphere.setVisible(false);
        //lights.add( light );
        camera.setNearPlaneDistance(0.01f);
        camera.setFarPlaneDistance(1000.0f);
        camera.addChild( headlight );

        floor = new InfinitePlane();//new Box( new Vector3f( 500f, 0.2f, 500f ) );
        ((InfinitePlane)floor).setType(InfinitePlane.Type.Grid);
        floor.setName( "Floor" );
        getScene().addChild( floor );

    }

    /**
     * Initialization of SWING and scenery. Also triggers an initial population of lights/camera in the scene
     */
    @SuppressWarnings("restriction") @Override public void init() {

        // Darcula dependency went missing from maven repo, factor it out
//        if(Boolean.parseBoolean(System.getProperty("sciview.useDarcula", "false"))) {
//            try {
//                BasicLookAndFeel darcula = new DarculaLaf();
//                UIManager.setLookAndFeel(darcula);
//            } catch (Exception e) {
//                getLogger().info("Could not load Darcula Look and Feel");
//            }
//        }
        final String logLevel = System.getProperty("scenery.LogLevel", "info");
        log.setLevel(LogLevel.value(logLevel));

        LogbackUtils.setLogLevel(null, logLevel);

        System.getProperties().stringPropertyNames().forEach(name -> {
            if(name.startsWith("scenery.LogLevel")) {
                LogbackUtils.setLogLevel("", System.getProperty(name, "info"));
            }
        });

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

        // TODO: check for jdk 8 v. jdk 11 on linux and choose renderer accordingly
        if( Platform.get() == Platform.LINUX ) {
            String version = System.getProperty("java.version");
            if( version.startsWith("1.") ) {
                version = version.substring(2, 3);
            } else {
                int dot = version.indexOf(".");
                if (dot != -1) {
                    version = version.substring(0, dot);
                }
            }

            // If Linux and JDK 8, then use OpenGLRenderer
            if( version.equals("8") )
                System.setProperty("scenery.Renderer", "OpenGLRenderer");
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

        p.setLayout(new OverlayLayout(p));
        p.setBackground(new Color(50, 48, 47));
        p.add(panel, BorderLayout.CENTER);
        panel.setVisible(true);

        nodePropertyEditor.getComponent(); // Initialize node property panel

        JTree inspectorTree = nodePropertyEditor.getTree();
        inspectorTree.setToggleClickCount(0);// This disables expanding menus on double click
        JPanel inspectorProperties = nodePropertyEditor.getProps();

        JBEditorTabs tp = new JBEditorTabs(null);
        tp.setTabsPosition(JBTabsPosition.right);
        tp.setSideComponentVertical(true);

        inspector = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
                new JScrollPane( inspectorTree ),
                new JScrollPane( inspectorProperties ));
        inspector.setDividerLocation( getWindowHeight() / 3 );
        inspector.setContinuousLayout(true);
        inspector.setBorder(BorderFactory.createEmptyBorder());
        inspector.setDividerSize(1);
        ImageIcon inspectorIcon = getScaledImageIcon(this.getClass().getResource("toolbox.png"), 16, 16);

        TabInfo tiInspector = new TabInfo(inspector, inspectorIcon);
        tiInspector.setText("");
        tp.addTab(tiInspector);

        // We need to get the surface scale here before initialising scenery's renderer, as
        // the information is needed already at initialisation time.
        final AffineTransform dt = frame.getGraphicsConfiguration().getDefaultTransform();
        final Vector2f surfaceScale = new Vector2f((float)dt.getScaleX(), (float)dt.getScaleY());
        getSettings().set("Renderer.SurfaceScale", surfaceScale);

        interpreterPane = new REPLPane(getScijavaContext());
        interpreterPane.getComponent().setBorder(BorderFactory.createEmptyBorder());
        ImageIcon interpreterIcon = getScaledImageIcon(this.getClass().getResource("terminal.png"), 16, 16);

        TabInfo tiREPL = new TabInfo(interpreterPane.getComponent(), interpreterIcon);
        tiREPL.setText("");
        tp.addTab(tiREPL);

        tp.addTabMouseListener(new MouseListener() {
            private boolean hidden = false;
            private int previousPosition = 0;

            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    toggleSidebar();
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        initializeInterpreter();

        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, //
                p,
                tp.getComponent()
        );
        mainSplitPane.setDividerLocation(frame.getSize().width - 36);
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder());
        mainSplitPane.setDividerSize(1);
        mainSplitPane.setResizeWeight(0.9);
        sidebarHidden = true;

        //frame.add(mainSplitPane, BorderLayout.CENTER);
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

        splashLabel = new SplashLabel();
        frame.setGlassPane(splashLabel);
        frame.getGlassPane().setVisible(true);
        frame.getGlassPane().requestFocusInWindow();
//            frame.getGlassPane().setBackground(new java.awt.Color(50, 48, 47, 255));
        frame.setVisible(true);

        sceneryPanel[0] = panel;

        setRenderer( Renderer.createRenderer( getHub(), getApplicationName(), getScene(),
                getWindowWidth(), getWindowHeight(),
                sceneryPanel[0]) );

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
            getLogger().info("Done initializing SciView");

            // subscribe to Node{Added, Removed, Changed} events happens automagically owing to the annotations
            frame.getGlassPane().setVisible(false);
            panel.setVisible(true);

            // install hook to keep inspector updated on external changes (scripting, etc)
            getScene().getOnNodePropertiesChanged().put("updateInspector",
                    node -> {
                        if( node == nodePropertyEditor.getCurrentNode() ) {
                            nodePropertyEditor.updateProperties(node);
                        }
                        return null;
                    });

            // Enable push rendering by default
            getRenderer().setPushMode( true );

            sciView.getCamera().setPosition(1.65, 1);

        });
    }

    private boolean sidebarHidden = false;
    private int previousSidebarPosition = 0;

    public boolean toggleSidebar() {
        if(!sidebarHidden) {
            previousSidebarPosition = mainSplitPane.getDividerLocation();
            // TODO: remove hard-coded tab width
            mainSplitPane.setDividerLocation(frame.getSize().width - 36);
            sidebarHidden = true;
        } else {
            if(previousSidebarPosition == 0) {
                previousSidebarPosition = getWindowWidth()/3 * 2;
            }

            mainSplitPane.setDividerLocation(previousSidebarPosition);
            sidebarHidden = false;
        }

        return sidebarHidden;
    }

    private ImageIcon getScaledImageIcon(final URL resource, int width, int height) {
        final ImageIcon first = new ImageIcon(resource);
        final Image image = first.getImage();

        BufferedImage resizedImg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = resizedImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(first.getImage(), 0, 0, width, height, null);
        g2.dispose();

        return new ImageIcon(resizedImg);
    }

    private void initializeInterpreter() {
        String startupCode = "";
        startupCode = new Scanner(SciView.class.getResourceAsStream("startup.py"), "UTF-8").useDelimiter("\\A").next();
        interpreterPane.getREPL().getInterpreter().getBindings().put("sciView", this);
        try {
            interpreterPane.getREPL().getInterpreter().eval(startupCode);
        } catch (ScriptException e) {
            e.printStackTrace();
        }
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

    /**
     * Return the current SceneryJPanel. This is necessary for custom context menus
     * @return panel the current SceneryJPanel
     */
    public SceneryJPanel getSceneryJPanel() {
        return panel;
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
        if(n.getBoundingBox() == null && n.getChildren().size() != 0) {
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

    /**
     * Place the camera such that all objects in the scene are within the field of view
     */
    public void fitCameraToScene() {
        centerOnNode(getScene());
        //TODO: smooth zoom in/out VLADO vlado Vlado
    }

    /**
     * Center the camera on the scene
     */
    public void centerOnScene() {
        centerOnNode(getScene());
    }

    /**
     * Center the camera on the scene
     */
    public void centerOnActiveNode() {
        if (activeNode == null) return;
        centerOnNode(activeNode);
    }

    /**
     * Center the camera on the specified Node
     */
    public void centerOnNode( Node currentNode ) {
        if (currentNode == null) {
            log.info("Cannot center on node. CurrentNode is null");
            return;
        }

        //center the on the same spot as ArcBall does
        centerOnPosition( currentNode.getMaximumBoundingBox().getBoundingSphere().getOrigin() );
    }

    /**
     * Center the camera on the specified Node
     */
    public void centerOnPosition( Vector3f currentPos ) {
        //current and desired directions in world coords
        final Vector3f currViewDir = new Vector3f(camera.getTarget()).sub(camera.getPosition());
        final Vector3f wantViewDir = new Vector3f(currentPos).sub(camera.getPosition());

        //current and desired directions as the camera sees them
        camera.getTransformation().transformDirection(currViewDir).normalize();
        camera.getTransformation().transformDirection(wantViewDir).normalize();

        //we gonna rotate directly to match the current view direction with the desired one,
        //which means to rotate by this angle:
        float totalDeltaAng = (float)Math.acos( currViewDir.dot(wantViewDir) );
        //
        //if the needed angle is less than 2 deg, we do nothing...
        if (totalDeltaAng > -0.035 && totalDeltaAng < 0.035) return;
        //
        //here's the axis along which we will rotate
        currViewDir.cross(wantViewDir);

        //animation options: control delay between animation frames -- fluency
        final long rotPauseyPerStep = 30; //miliseconds

        //animation options: control max number of steps -- upper limit on total time for animation
        final int rotMaxSteps = 999999;  //effectively disabled....

        //how many steps when max update/move is 5 deg -- smoothness
        int rotSteps = (int)Math.ceil( Math.abs(totalDeltaAng) / 0.087 );
        if (rotSteps > rotMaxSteps) rotSteps = rotMaxSteps;

        log.debug("centering by deltaAng="+ 180.0f*totalDeltaAng/3.14159f+" deg over "+rotSteps+" steps");

        //angular progress aux variables
        float angCurPos = 0, angNextPos, angDelta;

        camera.setTargeted(false);
        for (int i = 1; i <= rotSteps; ++i) {
            //this emulates ease-in ease-out animation, both vars are in [0:1]
            float timeProgress = (float)i / rotSteps;
            float angProgress = ((timeProgress *= 2) <= 1 ? //two cubics connected smoothly into S-shape curve from [0,0] to [1,1]
                    timeProgress * timeProgress * timeProgress :
                    (timeProgress -= 2) * timeProgress * timeProgress + 2) / 2;

            angNextPos = angProgress * totalDeltaAng;    //where I should be by now
            angDelta = angNextPos - angCurPos;           //how much I must travel to get there
            angCurPos = angNextPos;                      //suppose we already got there...

            new Quaternionf().rotateAxis(-angDelta,currViewDir).mul(camera.getRotation(),camera.getRotation());
            try {
                Thread.sleep(rotPauseyPerStep);
            } catch (InterruptedException e) {
                i = rotSteps;
            }
        }
    }

    public float getFPSSpeedSlow() {
        return fpsSpeedSlow;
    }
    public float getFPSSpeedFast() {
        return fpsSpeedFast;
    }
    public float getFPSSpeedVeryFast() {
        return fpsSpeedVeryFast;
    }

    private float speedWithinBounds(float newspeed, final float minBound, final float maxBound)
    {
        if( newspeed < minBound ) newspeed = minBound;
        else if( newspeed > maxBound ) newspeed = maxBound;
        return newspeed;
    }
    public void setFPSSpeed( float newBaseSpeed ) {
        fpsSpeedSlow     = speedWithinBounds(newBaseSpeed, 0.01f,30f);
        fpsSpeedFast     = fpsSpeedSlow * 20;
        fpsSpeedVeryFast = fpsSpeedFast * 50;
        log.debug( "FPS speeds: slow=" + fpsSpeedSlow + ", fast=" + fpsSpeedFast + ", very fast=" + fpsSpeedVeryFast );
    }
    public void setFPSSpeedSlow( float slowSpeed ) {
        fpsSpeedSlow = speedWithinBounds(slowSpeed, 0.01f,30f);
    }
    public void setFPSSpeedFast( float fastSpeed ) {
        fpsSpeedFast = speedWithinBounds(fastSpeed, 0.2f,600f);
    }
    public void setFPSSpeedVeryFast( float veryFastSpeed ) {
        fpsSpeedVeryFast = speedWithinBounds(veryFastSpeed, 40f,2000f);
    }

    public float getMouseSpeed() {
        return mouseSpeedMult;
    }

    public void setMouseSpeed( float newspeed ) {
        mouseSpeedMult = speedWithinBounds(newspeed, 0.1f,3.0f);
        log.debug( "Mouse speed: " + mouseSpeedMult );
    }

    /*
     * Reset first-person-shooter (FPS) style behaviour in the input handler.
     * The main deal here is to provide new camera movement objects with the
     * fresh delta-move constants.
     */
    public void resetFPSBehaviours() {
        final InputHandler h = getInputHandler();
        if(h == null) {
            getLogger().error("InputHandler is null, cannot change bindings.");
            return;
        }

        //'WASD' from Scenery
        h.addBehaviour( "move_forward", new MovementCommand( "move_forward",  "forward", () -> getScene().findObserver(), fpsSpeedSlow ) );
        h.addBehaviour( "move_back",    new MovementCommand( "move_backward", "back",    () -> getScene().findObserver(), fpsSpeedSlow ) );
        h.addBehaviour( "move_left",    new MovementCommand( "move_left",     "left",    () -> getScene().findObserver(), fpsSpeedSlow ) );
        h.addBehaviour( "move_right",   new MovementCommand( "move_right",    "right",   () -> getScene().findObserver(), fpsSpeedSlow ) );

        //shift+'WASD' from Scenery
        h.addBehaviour( "move_forward_fast", new MovementCommand( "move_forward",  "forward", () -> getScene().findObserver(), fpsSpeedFast ) );
        h.addBehaviour( "move_back_fast",    new MovementCommand( "move_backward", "back",    () -> getScene().findObserver(), fpsSpeedFast ) );
        h.addBehaviour( "move_left_fast",    new MovementCommand( "move_left",     "left",    () -> getScene().findObserver(), fpsSpeedFast ) );
        h.addBehaviour( "move_right_fast",   new MovementCommand( "move_right",    "right",   () -> getScene().findObserver(), fpsSpeedFast ) );

        //shift+ctrl+'WASD' defined here in inputSetup()
        h.addBehaviour("move_forward_veryfast", new MovementCommand( "move_forward", "forward", () -> getScene().findObserver(), fpsSpeedVeryFast ));
        h.addBehaviour("move_back_veryfast",    new MovementCommand( "move_back",    "back",    () -> getScene().findObserver(), fpsSpeedVeryFast ));
        h.addBehaviour("move_left_veryfast",    new MovementCommand( "move_left",    "left",    () -> getScene().findObserver(), fpsSpeedVeryFast ));
        h.addBehaviour("move_right_veryfast",   new MovementCommand( "move_right",   "right",   () -> getScene().findObserver(), fpsSpeedVeryFast ));

        //[[ctrl]+shift]+'XC'
        h.addBehaviour("move_up_slow",       new MovementCommand( "move_up",   "up",   () -> getScene().findObserver(), fpsSpeedSlow ));
        h.addBehaviour("move_down_slow",     new MovementCommand( "move_down", "down", () -> getScene().findObserver(), fpsSpeedSlow ));
        h.addBehaviour("move_up_fast",       new MovementCommand( "move_up",   "up",   () -> getScene().findObserver(), fpsSpeedFast ));
        h.addBehaviour("move_down_fast",     new MovementCommand( "move_down", "down", () -> getScene().findObserver(), fpsSpeedFast ));
        h.addBehaviour("move_up_veryfast",   new MovementCommand( "move_up",   "up",   () -> getScene().findObserver(), fpsSpeedVeryFast ));
        h.addBehaviour("move_down_veryfast", new MovementCommand( "move_down", "down", () -> getScene().findObserver(), fpsSpeedVeryFast ));
    }

    public void setObjectSelectionMode() {
        Function3<Scene.RaycastResult, Integer, Integer, Unit> selectAction = (nearest,x,y) -> {
            if( !nearest.getMatches().isEmpty() ) {
                // copy reference on the last object picking result into "public domain"
                // (this must happen before the ContextPopUpNodeChooser menu!)
                objectSelectionLastResult = nearest;

                // Setup the context menu for this picking
                // (in the menu, the user will chose node herself)
                new ContextPopUpNodeChooser(this).show(panel,x,y);
            }
            return Unit.INSTANCE;
        };
        setObjectSelectionMode(selectAction);
    }

    public Scene.RaycastResult objectSelectionLastResult;

    /*
     * Set the action used during object selection
     */
    public void setObjectSelectionMode(Function3<Scene.RaycastResult, Integer, Integer, Unit> selectAction) {
        final InputHandler h = getInputHandler();
        List<Class<?>> ignoredObjects = new ArrayList<>();
        ignoredObjects.add( BoundingGrid.class );
        ignoredObjects.add( Camera.class ); //do not mess with "scene params", allow only "scene data" to be selected
        ignoredObjects.add( DetachedHeadCamera.class );
        ignoredObjects.add( DirectionalLight.class );
        ignoredObjects.add( PointLight.class );

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

        // Mouse: node-translate controls
        setObjectSelectionMode();
        NodeTranslateControl nodeTranslate = new NodeTranslateControl(this, 0.005f);
        h.addBehaviour(  "mouse_control_nodetranslate", nodeTranslate );
        h.addKeyBinding( "mouse_control_nodetranslate", "ctrl button1" );
        h.addBehaviour(  "mouse_control_noderotate", new NodeRotateControl(this) );
        h.addKeyBinding( "mouse_control_noderotate", "ctrl shift button1" );
        h.addBehaviour(  "scroll_nodetranslate", nodeTranslate );
        h.addKeyBinding( "scroll_nodetranslate", "ctrl scroll" );

        // Mouse: within-scene navigation: ArcBall and FPS
        enableArcBallControl();
        enableFPSControl();

        // Keyboard for FPS-control:
        // behaviours defined already in resetFPSBehaviours() called from enableFPSControl() just above,
        // 'WASD' and shift+'WASD' keys are registered already in scenery
        h.addKeyBinding("move_forward_veryfast", "ctrl shift W");
        h.addKeyBinding("move_back_veryfast",    "ctrl shift S");
        h.addKeyBinding("move_left_veryfast",    "ctrl shift A");
        h.addKeyBinding("move_right_veryfast",   "ctrl shift D");

        h.addKeyBinding("move_up_slow",       "C");
        h.addKeyBinding("move_down_slow",     "X");
        h.addKeyBinding("move_up_fast",       "shift C");
        h.addKeyBinding("move_down_fast",     "shift X");
        h.addKeyBinding("move_up_veryfast",   "ctrl shift C");
        h.addKeyBinding("move_down_veryfast", "ctrl shift X");

        //TODO: R, shift+R, shift-button2 VLADO Vlado vlado

        h.addBehaviour(  "enable_decrease", new enableDecrease() );
        h.addKeyBinding( "enable_decrease", "MINUS" );

        h.addBehaviour(  "enable_increase", new enableIncrease() );
        h.addKeyBinding( "enable_increase", "EQUALS" );

        // Extra keyboard controls
        h.addBehaviour(  "show_help", new showHelpDisplay() );
        h.addKeyBinding( "show_help", "F1" );
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

        Vector3f target;
        if( getActiveNode() == null ) {
            target = new Vector3f( 0, 0, 0 );
        } else {
            target = getActiveNode().getPosition();
        }

        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        targetArcball = new AnimatedCenteringBeforeArcBallControl( "mouse_control_arcball", cameraSupplier,
                                                  getRenderer().getWindow().getWidth(),
                                                  getRenderer().getWindow().getHeight(), target );
        targetArcball.setMaximumDistance( Float.MAX_VALUE );
        targetArcball.setMouseSpeedMultiplier( mouseSpeedMult );
        targetArcball.setScrollSpeedMultiplier( 0.05f );
        targetArcball.setDistance( getCamera().getPosition().sub( target ).length() );

        h.addBehaviour(  "mouse_control_arcball", targetArcball );
        h.addKeyBinding( "mouse_control_arcball", "shift button1" );
        h.addBehaviour(  "scroll_arcball", targetArcball );
        h.addKeyBinding( "scroll_arcball", "shift scroll" );
    }

    /*
     * A wrapping class for the {@ArcballCameraControl} that calls {@link CenterOnPosition()}
     * before the actual Arcball camera movement takes place. This way, the targeted node is
     * first smoothly brought into the centre along which Arcball is revolving, preventing
     * from sudden changes of view (and lost of focus from the user.
     */
    class AnimatedCenteringBeforeArcBallControl extends ArcballCameraControl {
        //a bunch of necessary c'tors (originally defined in the ArcballCameraControl class)
        public AnimatedCenteringBeforeArcBallControl(@NotNull String name, @NotNull Function0<? extends Camera> n, int w, int h, @NotNull Function0<? extends Vector3f> target) {
            super(name, n, w, h, target);
        }

        public AnimatedCenteringBeforeArcBallControl(@NotNull String name, @NotNull Supplier<Camera> n, int w, int h, @NotNull Supplier<Vector3f> target) {
            super(name, n, w, h, target);
        }

        public AnimatedCenteringBeforeArcBallControl(@NotNull String name, @NotNull Function0<? extends Camera> n, int w, int h, @NotNull Vector3f target) {
            super(name, n, w, h, target);
        }

        public AnimatedCenteringBeforeArcBallControl(@NotNull String name, @NotNull Supplier<Camera> n, int w, int h, @NotNull Vector3f target) {
            super(name, n, w, h, target);
        }

        @Override
        public void init( int x, int y )
        {
            centerOnPosition( targetArcball.getTarget().invoke() );
            super.init(x,y);
        }

        @Override
        public void scroll(double wheelRotation, boolean isHorizontal, int x, int y)
        {
            centerOnPosition( targetArcball.getTarget().invoke() );
            super.scroll(wheelRotation,isHorizontal,x,y);
        }
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

        h.addBehaviour(  "mouse_control", fpsControl );
        h.addKeyBinding( "mouse_control", "button1" );

        //slow and fast camera motion
        h.addBehaviour(  "mouse_control_cameratranslate", new CameraTranslateControl( this, 1f ) );
        h.addKeyBinding( "mouse_control_cameratranslate", "button3" );
        //
        //fast and very fast camera motion
        h.addBehaviour(  "mouse_control_cameratranslateF", new CameraTranslateControl( this, 10f ) );
        h.addKeyBinding( "mouse_control_cameratranslateF", "shift button3" );

        resetFPSBehaviours();
    }

    /**
     * Add a box to the scene with default parameters
     * @return the Node corresponding to the box
     */
    public Node addBox() {
        return addBox( new JOMLVector3( 0.0f, 0.0f, 0.0f ) );
    }

    /**
     * Add a box at the specific position and unit size
     * @param position position to put the box
     * @return the Node corresponding to the box
     */
    public Node addBox( Vector3 position ) {
        return addBox( position, new JOMLVector3( 1.0f, 1.0f, 1.0f ) );
    }

    /**
     * Add a box at the specified position and with the specified size
     * @param position position to put the box
     * @param size size of the box
     * @return the Node corresponding to the box
     */
    public Node addBox( Vector3 position, Vector3 size ) {
        return addBox( position, size, DEFAULT_COLOR, false );
    }

    /**
     * Add a box at the specified position with specified size, color, and normals on the inside/outside
     * @param position position to put the box
     * @param size size of the box
     * @param color color of the box
     * @param inside are normals inside the box?
     * @return the Node corresponding to the box
     */
    public Node addBox( final Vector3 position, final Vector3 size, final ColorRGB color,
                                         final boolean inside ) {
        // TODO: use a material from the current palate by default
        final Material boxmaterial = new Material();
        boxmaterial.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
        boxmaterial.setDiffuse( Utils.convertToVector3f( color ) );
        boxmaterial.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );

        final Box box = new Box( JOMLVector3.convert( size ), inside );
        box.setMaterial( boxmaterial );
        box.setPosition( JOMLVector3.convert( position ) );

        return addNode( box );
    }

    /**
     * Add a unit sphere at the origin
     * @return the Node corresponding to the sphere
     */
    public Node addSphere() {
        return addSphere( new JOMLVector3( 0.0f, 0.0f, 0.0f ), 1 );
    }

    /**
     * Add a sphere at the specified position with a given radius
     * @param position position to put the sphere
     * @param radius radius of the sphere
     * @return the Node corresponding to the sphere
     */
    public Node addSphere( Vector3 position, float radius ) {
        return addSphere( position, radius, DEFAULT_COLOR );
    }

    /**
     * Add a sphere at the specified positoin with a given radius and color
     * @param position position to put the sphere
     * @param radius radius the sphere
     * @param color color of the sphere
     * @return  the Node corresponding to the sphere
     */
    public Node addSphere( final Vector3 position, final float radius, final ColorRGB color ) {
        final Material material = new Material();
        material.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( Utils.convertToVector3f( color ) );
        material.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );

        final Sphere sphere = new Sphere( radius, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( JOMLVector3.convert( position ) );

        return addNode( sphere );
    }

    /**
     * Add a Cylinder at the given position with radius, height, and number of faces/segments
     * @param position position of the cylinder
     * @param radius radius of the cylinder
     * @param height height of the cylinder
     * @param num_segments number of segments to represent the cylinder
     * @return  the Node corresponding to the cylinder
     */
    public Node addCylinder( final Vector3 position, final float radius, final float height, final int num_segments ) {
        final Cylinder cyl = new Cylinder( radius, height, num_segments );
        cyl.setPosition( JOMLVector3.convert( position ) );
        return addNode( cyl );
    }

    /**
     * Add a Cone at the given position with radius, height, and number of faces/segments
     * @param position position to put the cone
     * @param radius radius of the cone
     * @param height height of the cone
     * @param num_segments number of segments used to represent cone
     * @return  the Node corresponding to the cone
     */
    public Node addCone( final Vector3 position, final float radius, final float height, final int num_segments ) {
        final Cone cone = new Cone( radius, height, num_segments, new Vector3f(0,0,1) );
        cone.setPosition( JOMLVector3.convert( position ) );
        return addNode( cone );
    }

    /**
     * Add a Line from 0,0,0 to 1,1,1
     * @return  the Node corresponding to the line
     */
    public Node addLine() {
        return addLine( new JOMLVector3( 0.0f, 0.0f, 0.0f ), new JOMLVector3( 1.0f, 1.0f, 1.0f ) );
    }

    /**
     * Add a line from start to stop
     * @param start start position of line
     * @param stop stop position of line
     * @return  the Node corresponding to the line
     */
    public Node addLine( Vector3 start, Vector3 stop ) {
        return addLine( start, stop, DEFAULT_COLOR );
    }

    /**
     * Add a line from start to stop with the given color
     * @param start start position of line
     * @param stop stop position of line
     * @param color color of line
     * @return the Node corresponding to the line
     */
    public Node addLine( Vector3 start, Vector3 stop, ColorRGB color ) {
        return addLine( new Vector3[] { start, stop }, color, 0.1f );
    }

    /**
     * Add a multi-segment line that goes through the supplied points with a single color and edge width
     * @param points points along line including first and terminal points
     * @param color color of line
     * @param edgeWidth width of line segments
     * @return the Node corresponding to the line
     */
    public Node addLine( final Vector3[] points, final ColorRGB color, final double edgeWidth ) {
        final Material material = new Material();
        material.setAmbient( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        material.setDiffuse( Utils.convertToVector3f( color ) );
        material.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );

        final Line line = new Line( points.length );
        for( final Vector3 pt : points ) {
            line.addPoint( JOMLVector3.convert( pt ) );
        }

        line.setEdgeWidth( ( float ) edgeWidth );

        line.setMaterial( material );
        line.setPosition( JOMLVector3.convert( points[0] ) );

        return addNode( line );
    }

    /**
     * Add a PointLight source at the origin
     * @return a Node corresponding to the PointLight
     */
    public Node addPointLight() {
        final Material material = new Material();
        material.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new Vector3f( 0.0f, 1.0f, 0.0f ) );
        material.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );

        final PointLight light = new PointLight( 5.0f );
        light.setMaterial( material );
        light.setPosition( new Vector3f( 0.0f, 0.0f, 0.0f ) );
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
        Vector3f c = boundingSphere.getOrigin();
        float r = boundingSphere.getRadius();
        for( int k = 0; k < lights.size(); k++ ) {
            PointLight light = lights.get(k);
            float x = (float) (c.x() + r * Math.cos( k == 0 ? 0 : Math.PI * 2 * ((float)k / (float)lights.size()) ));
            float z = (float) (c.y() + r * Math.sin( k == 0 ? 0 : Math.PI * 2 * ((float)k / (float)lights.size()) ));
            light.setLightRadius( 2 * r );
            light.setPosition( new Vector3f( x, y, z ) );
        }
    }

    /**
     * Write a scenery mesh as an stl to the given file
     * @param filename filename of the stl
     * @param scMesh mesh to save
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
     * @return default point size used for point clouds
     */
    public float getDefaultPointSize() {
        return 0.025f;
    }

    /**
     * Create an array of normal vectors from a set of vertices corresponding to triangles
     *
     * @param verts vertices to use for computing normals, assumed to be ordered as triangles
     * @return array of normals
     */
    public float[] makeNormalsFromVertices( ArrayList<RealPoint> verts ) {
        float[] normals = new float[verts.size()];// div3 * 3coords

        for( int k = 0; k < verts.size(); k += 3 ) {
            Vector3f v1 = new Vector3f( verts.get( k ).getFloatPosition( 0 ), //
                                        verts.get( k ).getFloatPosition( 1 ), //
                                        verts.get( k ).getFloatPosition( 2 ) );
            Vector3f v2 = new Vector3f( verts.get( k + 1 ).getFloatPosition( 0 ),
                                        verts.get( k + 1 ).getFloatPosition( 1 ),
                                        verts.get( k + 1 ).getFloatPosition( 2 ) );
            Vector3f v3 = new Vector3f( verts.get( k + 2 ).getFloatPosition( 0 ),
                                        verts.get( k + 2 ).getFloatPosition( 1 ),
                                        verts.get( k + 2 ).getFloatPosition( 2 ) );
            Vector3f a = v2.sub( v1 );
            Vector3f b = v3.sub( v1 );
            Vector3f n = a.cross( b ).normalize();
            normals[k / 3] = n.get( 0 );
            normals[k / 3 + 1] = n.get( 1 );
            normals[k / 3 + 2] = n.get( 2 );
        }
        return normals;
    }

    /**
     * Open a file specified by the source path. The file can be anything that SciView knows about: mesh, volume, point cloud
     * @param source string of a data source
     * @throws IOException
     */
    public void open( final String source ) throws IOException {
        if(source.endsWith(".xml")) {
            addNode(Volume.Companion.fromXML(source, getHub(), new VolumeViewerOptions()));
            return;
        }

        final Object data = io.open( source );
        if( data instanceof net.imagej.mesh.Mesh ) addMesh( ( net.imagej.mesh.Mesh ) data );
        else if( data instanceof Mesh ) addMesh( ( Mesh ) data );
        else if( data instanceof PointCloud ) addPointCloud( ( PointCloud ) data );
        else if( data instanceof Dataset ) addVolume( ( Dataset ) data );
        else if( data instanceof RandomAccessibleInterval ) addVolume( ( ( RandomAccessibleInterval ) data ), source );
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
     * @param points points to use in a PointCloud
     * @return a Node corresponding to the PointCloud
     */
    public Node addPointCloud( Collection<? extends RealLocalizable> points ) {
        return addPointCloud( points, "PointCloud" );
    }

    /**
     * Add the given points to the scene as a PointCloud with a given name
     * @param points points to use in a PointCloud
     * @param name name of the PointCloud
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
        material.setAmbient( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        material.setDiffuse( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        material.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        pointCloud.setMaterial( material );
        pointCloud.setPosition( new Vector3f( 0f, 0f, 0f ) );

        return addNode( pointCloud );
    }

    /**
     * Add a PointCloud to the scene
     * @param pointCloud existing PointCloud to add to scene
     * @return a Node corresponding to the PointCloud
     */
    public Node addPointCloud( final PointCloud pointCloud ) {
        pointCloud.setupPointCloud();
        pointCloud.getMaterial().setAmbient( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setDiffuse( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );
        pointCloud.setPosition( new Vector3f( 0f, 0f, 0f ) );

        return addNode( pointCloud );
    }

    /**
     * Add a Node to the scene and publish it to the eventservice
     * @param n node to add to scene
     * @return a Node corresponding to the Node
     */
    public Node addNode( final Node n ) {
        return addNode(n, true);
    }

    /**
     * Add Node n to the scene and set it as the active node/publish it to the event service if activePublish is true
     * @param n node to add to scene
     * @param activePublish flag to specify whether the node becomes active *and* is published in the inspector/services
     * @return a Node corresponding to the Node
     */
    public Node addNode( final Node n, final boolean activePublish ) {
        getScene().addChild( n );

        objectService.addObject(n);

        if( blockOnNewNodes ) {
            blockWhile(sciView -> (sciView.find(n.getName()) == null), 20);
            //System.out.println("find(name) " + find(n.getName()) );
        }

        // Set new node as active and centered?
        setActiveNode(n);
        if( centerOnNewNodes ) centerOnNode(n);
        if( activePublish ) eventService.publish(new NodeAddedEvent(n));

        return n;
    }

    /**
     * Add a scenery Mesh to the scene
     * @param scMesh scenery mesh to add to scene
     * @return a Node corresponding to the mesh
     */
    public Node addMesh( final Mesh scMesh ) {
        final Material material = new Material();
        material.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new Vector3f( 0.0f, 1.0f, 0.0f ) );
        material.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );

        scMesh.setMaterial( material );
        scMesh.setPosition( new Vector3f( 0.0f, 0.0f, 0.0f ) );

        objectService.addObject(scMesh);

        return addNode( scMesh );
    }

    /**
     * Add an ImageJ mesh to the scene
     * @param mesh net.imagej.mesh to add to scene
     * @return a Node corresponding to the mesh
     */
    public Node addMesh( net.imagej.mesh.Mesh mesh ) {
        Mesh scMesh = MeshConverter.toScenery( mesh );

        return addMesh( scMesh );
    }

    /**
     * [Deprecated: use deleteNode]
     * Remove a Mesh from the scene
     * @param scMesh mesh to remove from scene
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
     * Activate the node (without centering view on it). The node becomes a target
     * of the Arcball camera movement, will become subject of the node dragging
     * (ctrl[+shift]+mouse-left-click-and-drag), will be selected in the scene graph
     * inspector (the {@link NodePropertyEditor})
     * and {@link sc.iview.event.NodeActivatedEvent} will be published.
     *
     * @param n existing node that should become active focus of this SciView
     * @return the currently active node
     */
    public Node setActiveNode( Node n ) {
        if( activeNode == n ) return activeNode;
        activeNode = n;
        targetArcball.setTarget( n == null ? () -> new Vector3f( 0, 0, 0 ) : () -> n.getMaximumBoundingBox().getBoundingSphere().getOrigin());
        nodePropertyEditor.trySelectNode( activeNode );
        eventService.publish( new NodeActivatedEvent( activeNode ) );

        return activeNode;
    }

    /**
     * Activate the node, and center the view on it.
     * @param n
     * @return the currently active node
     */
    public Node setActiveCenteredNode( Node n ) {
        //activate...
        Node ret = setActiveNode(n);
        //...and center it
        if (ret != null) centerOnNode(ret);
        return ret;
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
        toggleSidebar();
    }

    public void setInspectorWindowVisibility(boolean visible)
    {
//        inspector.setVisible(visible);
//        if( visible )
//            mainSplitPane.setDividerLocation(getWindowWidth()/4 * 3);
//        else
//            mainSplitPane.setDividerLocation(getWindowWidth());
    }

    public void setInterpreterWindowVisibility(boolean visible)
    {
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
     * @param path path for saving the screenshot
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
     * Take a screenshot and return it as an Img
     * @return an Img of type UnsignedByteType
     */
    public Img<ARGBType> getARGBScreenshot() {
        Img<UnsignedByteType> screenshot = getScreenshot();

        return Utils.convertToARGB(screenshot);
    }

    /**
     * @param name The name of the node to find.
     * @return the node object or null, if the node has not been found.
     */
    public Node find(final String name) {
        final Node n = getScene().find(name);

        if(n == null) {
            getLogger().warn("Node with name " + name + " not found.");
        }

        return n;
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
     * @param node node to delete from scene
     */
    public void deleteNode( Node node ) {
        deleteNode( node, true );
    }

    /**
     * Delete a specified node and control whether the event is published
     * @param node node to delete from scene
     * @param activePublish whether the deletion should be published
     */
    public void deleteNode( Node node, boolean activePublish ) {
        for( Node child : node.getChildren() ) {
            deleteNode(child, activePublish);
        }

        objectService.removeObject(node);
        node.getParent().removeChild( node );
        if (activeNode == node) setActiveNode(null); //maintain consistency
        if( activePublish ) eventService.publish(new NodeRemovedEvent(node));
    }

    /**
     * Dispose the current scenery renderer, hub, and other scenery things
     */
    public void dispose() {
        List<Node> objs = objectService.getObjects(Node.class);
        for( Node obj : objs ) {
            objectService.removeObject(obj);
        }
        getScijavaContext().service(SciViewService.class).close(this);
        this.close();
    }


    public void close() {
        super.close();

        frame.dispose();
    }

    /**
     * Move the current active camera to the specified position
     * @param position position to move the camera to
     */
    public void moveCamera( float[] position ) {
        getCamera().setPosition( new Vector3f( position[0], position[1], position[2] ) );
    }

    /**
     * Move the current active camera to the specified position
     * @param position position to move the camera to
     */
    public void moveCamera( double[] position ) {
        getCamera().setPosition( new Vector3f( ( float ) position[0], ( float ) position[1], ( float ) position[2] ) );
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
     * @param node node to add as a child to the scene
     */
    public void addChild( Node node ) {
        getScene().addChild( node );
    }

    /**
     * Add a Dataset to the scene as a volume. Voxel resolution and name are extracted from the Dataset itself
     * @param image image to add as a volume
     * @return a Node corresponding to the Volume
     */
    public Node addVolume( Dataset image ) {

        float[] voxelDims = new float[image.numDimensions()];
        for( int d = 0; d < voxelDims.length; d++ ) {
            double inValue = image.axis(d).averageScale(0, 1);
            if( image.axis(d).unit() == null )
                voxelDims[d] = (float) inValue;
            else
                voxelDims[d] = (float) unitService.value( inValue, image.axis(d).unit(), axis(d).unit() );
        }

        return addVolume( image, voxelDims );
    }

    /**
     * Add a Dataset as a Volume with the specified voxel dimensions
     * @param image image to add as a volume
     * @param voxelDimensions dimensions of voxels in volume
     * @return a Node corresponding to the Volume
     */
    @SuppressWarnings({ "rawtypes", "unchecked" }) public Node addVolume( Dataset image, float[] voxelDimensions ) {
        return addVolume( ( RandomAccessibleInterval ) image.getImgPlus(), image.getName(),
                          voxelDimensions );
    }

    /**
     * Add a RandomAccessibleInterval to the image
     * @param image image to add as a volume
     * @param name name of image
     * @param extra, kludge argument to prevent matching issues
     * @param <T> pixel type of image
     * @return a Node corresponding to the volume
     */
    public <T extends RealType<T>> Node addVolume( RandomAccessibleInterval<T> image, String name, String extra ) {
        return addVolume( image, name, 1, 1, 1 );
    }

    /**
     * Add a RandomAccessibleInterval to the image
     * @param image image to add as a volume
     * @param <T> pixel type of image
     * @return a Node corresponding to the volume
     */
    public <T extends RealType<T>> Node addVolume(RandomAccessibleInterval<T> image, String name) {
        return addVolume(image, name, 1f, 1f, 1f);
    }

    /**
     * Add a RandomAccessibleInterval to the image
     * @param image image to add as a volume
     * @param <T> pixel type of image
     * @return a Node corresponding to the volume
     */
    public <T extends RealType<T>> Node addVolume( RandomAccessibleInterval<T> image, float[] voxelDimensions ) {
        long[] pos = new long[]{10, 10, 10};

        return addVolume( image, "volume", voxelDimensions );
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image
     * @param <T>
     * @return a Node corresponding to the Volume
     */
    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image ) throws Exception {
        if( image instanceof RandomAccessibleInterval ) {
            return addVolume((RandomAccessibleInterval) image, "Volume");
        } else {
            throw new Exception("Unsupported Volume type:" + image);
        }
    }

    /**
     * Add an IterableInterval as a Volume
     * @param image image to add as a volume
     * @param name name of image
     * @param <T> pixel type of image
     * @return a Node corresponding to the Volume
     */
    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image, String name ) throws Exception {
        if( image instanceof RandomAccessibleInterval ) {
            return addVolume( (RandomAccessibleInterval) image, name, 1, 1, 1 );
        } else {
            throw new Exception("Unsupported Volume type:" + image);
        }
    }

    /**
     * Set the colormap using an ImageJ LUT name
     * @param n node to apply colormap to
     * @param lutName name of LUT according to imagej LUTService
     */
    public void setColormap( Node n, String lutName ) {
        try {
            setColormap( n, lutService.loadLUT( lutService.findLUTs().get( lutName ) ) );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Set the ColorMap of node n to the supplied colorTable
     * @param n node to apply colortable to
     * @param colorTable ColorTable to use
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
            ((Volume) n).setColormap(Colormap.fromColorTable(colorTable));
            n.setDirty(true);
            n.setNeedsUpdate(true);
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
     */
    public <T extends RealType<T>> Node addVolume(SourceAndConverter<T> sac,
                                                  int numTimepoints,
                                                  String name,
                                                  float... voxelDimensions ) {
        List<SourceAndConverter<T>> sources = new ArrayList<>();
        sources.add(sac);

        return addVolume(sources, numTimepoints, name, voxelDimensions);
    }

    /**
     * Add an IterableInterval to the image with the specified voxelDimensions and name
     * This version of addVolume does most of the work
     * @param image image to add as a volume
     * @param name name of image
     * @param voxelDimensions dimensions of voxel in volume
     * @param <T> pixel type of image
     * @return a Node corresponding to the Volume
     */
    public <T extends RealType<T>> Node addVolume( RandomAccessibleInterval<T> image, String name,
                                                                    float... voxelDimensions ) {
        //log.debug( "Add Volume " + name + " image: " + image );

        long[] dimensions = new long[image.numDimensions()];
        image.dimensions( dimensions );

        long[] minPt = new long[image.numDimensions()];

        // Get type at min point
        RandomAccess<T> imageRA = image.randomAccess();
        image.min(minPt);
        imageRA.setPosition(minPt);
        T voxelType = imageRA.get().createVariable();

        ArrayList<ConverterSetup> converterSetups = new ArrayList();
        ArrayList<RandomAccessibleInterval<T>> stacks = AxisOrder.splitInputStackIntoSourceStacks(image, AxisOrder.getAxisOrder(AxisOrder.DEFAULT, image, false));
        AffineTransform3D sourceTransform = new AffineTransform3D();
        ArrayList<SourceAndConverter<T>> sources = new ArrayList();

        int numTimepoints = 1;
        for (RandomAccessibleInterval stack : stacks) {
            Source<T> s;
            if (stack.numDimensions() > 3) {
                numTimepoints = (int) (stack.max(3) + 1);
                s = new RandomAccessibleIntervalSource4D<T>(stack, voxelType, sourceTransform, name);
            } else {
                s = new RandomAccessibleIntervalSource<T>(stack, voxelType, sourceTransform, name);
            }
            SourceAndConverter<T> source = BigDataViewer.wrapWithTransformedSource(
                new SourceAndConverter<T>(s, BigDataViewer.createConverterToARGB(voxelType)));
            converterSetups.add(BigDataViewer.createConverterSetup(source, Volume.Companion.getSetupId().getAndIncrement()));
            sources.add(source);
        }

        Node v = addVolume(sources, numTimepoints, name, voxelDimensions);

        v.getMetadata().put("RandomAccessibleInterval", image);

        return v;
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
     */
    public <T extends RealType<T>> Node addVolume(List<SourceAndConverter<T>> sources,
                                                  ArrayList<ConverterSetup> converterSetups,
                                                  int numTimepoints,
                                                  String name,
                                                  float... voxelDimensions ) {

        CacheControl cacheControl = null;

//        RandomAccessibleInterval<T> image =
//                ((RandomAccessibleIntervalSource4D) sources.get(0).getSpimSource()).
//                .getSource(0, 0);
        RandomAccessibleInterval<T> image = sources.get(0).getSpimSource().getSource(0, 0);

        if (image instanceof VolatileView) {
            VolatileViewData<T, Volatile<T>> viewData  = ((VolatileView<T, Volatile<T>>) image).getVolatileViewData();
            cacheControl = viewData.getCacheControl();
        }

        long[] dimensions = new long[image.numDimensions()];
        image.dimensions( dimensions );

        long[] minPt = new long[image.numDimensions()];

        // Get type at min point
        RandomAccess<T> imageRA = image.randomAccess();
        image.min(minPt);
        imageRA.setPosition(minPt);
        T voxelType = imageRA.get().createVariable();

        System.out.println("addVolume " + image.numDimensions() + " interval " + ((Interval) image) );

        //int numTimepoints = 1;
        if( image.numDimensions() > 3 ) {
            numTimepoints = (int) image.dimension(3);
        }

        Volume.VolumeDataSource.RAISource<T> ds = new Volume.VolumeDataSource.RAISource<T>(voxelType, sources, converterSetups, numTimepoints, cacheControl);
        VolumeViewerOptions options = new VolumeViewerOptions();

        Volume v = new RAIVolume(ds, options, getHub());
        v.setName(name);

        v.getMetadata().put("sources", sources);

        TransferFunction tf = v.getTransferFunction();
        float rampMin = 0f;
        float rampMax = 0.1f;
        tf.clear();
        tf.addControlPoint(0.0f, 0.0f);
        tf.addControlPoint(rampMin, 0.0f);
        tf.addControlPoint(1.0f, rampMax);

        BoundingGrid bg = new BoundingGrid();
        bg.setNode(v);

        return addNode(v);
    }

    /**
     * Block while predicate is true
     *
     * @param predicate predicate function that returns true as long as this function should block
     * @param waitTime wait time before predicate re-evaluation
     */
    private void blockWhile(Function<SciView, Boolean> predicate, int waitTime) {
        while( predicate.apply(this) ) {
            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
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
     */
    public <T extends RealType<T>> Node addVolume(List<SourceAndConverter<T>> sources,
                                                  int numTimepoints,
                                                  String name,
                                                  float... voxelDimensions ) {
        int setupId = 0;
        ArrayList<ConverterSetup> converterSetups = new ArrayList<>();
        for( SourceAndConverter source: sources ) {
            converterSetups.add(BigDataViewer.createConverterSetup(source, setupId++));
        }

        return addVolume(sources, converterSetups, numTimepoints, name, voxelDimensions);
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
     */
    public <T extends RealType<T>> Node updateVolume( IterableInterval<T> image, String name,
                                                                       float[] voxelDimensions, Volume v ) {
        List<SourceAndConverter<T>> sacs = (List<SourceAndConverter<T>>) v.getMetadata().get("sources");

        RandomAccessibleInterval<T> source = sacs.get(0).getSpimSource().getSource(0, 0);// hard coded to timepoint and mipmap 0

        Cursor<T> sCur = Views.iterable(source).cursor();
        Cursor<T> iCur = image.cursor();
        while( sCur.hasNext() ) {
            sCur.fwd();
            iCur.fwd();
            sCur.get().set(iCur.get());
        }

        v.getVolumeManager().notifyUpdate(v);
        v.getVolumeManager().requestRepaint();
        //v.getCacheControls().clear();
        //v.setDirty( true );
        v.setNeedsUpdate( true );
        //v.setNeedsUpdateWorld( true );

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
     * @param push true if push mode should be used
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

    public Statistics getSceneryStatistics() {
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

            while(!getRenderer().getInitialized()/* || !getRenderer().getFirstImageReady()*/) {
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
     * @param x x coord
     * @param y y coord
     * @param z z coord
     * @return a GLVector of x,y,z
     */
    static public GLVector getGLVector(float x, float y, float z) {
        return new GLVector(x, y, z);
    }

    /**
     * Set the rotation of Node N by generating a quaternion from the supplied arguments
     * @param n node to set rotation for
     * @param x x coord of rotation quat
     * @param y y coord of rotation quat
     * @param z z coord of rotation quat
     * @param w w coord of rotation quat
     */
    public void setRotation(Node n, float x, float y, float z, float w) {
        n.setRotation(new Quaternionf(x,y,z,w));
    }

    public void setScale(Node n, float x, float y, float z) {
        n.setScale(new Vector3f(x,y,z));
    }

    public void setColor(Node n, float x, float y, float z, float w) {
        Vector3f col = new Vector3f(x, y, z);
        n.getMaterial().setAmbient(col);
        n.getMaterial().setDiffuse(col);
        n.getMaterial().setSpecular(col);
    }

    public void setPosition(Node n, float x, float y, float z) {
        n.setPosition(new Vector3f(x,y,z));
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

    public void setCamera(Camera camera) {
        this.camera = camera;
        setActiveObserver(camera);
    }

    public void setActiveObserver(Camera screenshotCam) {
        getScene().setActiveObserver(screenshotCam);
    }

    public Camera getActiveObserver() {
        return getScene().getActiveObserver();
    }

    public void setCenterOnNewNodes(boolean centerOnNewNodes) {
        this.centerOnNewNodes = centerOnNewNodes;
    }

    public boolean getCenterOnNewNodes() {
        return centerOnNewNodes;
    }

    public void setBlockOnNewNodes(boolean blockOnNewNodes) {
        this.blockOnNewNodes = blockOnNewNodes;
    }

    public boolean getBlockOnNewNodes() {
        return blockOnNewNodes;
    }

    public class TransparentSlider extends JSlider {

        public TransparentSlider() {
            // Important, we taking over the filling of the
            // component...
            setOpaque(false);
            setBackground(Color.DARK_GRAY);
            setForeground(Color.LIGHT_GRAY);
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
            setFPSSpeed( getFPSSpeedSlow() + 0.01f );
            setMouseSpeed( getMouseSpeed() + 0.05f );

            resetFPSBehaviours();
        }
    }

    class enableDecrease implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            setFPSSpeed( getFPSSpeedSlow() - 0.01f );
            setMouseSpeed( getMouseSpeed() - 0.05f );

            resetFPSBehaviours();
        }
    }

    class showHelpDisplay implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            StringBuilder helpString = new StringBuilder("SciView help:\n\n");
            for( InputTrigger trigger : getInputHandler().getAllBindings().keySet() ) {
                helpString.append(trigger).append("\t-\t").append(getInputHandler().getAllBindings().get(trigger)).append("\n");
            }
            // HACK: Make the console pop via stderr.
            // Later, we will use a nicer dialog box or some such.
            log.warn(helpString.toString());
        }
    }

    /**
     * Return a list of all nodes that match a given predicate function
     * @param nodeMatchPredicate, returns true if a node is a match
     * @return list of nodes that match the predicate
     */
    public List<Node> findNodes(Function1<Node, Boolean> nodeMatchPredicate) {
        return getScene().discover(getScene(), nodeMatchPredicate, false);
    }

    /*
     * Convenience function for getting a string of info about a Node
     */
    public String nodeInfoString(Node n) {
        return "Node name: " + n.getName() + " Node type: " + n.getNodeType() + " To String: " + n;
    }

    /**
     * Static launching method
     *
     * @return a newly created SciView
     */
    public static SciView create() throws Exception {
        SceneryBase.xinitThreads();

        FlatLightLaf.install();
        try {
            UIManager.setLookAndFeel( new FlatLightLaf() );
        } catch( Exception ex ) {
            System.err.println( "Failed to initialize Flat Light LaF, falling back to Swing default." );
        }

        System.setProperty( "scijava.log.level:sc.iview", "debug" );
        Context context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class, ThreadService.class);

        SciViewService sciViewService = context.service( SciViewService.class );
        SciView sciView = sciViewService.getOrCreateActiveSciView();

        return sciView;
    }

    /**
     * Static launching method
     * [DEPRECATED] use SciView.create() instead
     *
     * @return a newly created SciView
     */
    @Deprecated
    public static SciView createSciView() throws Exception {
        return create();
    }

}

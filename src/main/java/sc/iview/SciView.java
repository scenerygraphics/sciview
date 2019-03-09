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
import com.jogamp.opengl.math.Quaternion;
import com.sun.javafx.application.PlatformImpl;
import coremem.enums.NativeTypeEnum;
import graphics.scenery.Box;
import graphics.scenery.*;
import graphics.scenery.backends.Renderer;
import graphics.scenery.controls.InputHandler;
import graphics.scenery.controls.behaviours.ArcballCameraControl;
import graphics.scenery.controls.behaviours.FPSCameraControl;
import graphics.scenery.controls.behaviours.MovementCommand;
import graphics.scenery.controls.behaviours.SelectCommand;
import graphics.scenery.utils.SceneryFXPanel;
import graphics.scenery.utils.SceneryJPanel;
import graphics.scenery.utils.SceneryPanel;
import graphics.scenery.utils.Statistics;
import graphics.scenery.volumes.Volume;
import graphics.scenery.volumes.bdv.BDVVolume;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.geometry.Insets;
import javafx.geometry.*;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
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
import org.scijava.event.EventService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.ui.behaviour.InputTrigger;
import org.scijava.ui.swing.menu.SwingJMenuBarCreator;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;
import org.scijava.util.Colors;
import sc.iview.commands.edit.NodePropertyEditor;
import sc.iview.controls.behaviours.CameraTranslateControl;
import sc.iview.controls.behaviours.NodeTranslateControl;
import sc.iview.event.NodeActivatedEvent;
import sc.iview.event.NodeAddedEvent;
import sc.iview.event.NodeRemovedEvent;
import sc.iview.javafx.JavaFXMenuCreator;
import sc.iview.process.MeshConverter;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;
import tpietzsch.example2.VolumeViewerOptions;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Queue;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;

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
     * JavaFX UI
     */
    private boolean useJavaFX = false;

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

    private Label statusLabel;
    private Label loadingLabel;
    private JLabel splashLabel;
    private SceneryJPanel panel;
    private StackPane stackPane;
    private MenuBar menuBar;
    private final SceneryPanel[] sceneryPanel = { null };
    private JSlider timepointSlider = null;

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
        int x, y;

        try {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();

            x = screenSize.width/2 - getWindowWidth()/2 - 300;
            y = screenSize.height/2 - getWindowHeight()/2;
        } catch(HeadlessException e) {
            x = 10;
            y = 10;
        }

        JFrame frame = new JFrame("SciView");
        frame.setSize(getWindowWidth(), getWindowHeight());
        frame.setLocation(x, y);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        final NodePropertyEditor nodePropertyEditor = new NodePropertyEditor( this );

        if( useJavaFX ) {
            final JFXPanel fxPanel = new JFXPanel();
            frame.add(fxPanel);

            CountDownLatch latch = new CountDownLatch( 1 );

            PlatformImpl.startup( () -> {
            } );

            Platform.runLater( () -> {
                stackPane = new StackPane();
                stackPane.setBackground(
                        new Background( new BackgroundFill( Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY ) ) );

                GridPane pane = new GridPane();
                statusLabel = new Label( "SciView - press U for usage help" );
                statusLabel.setVisible(false);

                final SceneryFXPanel panel = new SceneryFXPanel(100, 100);

                Image loadingImage = new Image(this.getClass().getResourceAsStream("sciview-logo.png"), 600, 200, true, true);
                ImageView loadingImageView = new ImageView(loadingImage);
                loadingLabel = new Label("SciView is starting.");
                loadingLabel.setStyle(
                        "-fx-background-color: rgb(50,48,47);" +
                        "-fx-opacity: 1.0;" +
                        "-fx-font-weight: 400; " +
                        "-fx-font-size: 2.2em; " +
                        "-fx-text-fill: white;");
                loadingLabel.setTextFill(Paint.valueOf("white"));
                loadingLabel.setGraphic(loadingImageView);
                loadingLabel.setGraphicTextGap(40.0);
                loadingLabel.setContentDisplay(ContentDisplay.TOP);
                loadingLabel.prefHeightProperty().bind(pane.heightProperty());
                loadingLabel.prefWidthProperty().bind(pane.widthProperty());
                loadingLabel.setAlignment(Pos.CENTER);

                GridPane.setHgrow( panel, Priority.ALWAYS );
                GridPane.setVgrow( panel, Priority.ALWAYS );

                GridPane.setFillHeight( panel, true );
                GridPane.setFillWidth( panel, true );

                GridPane.setHgrow( statusLabel, Priority.ALWAYS );
                GridPane.setHalignment( statusLabel, HPos.CENTER );
                GridPane.setValignment( statusLabel, VPos.BOTTOM );

                statusLabel.maxWidthProperty().bind( pane.widthProperty() );

                pane.setStyle( "-fx-background-color: rgb(50,48,47);" +
                               "-fx-font-family: Helvetica Neue, Helvetica, Segoe, Proxima Nova, Arial, sans-serif;" +
                               "-fx-font-weight: 400;" + "-fx-font-size: 1.2em;" + "-fx-text-fill: white;" +
                               "-fx-text-alignment: center;" );

                statusLabel.setStyle( "-fx-padding: 0.2em;" + "-fx-text-fill: white;" );

                statusLabel.setTextAlignment( TextAlignment.CENTER );

                menuBar = new MenuBar();
                pane.add( menuBar, 1, 1 );
                pane.add( panel, 1, 2 );
                pane.add( statusLabel, 1, 3 );
                stackPane.getChildren().addAll(pane, loadingLabel);

                final ContextMenu contextMenu = new ContextMenu();
                final MenuItem title = new MenuItem("Node");
                final MenuItem position = new MenuItem("Position");
                title.setDisable(true);
                position.setDisable(true);
                contextMenu.getItems().addAll(title, position);

                panel.setOnContextMenuRequested(event -> {
                    final Point2D localPosition = panel.sceneToLocal(event.getSceneX(), event.getSceneY());
                    final List<Scene.RaycastResult> matches = camera.getNodesForScreenSpacePosition((int)localPosition.getX(), (int)localPosition.getY());
                    if(matches.size() > 0) {
                        final Node firstMatch = matches.get(0).getNode();
                        title.setText("Node: " + firstMatch.getName() + " (" + firstMatch.getClass().getSimpleName() + ")");
                        position.setText(firstMatch.getPosition().toString());
                    } else {
                        title.setText("(no matches)");
                        position.setText("");
                    }
                    contextMenu.show(panel, event.getScreenX(), event.getScreenY());
                });

                panel.setOnMouseClicked(event -> {
                    if(event.getButton() == MouseButton.PRIMARY) {
                        contextMenu.hide();
                    }
                });

                sceneryPanel[0] = panel;

                javafx.scene.Scene scene = new javafx.scene.Scene( stackPane );
                fxPanel.setScene(scene);
//                scene.addEventHandler(MouseEvent.ANY, event -> getLogger().info("Mouse event: " + event.toString()));
//                sceneryPanel[0].addEventHandler(MouseEvent.ANY, event -> getLogger().info("PANEL Mouse event: " + event.toString()));

                frame.setVisible(true);
//                stage.setScene( scene );
//                stage.setOnCloseRequest( event -> {
//                    getDisplay().close();
//                    this.close();
//                } );
//                stage.focusedProperty().addListener( ( ov, t, t1 ) -> {
//                    if( t1 )// If you just gained focus
//                        displayService.setActiveDisplay( getDisplay() );
//                } );

                new JavaFXMenuCreator().createMenus( menus.getMenu( "SciView" ), menuBar );

//                stage.show();

                latch.countDown();
            } );

            try {
                latch.await();
            } catch( InterruptedException e1 ) {
                e1.printStackTrace();
            }

            // window width and window height get ignored by the renderer if it is embedded.
            // dimensions are determined from the SceneryFXPanel, then.
            setRenderer( Renderer.createRenderer( getHub(), getApplicationName(), getScene(),
                                                  getWindowWidth(), getWindowHeight(),
                                                  sceneryPanel[0] ) );
        } else {
            final JPanel p = new JPanel(new BorderLayout(0, 0));
            panel = new SceneryJPanel();
            timepointSlider = new JSlider();
            timepointSlider.setPaintTicks(true);
            timepointSlider.setSnapToTicks(true);
            timepointSlider.setPaintLabels(true);
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
            splashLabel = new JLabel(new ImageIcon(splashImage.getScaledInstance(500, 200, java.awt.Image.SCALE_SMOOTH)));
            splashLabel.setBackground(new java.awt.Color(50, 48, 47));

            p.setLayout(new OverlayLayout(p));
            p.setBackground(new java.awt.Color(50, 48, 47));
            p.add(splashLabel, BorderLayout.CENTER);
            p.add(timepointSlider);
            p.add(panel, BorderLayout.CENTER);
            timepointSlider.setAlignmentY(1.0f);
            timepointSlider.setAlignmentX(1.0f);
            timepointSlider.setVisible(false);
            panel.setVisible(false);

            JPanel nodepropPanel = nodePropertyEditor.getComponent();
            nodepropPanel.setVisible(true);

            JTree inspectorTree = nodePropertyEditor.getTree();
            JPanel inspectorProperties = nodePropertyEditor.getProps();

            JSplitPane inspector = new JSplitPane(JSplitPane.VERTICAL_SPLIT, //
                    new JScrollPane( inspectorTree ),
                    new JScrollPane( inspectorProperties ));
            inspector.setDividerLocation( getWindowHeight() / 3 );
            inspector.setContinuousLayout(true);

            JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, //
                    p,
                    inspector
                    );
            mainSplitPane.setDividerLocation( getWindowWidth()/4 * 3 );

            frame.add(mainSplitPane);
            frame.setVisible(true);

            sceneryPanel[0] = panel;

            setRenderer( Renderer.createRenderer( getHub(), getApplicationName(), getScene(),
                    getWindowWidth(), getWindowHeight(),
                    sceneryPanel[0]) );
        }

        // Enable push rendering by default
        getRenderer().setPushMode( true );

        getHub().add( SceneryElement.Renderer, getRenderer() );

        GLVector[] tetrahedron = new GLVector[4];
        tetrahedron[0] = new GLVector( 1.0f, 0f, -1.0f/(float)Math.sqrt(2.0f) );
        tetrahedron[1] = new GLVector( -1.0f,0f,-1.0f/(float)Math.sqrt(2.0) );
        tetrahedron[2] = new GLVector( 0.0f,1.0f,1.0f/(float)Math.sqrt(2.0) );
        tetrahedron[3] = new GLVector( 0.0f,-1.0f,1.0f/(float)Math.sqrt(2.0) );

        PointLight[] lights = new PointLight[4];

        for( int i = 0; i < lights.length; i++ ) {
            lights[i] = new PointLight( 150.0f );
            lights[i].setPosition( tetrahedron[i].times(25.0f) );
            lights[i].setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
            lights[i].setIntensity( 2500.0f );
            getScene().addChild( lights[i] );
        }

        Camera cam = new DetachedHeadCamera();
        cam.setPosition( new GLVector( 0.0f, 5.0f, 5.0f ) );
        cam.perspectiveCamera( 50.0f, getWindowWidth()+600, getWindowHeight(), 0.1f, 1000.0f );
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

        if(useJavaFX) {
            Platform.runLater(() -> {
                while (!getRenderer().getFirstImageReady()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

                // fade out loading screen, show status bar
                FadeTransition ft = new FadeTransition(Duration.millis(500), loadingLabel);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.setCycleCount(1);
                ft.setInterpolator(Interpolator.EASE_OUT);
                ft.setOnFinished(event -> {
                    loadingLabel.setVisible(false);
                    statusLabel.setVisible(true);
                });

                ft.play();
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                try {
                    while (!getSceneryRenderer().getFirstImageReady()) {
                        getLogger().info("Waiting for renderer");
                        Thread.sleep(100);
                    }

                    Thread.sleep(200);
                } catch (InterruptedException e) {
                }

                panel.setVisible(true);
                splashLabel.setVisible(false);
                nodePropertyEditor.rebuildTree();
                getLogger().info("Done initializing SciView");
            });
        }

    }

    public void setStatusText(String text) {
        statusLabel.setText(text);
    }

    public void setFloor( Node n ) {
        floor = n;
    }

    public Node getFloor() {
        return floor;
    }

    private float getFloory() {
        return floor.getPosition().y();
    }

    private void setFloory( float new_pos ) {
        float temp_pos = 0f;
        temp_pos = new_pos;
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

    public void centerOnNode( Node currentNode ) {
        if( currentNode == null ) return;

        Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();

        getCamera().setTarget( currentNode.getPosition() );
        getCamera().setTargeted( true );

        // Set forward direction to point from camera at active node
        getCamera().setForward( bb.getBoundingSphere().getOrigin().minus( getCamera().getPosition() ).normalize().times( -1 ) );

        float distance = (float) (bb.getBoundingSphere().getRadius() / Math.tan( getCamera().getFov() / 360 * java.lang.Math.PI ));

        // Solve for the proper rotation
        Quaternion rotation = new Quaternion().setLookAt( getCamera().getForward().toFloatArray(),
                                                          new GLVector(0,1,0).toFloatArray(),
                                                          new GLVector(1,0,0).toFloatArray(),
                                                          new GLVector( 0,1,0).toFloatArray(),
                                                          new GLVector( 0, 0, 1).toFloatArray() );

        getCamera().setRotation( rotation.normalize() );
        getCamera().setPosition( bb.getBoundingSphere().getOrigin().plus( getCamera().getForward().times( distance * -1 ) ) );

        getCamera().setDirty(true);
        getCamera().setNeedsUpdate(true);
    }

    public void setFPSSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 30.0f ) newspeed = 30.0f;
        fpsScrollSpeed = newspeed;
        log.debug( "FPS scroll speed: " + fpsScrollSpeed );
    }

    public float getFPSSpeed() {
        return fpsScrollSpeed;
    }

    public void setMouseSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 3.0f ) newspeed = 3.0f;
        mouseSpeedMult = newspeed;
        log.debug( "Mouse speed: " + mouseSpeedMult );
    }

    public float getMouseSpeed() {
        return mouseSpeedMult;
    }

    public void resetFPSInputs() {
        getInputHandler().addBehaviour( "move_forward_scroll",
                                        new MovementCommand( "move_forward", "forward", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        getInputHandler().addBehaviour( "move_forward",
                                        new MovementCommand( "move_forward", "forward", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        getInputHandler().addBehaviour( "move_back",
                                        new MovementCommand( "move_back", "back", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        getInputHandler().addBehaviour( "move_left",
                                        new MovementCommand( "move_left", "left", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        getInputHandler().addBehaviour( "move_right",
                                        new MovementCommand( "move_right", "right", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        getInputHandler().addBehaviour( "move_up",
                                        new MovementCommand( "move_up", "up", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
        getInputHandler().addBehaviour( "move_down",
                                        new MovementCommand( "move_down", "down", () -> getScene().findObserver(),
                                                             getFPSSpeed() ) );
    }

    class enableIncrease implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            setFPSSpeed( getFPSSpeed() + 0.5f );
            setMouseSpeed( getMouseSpeed() + 0.05f );

            log.debug( "Increasing FPS scroll Speed" );

            resetFPSInputs();
        }
    }

    class enableDecrease implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            setFPSSpeed( getFPSSpeed() - 0.1f );
            setMouseSpeed( getMouseSpeed() - 0.05f );

            log.debug( "Decreasing FPS scroll Speed" );

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

    @Override public void inputSetup() {
        Function1<? super List<Scene.RaycastResult>, Unit> selectAction = nearest -> {
            if( !nearest.isEmpty() ) {
                setActiveNode( nearest.get( 0 ).getNode() );
                log.debug( "Selected node: " + getActiveNode().getName() );
            }
            return Unit.INSTANCE;
        };

        List<Class<? extends Object>> ignoredObjects = new ArrayList<>();
        ignoredObjects.add( BoundingGrid.class );

        getInputHandler().useDefaultBindings( "" );

        // Mouse controls
        getInputHandler().addBehaviour( "object_selection_mode",
                                        new SelectCommand( "objectSelector", getRenderer(), getScene(),
                                                           () -> getScene().findObserver(), false, ignoredObjects,
                                                           selectAction ) );
        getInputHandler().addKeyBinding( "object_selection_mode", "double-click button1" );

        enableArcBallControl();
        enableFPSControl();

        getInputHandler().addBehaviour( "mouse_control_nodetranslate", new NodeTranslateControl( this, 0.002f ) );
        getInputHandler().addKeyBinding( "mouse_control_nodetranslate", "shift button2" );

        // Extra keyboard controls
        getInputHandler().addBehaviour( "show_help", new showHelpDisplay() );
        getInputHandler().addKeyBinding( "show_help", "U" );

        getInputHandler().addBehaviour( "enable_decrease", new enableDecrease() );
        getInputHandler().addKeyBinding( "enable_decrease", "M" );

        getInputHandler().addBehaviour( "enable_increase", new enableIncrease() );
        getInputHandler().addKeyBinding( "enable_increase", "N" );
    }

    private void enableArcBallControl() {
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

        getInputHandler().addBehaviour( "mouse_control_arcball", targetArcball );
        getInputHandler().addKeyBinding( "mouse_control_arcball", "shift button1" );
        getInputHandler().addBehaviour( "scroll_arcball", targetArcball );
        getInputHandler().addKeyBinding( "scroll_arcball", "shift scroll" );
    }

    private void enableFPSControl() {
        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        fpsControl = new FPSCameraControl( "mouse_control", cameraSupplier, getRenderer().getWindow().getWidth(),
                                           getRenderer().getWindow().getHeight() );

        getInputHandler().addBehaviour( "mouse_control", fpsControl );
        getInputHandler().addKeyBinding( "mouse_control", "button1" );

        getInputHandler().addBehaviour( "mouse_control_cameratranslate", new CameraTranslateControl( this, 0.002f ) );
        getInputHandler().addKeyBinding( "mouse_control_cameratranslate", "button2" );

        resetFPSInputs();

        getInputHandler().addKeyBinding( "move_forward_scroll", "scroll" );
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
        boxmaterial.setDiffuse( vector( color ) );
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
        material.setDiffuse( vector( color ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        final Sphere sphere = new Sphere( radius, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( ClearGLVector3.convert( position ) );

        return addNode( sphere );
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
        material.setDiffuse( vector( color ) );
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

        return addNode( light );
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
        final FloatBuffer vBuffer = ByteBuffer.allocateDirect( flatVerts.length * 4 ) //
                .order( ByteOrder.nativeOrder() ).asFloatBuffer();
        final FloatBuffer nBuffer = ByteBuffer.allocateDirect( 0 ) //
                .order( ByteOrder.nativeOrder() ).asFloatBuffer();

        vBuffer.put( flatVerts );
        vBuffer.flip();

        pointCloud.setVertices( vBuffer );
        pointCloud.setNormals( nBuffer );
        pointCloud.setIndices( ByteBuffer.allocateDirect( 0 ) //
                                       .order( ByteOrder.nativeOrder() ).asIntBuffer() );
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
        getScene().addChild( n );
        setActiveNode( n );
        updateFloorPosition();
        eventService.publish( new NodeAddedEvent( n ) );
        return n;
    }

    public Node addMesh( final Mesh scMesh ) {
        final Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new GLVector( 0.0f, 1.0f, 0.0f ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );

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
        targetArcball.setTarget( n == null ? () -> new GLVector( 0, 0, 0 ) : n::getPosition);
        eventService.publish( new NodeActivatedEvent( activeNode ) );
        if(n instanceof BDVVolume) {
            timepointSlider.setOpaque(false);
            timepointSlider.setMinimum(0);
            timepointSlider.setMaximum(((BDVVolume) n).getMaxTimepoint());
            timepointSlider.setVisible(true);
            final int spacing = (int)Math.ceil(((BDVVolume) n).getMaxTimepoint()/10);
            timepointSlider.setMajorTickSpacing(spacing);
            timepointSlider.setMinorTickSpacing(spacing/4);
            timepointSlider.setPaintTicks(true);
            timepointSlider.setLabelTable(timepointSlider.createStandardLabels(spacing));
            timepointSlider.setValue(((BDVVolume) n).getCurrentTimepoint());

            timepointSlider.addChangeListener(e -> ((BDVVolume) n).goToTimePoint(timepointSlider.getValue()));
        } else {
            for (ChangeListener changeListener : timepointSlider.getChangeListeners()) {
                timepointSlider.removeChangeListener(changeListener);
            }
            timepointSlider.setVisible(false);
        }
        return activeNode;
    }

    public synchronized void animate( int fps, Runnable action ) {
        // TODO: Make animation speed less laggy and more accurate.
        final int delay = 1000 / fps;
        animations.add( threadService.run( () -> {
            while( animating ) {
                action.run();
                try {
                    Thread.sleep( delay );
                } catch( InterruptedException e ) {
                    break;
                }
            }
        } ) );
        animating = true;
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
        return getSceneNodes( n -> !( n instanceof Camera ) && !( n instanceof PointLight ) );
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
        node.getParent().removeChild( node );
        eventService.publish( new NodeRemovedEvent( node ) );
        if( activeNode == node ) setActiveNode( null );
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
        final VolumeViewerOptions opts = new VolumeViewerOptions();
        opts.maxCacheSizeInMB(Integer.parseInt(System.getProperty("scenery.BDVVolume.maxCacheSize", "512")));
        final BDVVolume v = new BDVVolume(source, opts);
        v.setScale(new GLVector(0.01f, 0.01f, 0.01f));

        getScene().addChild(v);
        setActiveNode(v);
        v.goToTimePoint(0);

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

        if(n instanceof Volume) {
            ((Volume) n).getColormaps().put("sciviewColormap", new Volume.Colormap.ColormapBuffer(new GenericTexture("colorTable",
                    new GLVector(colorTable.getLength(),
                            copies, 1.0f), 4,
                    GLTypeEnum.UnsignedByte,
                    byteBuffer)));
            ((Volume) n).setColormap("sciviewColormap");
        }
    }

    public <T extends RealType<T>> Node addVolume( IterableInterval<T> image, String name,
                                                                    float... voxelDimensions ) {
        log.debug( "Add Volume" );

        long dimensions[] = new long[3];
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

        GLVector scaleVec = new GLVector( 0.5f * dimensions[0], //
                                          0.5f * dimensions[1], //
                                          0.5f * dimensions[2] );

        v.setScale( scaleVec );// TODO maybe dont do this
        // TODO: This translation should probably be accounted for in scenery; volumes use a corner-origin and
        //        meshes use center-origin coordinate systems.
        v.setPosition( v.getPosition().plus( new GLVector( 0.5f * dimensions[0] - 0.5f, 0.5f * dimensions[1] - 0.5f,
                                                           0.5f * dimensions[2] - 0.5f ) ) );

        v.setTrangemin( minVal );
        v.setTrangemax( maxVal );

        try {
            setColormap( v, lutService.loadLUT( lutService.findLUTs().get( "WCIF/ICA.lut" ) ) );
        } catch( IOException e ) {
            e.printStackTrace();
        }

        setActiveNode( v );

        return v;
    }

    public <T extends RealType<T>> Node updateVolume( IterableInterval<T> image, String name,
                                                                       float[] voxelDimensions, Volume v ) {
        log.debug( "Update Volume" );

        long dimensions[] = new long[3];
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

    private static GLVector vector( ColorRGB color ) {
        if( color instanceof ColorRGBA ) {
            return new GLVector( color.getRed() / 255f, //
                                 color.getGreen() / 255f, //
                                 color.getBlue() / 255f, //
                                 color.getAlpha() / 255f );
        }
        return new GLVector( color.getRed() / 255f, //
                             color.getGreen() / 255f, //
                             color.getBlue() / 255f );
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
            final Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();
            final Node.BoundingSphere bs = bb.getBoundingSphere();
            final float neededFloor = bb.getMin().y() - Math.max( bs.getRadius(), 1 );
            if( neededFloor < getFloory() ) setFloory( neededFloor );
        }

        floor.setPosition( new GLVector( 0f, getFloory(), 0f ) );
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
}

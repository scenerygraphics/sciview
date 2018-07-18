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
import com.sun.javafx.application.PlatformImpl;
import coremem.enums.NativeTypeEnum;
import graphics.scenery.*;
import graphics.scenery.backends.Renderer;
import graphics.scenery.controls.InputHandler;
import graphics.scenery.controls.behaviours.ArcballCameraControl;
import graphics.scenery.controls.behaviours.FPSCameraControl;
import graphics.scenery.controls.behaviours.MovementCommand;
import graphics.scenery.controls.behaviours.SelectCommand;
import graphics.scenery.controls.behaviours.SelectCommand.SelectResult;
import graphics.scenery.utils.SceneryPanel;
import graphics.scenery.volumes.Volume;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.imagej.Dataset;
import net.imagej.lut.LUTService;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.RealPoint;
import net.imglib2.display.AbstractArrayColorTable;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.apache.commons.lang3.SystemUtils;
import org.scijava.Context;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.menu.MenuService;
import org.scijava.plugin.Parameter;
import org.scijava.thread.ThreadService;
import org.scijava.ui.behaviour.ClickBehaviour;
import org.scijava.util.ColorRGB;
import org.scijava.util.ColorRGBA;
import org.scijava.util.Colors;
import sc.iview.javafx.JavaFXMenuCreator;
import sc.iview.process.MeshConverter;
import sc.iview.vector.ClearGLVector3;
import sc.iview.vector.Vector3;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SciView extends SceneryBase {

    public static final ColorRGB DEFAULT_COLOR = Colors.LIGHTGRAY;

    @Parameter private LogService log;

    @Parameter private MenuService menus;

    @Parameter private IOService io;

    @Parameter private OpService ops;

    @Parameter private DisplayService displayService;

    @Parameter private LUTService lutService;

    @Parameter private ThreadService threadService;

    /**
     * Queue keeps track of the currently running animations
     **/
    private Queue<Future> animations;

    /**
     * Animation pause tracking
     **/
    private boolean animating;

    private Node activeNode = null;

    protected ArcballCameraControl targetArcball;
    protected FPSCameraControl fpsControl;

    private Boolean defaultArcBall = false;// arcball target broken

    Camera camera = null;

    private boolean initialized = false;// i know TODO

    private boolean useJavaFX = true;
    SceneryPanel imagePanel = null;

    private float fpsScrollSpeed = 3.0f;

    private float mouseSpeedMult = 0.25f;

    private float flooryaxis = -1.0f;

    private Display<?> scijavaDisplay;

    protected Node floor;

    public SciView( Context context ) {
        super( "SciView", 800, 600, false, context );
        context.inject( this );
    }

    public SciView( String applicationName, int windowWidth, int windowHeight ) {
        super( applicationName, windowWidth, windowHeight, false );
    }

    public InputHandler publicGetInputHandler() {
        return getInputHandler();
    }

    @SuppressWarnings("restriction") @Override public void init() {

        // TODO: there is a Linux issue with the Vulkan renderer and X that leads to a known "RenderBadPicture" error
        if( SystemUtils.IS_OS_LINUX && !System.getProperties().containsKey( "scenery.Renderer" ) ) {
            System.setProperty( "scenery.Renderer", "OpenGLRenderer" );
        }

        if( useJavaFX ) {
            CountDownLatch latch = new CountDownLatch( 1 );
            final SceneryPanel[] sceneryPanel = { null };

            PlatformImpl.startup( () -> {
            } );

            Platform.runLater( () -> {

                Stage stage = new Stage();
                stage.setTitle( "SciView" );

                StackPane stackPane = new StackPane();
                stackPane.setBackground(
                        new Background( new BackgroundFill( Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY ) ) );

                GridPane pane = new GridPane();
                Label label = new Label( "SciView - press U for usage help" );

                sceneryPanel[0] = new SceneryPanel( getWindowWidth(), getWindowHeight() );

                GridPane.setHgrow( sceneryPanel[0], Priority.ALWAYS );
                GridPane.setVgrow( sceneryPanel[0], Priority.ALWAYS );

                GridPane.setFillHeight( sceneryPanel[0], true );
                GridPane.setFillWidth( sceneryPanel[0], true );

                GridPane.setHgrow( label, Priority.ALWAYS );
                GridPane.setHalignment( label, HPos.CENTER );
                GridPane.setValignment( label, VPos.BOTTOM );

                label.maxWidthProperty().bind( pane.widthProperty() );

                pane.setStyle( "-fx-background-color: rgb(50,48,47);" +
                               "-fx-font-family: Helvetica Neue, Helvetica, Segoe, Proxima Nova, Arial, sans-serif;" +
                               "-fx-font-weight: 400;" + "-fx-font-size: 1.2em;" + "-fx-text-fill: white;" +
                               "-fx-text-alignment: center;" );

                label.setStyle( "-fx-padding: 0.2em;" + "-fx-text-fill: white;" );

                label.setTextAlignment( TextAlignment.CENTER );

                MenuBar menuBar = new MenuBar();
                pane.add( menuBar, 1, 1 );
                pane.add( sceneryPanel[0], 1, 2 );
                pane.add( label, 1, 3 );
                stackPane.getChildren().addAll( pane );

                javafx.scene.Scene scene = new javafx.scene.Scene( stackPane );
                stage.setScene( scene );
                stage.setOnCloseRequest( event -> {
                    getDisplay().close();
                    this.close();
                } );
                stage.focusedProperty().addListener( ( ov, t, t1 ) -> {
                    if( t1 )// If you just gained focus
                        displayService.setActiveDisplay( getDisplay() );
                } );

                new JavaFXMenuCreator().createMenus( menus.getMenu( "SciView" ), menuBar );

                stage.show();

                latch.countDown();
            } );

            try {
                latch.await();
            } catch( InterruptedException e1 ) {
                e1.printStackTrace();
            }

            setRenderer( Renderer.createRenderer( getHub(), getApplicationName(), getScene(), getWindowWidth(),
                                                  getWindowHeight(), sceneryPanel[0] ) );
        } else {
            setRenderer( Renderer.createRenderer( getHub(), getApplicationName(), getScene(), 512, 512 ) );
        }

        // Enable push rendering by default
        getRenderer().setPushMode( true );

        getHub().add( SceneryElement.Renderer, getRenderer() );

        PointLight[] lights = new PointLight[2];

        for( int i = 0; i < lights.length; i++ ) {
            lights[i] = new PointLight( 150.0f );
            lights[i].setPosition( new GLVector( 20.0f * i - 20.0f, 20.0f * i - 20.0f, 20.0f * i - 20.0f ) );
            lights[i].setEmissionColor( new GLVector( 1.0f, 1.0f, 1.0f ) );
            lights[i].setIntensity( 5000.2f * ( i + 1 ) );
            getScene().addChild( lights[i] );
        }

        Camera cam = new DetachedHeadCamera();
        cam.setPosition( new GLVector( 0.0f, 5.0f, 5.0f ) );
        cam.perspectiveCamera( 50.0f, getWindowWidth(), getWindowHeight(), 0.001f, 750.0f );
        //cam.setTarget( new GLVector( 0, 0, 0 ) );
        //cam.setTargeted( true );
        cam.setActive( true );
        getScene().addChild( cam );
        this.camera = cam;

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setPosition( new GLVector( 0f, -1f, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

        animations = new LinkedList<>();

        // Try to surround the scene with a box
//        Box shell = new Box( new GLVector( 100.0f, 100.0f, 100.0f ), true );
//        shell.getMaterial().setDiffuse( new GLVector( 0.2f, 0.2f, 0.2f ) );
//        shell.getMaterial().setSpecular( GLVector.getNullVector( 3 ) );
//        shell.getMaterial().setAmbient( GLVector.getNullVector( 3 ) );
//        //shell.getMaterial().setDoubleSided( true );
//        shell.getMaterial().setCullingMode( Material.CullingMode.Front );
//        // Could we generate a grid pattern with proper scale/units as a texture right now?
//        shell.setPosition( new GLVector(0,0,0) );
//        getCamera().addChild( shell );

        //initialized = true; // inputSetup is called second, so that needs to toggle initialized
    }

    public void setFloor( Node n ) {
        floor = n;
    }

    public Node getFloor() {
        return floor;
    }

    private float getFloory() {
        return flooryaxis;
    }

    private void setFloory( float new_pos ) {
        float temp_pos = 0f;
        temp_pos = new_pos;
        if( temp_pos < -100f ) temp_pos = -100f;
        else if( new_pos > 5f ) temp_pos = 5f;
        flooryaxis = temp_pos;
        String helpString = "SciView help:\n\n";
        helpString += flooryaxis + "\n";
        log.warn( helpString );
    }

    public boolean isInitialized() {
        return initialized;
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

    class toggleCameraControl implements ClickBehaviour {
        String currentMode = "arcball";

        @Override public void click( int x, int y ) {
            if( currentMode.startsWith( "fps" ) ) {
                enableArcBallControl();

                currentMode = "arcball";
            } else {
                enableFPSControl();

                currentMode = "fps";
            }

            log.info( "Switched to " + currentMode + " control" );
        }
    }

    public void setFPSSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 30.0f ) newspeed = 30.0f;
        fpsScrollSpeed = newspeed;
        log.warn( "FPS scroll speed: " + fpsScrollSpeed );
    }

    public float getFPSSpeed() {
        return fpsScrollSpeed;
    }

    public void setMouseSpeed( float newspeed ) {
        if( newspeed < 0.30f ) newspeed = 0.3f;
        else if( newspeed > 3.0f ) newspeed = 3.0f;
        mouseSpeedMult = newspeed;
        log.warn( "Mouse speed: " + mouseSpeedMult );
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

            log.warn( "Increasing FPS scroll Speed" );

            resetFPSInputs();
        }
    }

    class enableDecrease implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            setFPSSpeed( getFPSSpeed() - 0.1f );
            setMouseSpeed( getMouseSpeed() - 0.05f );

            log.warn( "Decreasing FPS scroll Speed" );

            resetFPSInputs();
        }
    }

    class showHelpDisplay implements ClickBehaviour {

        @Override public void click( int x, int y ) {
            String helpString = "SciView help:\n\n";
            // HACK: hard-coded, but no accessor for getAllBindings in scenery
            helpString += "U - this menu\n";
            helpString += "W/S - forward/backward\n";
            helpString += "A/D - left/right\n";
            helpString += "space/shift + space - up/down\n";
            helpString += "X - toggle between FPS and arcball camera control\n";
            helpString += "  FPS camera control allows free movement in all directions\n";
            helpString += "  Arcball camera control allows for mouse-only rotation about an object's center\n";
            helpString += "Q - debug view\n";
            helpString += "P - take a screenshot\n";
            helpString += "shift + P - record a video\n";
            helpString += "shift + V - toggle virtual reality mode\n";
            helpString += "F - toggle fullscreen\n";
            helpString += "K - increase exposure\n";
            helpString += "L - decrease exposure\n";
            helpString += "shift + K - increase gamma\n";
            helpString += "shift + L - decrease gamma\n";
            helpString += "N - Increase Speed of FPS in FPS mode and mouse movement in arcball mode\n";
            helpString += "M - Decrease Speed of FPS in FPS mode and mouse movement in arcball mode\n";
            // HACK: Make the console pop via stderr.
            // Later, we will use a nicer dialog box or some such.
            log.warn( helpString );
        }
    }

    @Override public void inputSetup() {
        //setInputHandler((ClearGLInputHandler) viewer.getHub().get(SceneryElement.INPUT));

        Function1<? super List<SelectResult>, Unit> selectAction = nearest -> {
            log.warn( "Select action triggered" );
            if( !nearest.isEmpty() ) {
                setActiveNode( nearest.get( 0 ).getNode() );
                log.warn( "Selected node: " + getActiveNode() );
            }
            return Unit.INSTANCE;
        };

        List<Class<? extends Object>> ignoredObjects = new ArrayList<>();
        ignoredObjects.add( BoundingGrid.class );

        getInputHandler().useDefaultBindings( "" );
        getInputHandler().addBehaviour( "object_selection_mode",
                                        new SelectCommand( "objectSelector", getRenderer(), getScene(),
                                                           () -> getScene().findObserver(), false, ignoredObjects,
                                                           selectAction ) );
        getInputHandler().addKeyBinding( "object_selection_mode", "double-click button1" );

        //enableArcBallControl();
        enableFPSControl();

        getInputHandler().addBehaviour( "toggle_control_mode", new toggleCameraControl() );
        getInputHandler().addKeyBinding( "toggle_control_mode", "X" );

        //setupCameraModeSwitching( "X" );

        getInputHandler().addBehaviour( "show_help", new showHelpDisplay() );
        getInputHandler().addKeyBinding( "show_help", "U" );

        getInputHandler().addBehaviour( "enable_decrease", new enableDecrease() );
        getInputHandler().addKeyBinding( "enable_decrease", "M" );

        getInputHandler().addBehaviour( "enable_increase", new enableIncrease() );
        getInputHandler().addKeyBinding( "enable_increase", "N" );

        initialized = true;
    }

    public void enableArcBallControl() {
        GLVector target;
        if( getActiveNode() == null ) {
            target = new GLVector( 0, 0, 0 );
        } else {
            target = getActiveNode().getPosition();
        }

        float mouseSpeed = 0.25f;
        mouseSpeed = getMouseSpeed();

        String helpString = "SciView help:\n\n";
        // HACK: hard-coded, but no accessor for getAllBindings in scenery
        helpString += mouseSpeed + "\n";
        log.warn( helpString );

        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        targetArcball = new ArcballCameraControl( "mouse_control", cameraSupplier, getRenderer().getWindow().getWidth(),
                                                  getRenderer().getWindow().getHeight(), target );
        targetArcball.setMaximumDistance( Float.MAX_VALUE );
        targetArcball.setMouseSpeedMultiplier( mouseSpeed );
        targetArcball.setScrollSpeedMultiplier( 0.05f );
        targetArcball.setDistance( getCamera().getPosition().minus( target ).magnitude() );

        getInputHandler().addBehaviour( "mouse_control", targetArcball );
        getInputHandler().addBehaviour( "scroll_arcball", targetArcball );
        getInputHandler().addKeyBinding( "scroll_arcball", "scroll" );

        getInputHandler().removeBehaviour( "move_forward" );
        getInputHandler().removeBehaviour( "move_back" );
        getInputHandler().removeBehaviour( "move_left" );
        getInputHandler().removeBehaviour( "move_right" );
        getInputHandler().removeBehaviour( "move_up" );
        getInputHandler().removeBehaviour( "move_down" );
    }

    public void enableFPSControl() {
        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        fpsControl = new FPSCameraControl( "mouse_control", cameraSupplier, getRenderer().getWindow().getWidth(),
                                           getRenderer().getWindow().getHeight() );

        getInputHandler().addBehaviour( "mouse_control", fpsControl );
        getInputHandler().removeKeyBinding( "scroll_arcball" );
        getInputHandler().removeBehaviour( "scroll_arcball" );

        float defaultSpeed = 3.0f;
        defaultSpeed = getFPSSpeed();
        String helpString = "SciView help:\n\n";
        // HACK: hard-coded, but no accessor for getAllBindings in scenery
        helpString += defaultSpeed + "\n";
        log.warn( helpString );

        resetFPSInputs();

//        getInputHandler().addKeyBinding( "move_up", "C" );
//        getInputHandler().addKeyBinding( "move_down", "Z" );
        getInputHandler().addKeyBinding( "move_forward_scroll", "scroll" );
    }

    private Object selectNode( List<SelectResult> result ) {
        if( !result.isEmpty() ) {
            Collections.sort( result, new Comparator<SelectResult>() {
                @Override public int compare( SelectResult lhs, SelectResult rhs ) {
                    return lhs.getDistance() > rhs.getDistance() ? -1 : lhs.getDistance() < rhs.getDistance() ? 1 : 0;
                }
            } );
            activeNode = result.get( 0 ).getNode();
            //log.warn( "Selected " + activeNode );
            return activeNode;
        }
        return null;
    }

    public graphics.scenery.Node addBox() {
        return addBox( new ClearGLVector3( 0.0f, 0.0f, 0.0f ) );
    }

    public graphics.scenery.Node addBox( Vector3 position ) {
        return addBox( position, new ClearGLVector3( 1.0f, 1.0f, 1.0f ) );
    }

    public graphics.scenery.Node addBox( Vector3 position, Vector3 size ) {
        return addBox( position, size, DEFAULT_COLOR, false );
    }

    public graphics.scenery.Node addBox( Vector3 position, Vector3 size, ColorRGB color, boolean inside ) {
        // TODO: use a material from the current pallate by default
        Material boxmaterial = new Material();
        boxmaterial.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        boxmaterial.setDiffuse( vector( color ) );
        boxmaterial.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        //boxmaterial.setDoubleSided( true );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Box box = new Box( ClearGLVector3.convert( size ), inside );
        box.setMaterial( boxmaterial );
        box.setPosition( ClearGLVector3.convert( position ) );

        //System.err.println( "Num elements in scene: " + viewer.getSceneNodes().size() );

        activeNode = box;

        getScene().addChild( box );

        if( defaultArcBall ) enableArcBallControl();

        // Node currentNode = getActiveNode();

        float temp = 0.0f;
        getFloory();
        temp = position.yf();
        if( getFloory() < temp ) {
            System.err.println( "Not lowered " );
        } else {
            setFloory( temp - 1f );
        }
        getFloor().setVisible( !getFloor().getVisible() );

        float yposition = -1.0f;

        yposition = getFloory();

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setPosition( new GLVector( 0f, yposition, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

        return box;

        //System.err.println( "Num elements in scene: " + viewer.getSceneNodes().size() );
    }

    public graphics.scenery.Node addSphere() {
        return addSphere( new ClearGLVector3( 0.0f, 0.0f, 0.0f ), 1 );
    }

    public graphics.scenery.Node addSphere( Vector3 position, float radius ) {
        return addSphere( position, radius, DEFAULT_COLOR );
    }

    public graphics.scenery.Node addSphere( Vector3 position, float radius, ColorRGB color ) {
        Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( vector( color ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Sphere sphere = new Sphere( radius, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( ClearGLVector3.convert( position ) );

        activeNode = sphere;

        getScene().addChild( sphere );

        if( defaultArcBall ) enableArcBallControl();

        Node currentNode = getActiveNode();

        float temp = 0.0f;
        float rad = 0.0f;
        Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();
        Node.BoundingSphere bs = currentNode.generateBoundingBox().getBoundingSphere();
        getFloory();
        temp = bb.getMin().y();
        rad = bs.getRadius();
        if( getFloory() < temp ) {
            System.err.println( "Not lowered " );
        } else {
            setFloory( temp - rad );
        }
        getFloor().setVisible( !getFloor().getVisible() );

        float yposition = -1.0f;

        yposition = getFloory();

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setPosition( new GLVector( 0f, yposition, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

        return sphere;
    }

    public graphics.scenery.Node addLine() {
        return addLine( new ClearGLVector3( 0.0f, 0.0f, 0.0f ), new ClearGLVector3( 0.0f, 0.0f, 0.0f ) );
    }

    public graphics.scenery.Node addLine( Vector3 start, Vector3 stop ) {
        return addLine( start, stop, DEFAULT_COLOR );
    }

    public graphics.scenery.Node addLine( Vector3 start, Vector3 stop, ColorRGB color ) {

        Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        material.setDiffuse( vector( color ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        final Line line = new Line( 2 );

        line.addPoint( ClearGLVector3.convert( start ) );
        line.addPoint( ClearGLVector3.convert( stop ) );

        line.setEdgeWidth( 0.1f );

        line.setMaterial( material );
        line.setPosition( ClearGLVector3.convert( start ) );

        activeNode = line;

        getScene().addChild( line );

        if( defaultArcBall ) enableArcBallControl();

        Node currentNode = getActiveNode();

        float temp = 0.0f;
        Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();
        getFloory();
        temp = bb.getMin().y();
        if( getFloory() < temp ) {
            System.err.println( "Not lowered " );
        } else {
            setFloory( temp - 1f );
        }
        getFloor().setVisible( !getFloor().getVisible() );

        float yposition = -1.0f;

        yposition = getFloory();

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setPosition( new GLVector( 0f, yposition, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

        return line;
    }

    public graphics.scenery.Node addLine( Vector3[] points, ColorRGB color, double edgeWidth ) {
        Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        material.setDiffuse( vector( color ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );

        final Line line = new Line( points.length );
        for( Vector3 pt : points ) {
            line.addPoint( ClearGLVector3.convert( pt ) );
        }

        line.setEdgeWidth( ( float ) edgeWidth );

        line.setMaterial( material );
        line.setPosition( ClearGLVector3.convert( points[0] ) );

        activeNode = line;

        getScene().addChild( line );

        if( defaultArcBall ) enableArcBallControl();

        Node currentNode = getActiveNode();

        float temp = 0.0f;
        Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();
        getFloory();
        temp = bb.getMin().y();
        if( getFloory() < temp ) {
            System.err.println( "Not lowered " );
        } else {
            setFloory( temp - 1f );
        }
        getFloor().setVisible( !getFloor().getVisible() );

        float yposition = -1.0f;

        yposition = getFloory();

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setPosition( new GLVector( 0f, yposition, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

        return line;
    }

    public graphics.scenery.Node addPointLight() {
        Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new GLVector( 0.0f, 1.0f, 0.0f ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final PointLight light = new PointLight( 5.0f );
        light.setMaterial( material );
        light.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );

        getScene().addChild( light );
        return light;
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

    public graphics.scenery.Node addPointCloud( Collection<? extends RealLocalizable> points ) {
        return addPointCloud( points, "PointCloud" );
    }

    public graphics.scenery.Node addPointCloud( Collection<? extends RealLocalizable> points, String name ) {
        float[] flatVerts = new float[points.size() * 3];
        int k = 0;
        for( RealLocalizable point : points ) {
            flatVerts[k * 3] = point.getFloatPosition( 0 );
            flatVerts[k * 3 + 1] = point.getFloatPosition( 1 );
            flatVerts[k * 3 + 2] = point.getFloatPosition( 2 );
            k++;
        }

        PointCloud pointCloud = new PointCloud( getDefaultPointSize(), name );
        Material material = new Material();
        FloatBuffer vBuffer = ByteBuffer.allocateDirect( flatVerts.length * 4 ).order(
                ByteOrder.nativeOrder() ).asFloatBuffer();
        FloatBuffer nBuffer = ByteBuffer.allocateDirect( 0 ).order( ByteOrder.nativeOrder() ).asFloatBuffer();

        vBuffer.put( flatVerts );
        vBuffer.flip();

        pointCloud.setVertices( vBuffer );
        pointCloud.setNormals( nBuffer );
        pointCloud.setIndices( ByteBuffer.allocateDirect( 0 ).order( ByteOrder.nativeOrder() ).asIntBuffer() );
        pointCloud.setupPointCloud();
        material.setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        material.setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.setMaterial( material );
        pointCloud.setPosition( new GLVector( 0f, 0f, 0f ) );
        getScene().addChild( pointCloud );

        return pointCloud;
    }

    public graphics.scenery.Node addPointCloud( PointCloud pointCloud ) {

        pointCloud.setupPointCloud();
        pointCloud.getMaterial().setAmbient( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.getMaterial().setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        pointCloud.setPosition( new GLVector( 0f, 0f, 0f ) );
        getScene().addChild( pointCloud );

        return pointCloud;
    }

    public graphics.scenery.Node addNode( Node n ) {
        getScene().addChild( n );

        Node currentNode = getActiveNode();

        float temp = 0.0f;
        Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();
        getFloory();
        temp = bb.getMin().y();
        if( getFloory() < temp ) {
            System.err.println( "Not lowered " );
        } else {
            setFloory( temp - 1f );
        }
        getFloor().setVisible( !getFloor().getVisible() );

        float yposition = -1.0f;

        yposition = getFloory();

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setPosition( new GLVector( 0f, yposition, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

        return n;
    }

    public graphics.scenery.Node addMesh( Mesh scMesh ) {
        Material material = new Material();
        material.setAmbient( new GLVector( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new GLVector( 0.0f, 1.0f, 0.0f ) );
        material.setSpecular( new GLVector( 1.0f, 1.0f, 1.0f ) );
        //material.setDoubleSided( false );

        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector( 0.0f, 0.0f, 0.0f ) );

        setActiveNode( scMesh );

        getScene().addChild( scMesh );

        if( defaultArcBall ) enableArcBallControl();

        Node currentNode = getActiveNode();

        float temp = 0.0f;
        Node.OrientedBoundingBox bb = currentNode.generateBoundingBox();
        getFloory();
        temp = bb.getMin().y();
        if( getFloory() < temp ) {
            System.err.println( "Not lowered " );
        } else {
            setFloory( temp - 1f );
        }
        getFloor().setVisible( !getFloor().getVisible() );

        float yposition = -1.0f;

        yposition = getFloory();

        floor = new Box( new GLVector( 500f, 0.2f, 500f ) );
        floor.setPosition( new GLVector( 0f, yposition, 0f ) );
        floor.getMaterial().setDiffuse( new GLVector( 1.0f, 1.0f, 1.0f ) );
        getScene().addChild( floor );

        return scMesh;
    }

    public graphics.scenery.Node addMesh( net.imagej.mesh.Mesh mesh ) {
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
        activeNode = n;
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
        getRenderer().screenshot( path );
    }

    public Node[] getSceneNodes() {
        return getSceneNodes( n -> !( n instanceof Camera ) && !( n instanceof PointLight ) );
    }

    public Node[] getSceneNodes( Predicate<? super Node> filter ) {
        return getScene().getChildren().stream().filter( filter ).toArray( Node[]::new );
    }

    public void deleteSelectedMesh() {
        getScene().removeChild( getActiveNode() );
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

    public graphics.scenery.Node addVolume( Dataset image ) {
        float[] voxelDims = new float[image.numDimensions()];
        for( int d = 0; d < voxelDims.length; d++ ) {
            voxelDims[d] = ( float ) image.axis( d ).averageScale( 0, 1 );
        }
        return addVolume( image, voxelDims );
    }

    @SuppressWarnings({ "rawtypes", "unchecked" }) public graphics.scenery.Node addVolume( Dataset image,
                                                                                           float[] voxelDimensions ) {
        return addVolume( ( IterableInterval ) Views.flatIterable( image.getImgPlus() ), image.getName(),
                          voxelDimensions );
    }

    public <T extends RealType<T>> graphics.scenery.Node addVolume( IterableInterval<T> image ) {
        return addVolume( image, "Volume" );
    }

    public <T extends RealType<T>> graphics.scenery.Node addVolume( IterableInterval<T> image, String name ) {
        return addVolume( image, name, 1, 1, 1 );
    }

    public void setColormap( Node n, AbstractArrayColorTable colorTable ) {
        n.getMaterial().getTextures().put( "normal", "fromBuffer:diffuse" );
        n.getMaterial().setNeedsTextureReload( true );

        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                ( int ) ( 4 * 4 * colorTable.getLength() ) );// Num bytes * num components * color map length
        for( int k = 0; k < colorTable.getLength(); k++ ) {
            for( int c = 0; c < colorTable.getComponentCount(); c++ ) {
                byteBuffer.put( ( byte ) colorTable.get( c, k ) );// TODO this assumes numBits is 8, could by 16
            }
            if( colorTable.getComponentCount() == 3 ) byteBuffer.put( ( byte ) 255 );
        }
        byteBuffer.flip();

        n.getMaterial().getTransferTextures().put( "diffuse", new GenericTexture( "colorTable",
                                                                                  new GLVector( colorTable.getLength(),
                                                                                                1.0f, 1.0f ), 4,
                                                                                  GLTypeEnum.UnsignedByte,
                                                                                  byteBuffer ) );
        n.getMaterial().getTextures().put( "diffuse", "fromBuffer:diffuse" );
        n.getMaterial().setNeedsTextureReload( true );

    }

    public <T extends RealType<T>> graphics.scenery.Node addVolume( IterableInterval<T> image, String name,
                                                                    float... voxelDimensions ) {
        log.warn( "Add Volume" );

        long dimensions[] = new long[3];
        image.dimensions( dimensions );

        Volume v = new Volume();

        getScene().addChild( v );

        @SuppressWarnings("unchecked") Class<T> voxelType = ( Class<T> ) image.firstElement().getClass();
        int bytesPerVoxel = image.firstElement().getBitsPerPixel() / 8;
        float minVal = Float.MIN_VALUE, maxVal = Float.MAX_VALUE;
        NativeTypeEnum nType = null;

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

        GLVector scaleVec = new GLVector( 0.5f * ( float ) dimensions[0], 0.5f * ( float ) dimensions[1],
                                          0.5f * ( float ) dimensions[2] );

        v.setScale( scaleVec );// TODO maybe dont do this
        // TODO: This translation should probably be accounted for in scenery; volumes use a corner-origin and
        //        meshes use center-origin coordinate systems.
        v.setPosition( v.getPosition().plus( new GLVector( 0.5f * dimensions[0] - 0.5f, 0.5f * dimensions[1] - 0.5f,
                                                           0.5f * dimensions[2] - 0.5f ) ) );

        v.setTrangemin( minVal );
        v.setTrangemax( maxVal );

        try {
            setColormap( v, ( AbstractArrayColorTable ) lutService.loadLUT(
                    lutService.findLUTs().get( "WCIF/ICA.lut" ) ) );
        } catch( IOException e ) {
            e.printStackTrace();
        }

        setActiveNode( v );

        return v;
    }

    public <T extends RealType<T>> graphics.scenery.Node updateVolume( IterableInterval<T> image, String name,
                                                                       float[] voxelDimensions, Volume v ) {
        //log.warn( "Add Volume" );

        long dimensions[] = new long[3];
        image.dimensions( dimensions );

        @SuppressWarnings("unchecked") Class<T> voxelType = ( Class<T> ) image.firstElement().getClass();
        int bytesPerVoxel = image.firstElement().getBitsPerPixel() / 8;
        float minVal = Float.MIN_VALUE, maxVal = Float.MAX_VALUE;
        NativeTypeEnum nType = null;

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

    protected void finalize() {
        stopAnimation();
    }
}

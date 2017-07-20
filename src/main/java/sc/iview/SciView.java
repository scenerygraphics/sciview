package sc.iview;

import cleargl.GLVector;
import com.sun.javafx.application.PlatformImpl;
import coremem.enums.NativeTypeEnum;
import graphics.scenery.*;
import graphics.scenery.backends.Renderer;
import graphics.scenery.controls.behaviours.ArcballCameraControl;
import graphics.scenery.controls.behaviours.FPSCameraControl;
import graphics.scenery.controls.behaviours.SelectCommand;
import graphics.scenery.volumes.Volume;
import graphics.scenery.utils.SceneryPanel;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import kotlin.Unit;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import sc.iview.process.MeshConverter;

import org.scijava.ui.behaviour.ClickBehaviour;
import org.lwjgl.system.MemoryUtil;
import graphics.scenery.controls.behaviours.SelectCommand.SelectResult;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import static java.awt.Color.black;
import static java.awt.Color.white;
import static java.awt.SystemColor.text;
import static javafx.scene.paint.Color.rgb;

public class SciView extends SceneryDefaultApplication {

    private Thread animationThread;
    private Node activeNode = null;

    private Boolean defaultArcBall = true;

    Camera camera = null;
    
    private boolean initialized = false;// i know TODO

    private boolean useJavaFX = false;
    SceneryPanel imagePanel = null;

    public SciView() {
        super("SciView", 800, 600, false);
    }

    public SciView(String applicationName, int windowWidth, int windowHeight) {
        super(applicationName, windowWidth, windowHeight, false);
    }

    @Override
    public void init() {
        if( useJavaFX ) {
//            CountDownLatch latch = new CountDownLatch(1);
//
//
//            PlatformImpl.startup( new Runnable() { public void run() {} } );
//
//            Platform.runLater( new Runnable() {
//                public void run() {
//                    Stage stage = new Stage();
//                    stage.setTitle( getApplicationName() );
//
//                    StackPane stackPane = new StackPane();
//                    stackPane.backgroundProperty().set(new Background(new BackgroundFill(Color.TRANSPARENT, CornerRadii.EMPTY, Insets.EMPTY)));
//
//                    GridPane pane = new GridPane();
//                    Label label = new Label(getApplicationName());
//
//                    imagePanel = new SceneryPanel(getWindowWidth(), getWindowHeight());
//
//                    GridPane.setHgrow(imagePanel, Priority.ALWAYS);
//                    GridPane.setVgrow(imagePanel, Priority.ALWAYS);
//
//                    GridPane.setFillHeight(imagePanel, true);
//                    GridPane.setFillWidth(imagePanel, true);
//
//                    GridPane.setHgrow(label, Priority.ALWAYS);
//                    GridPane.setHalignment(label, HPos.CENTER);
//                    GridPane.setValignment(label, VPos.BOTTOM);
//
//                    // Use as status bar
//                    label.maxWidthProperty().bind(pane.widthProperty());
//                    label.setTextAlignment( TextAlignment.CENTER );
//
//                    pane.add(imagePanel, 1, 1);
//                    pane.add(label, 1, 2);
//                    stackPane.getChildren().addAll(pane);
//
//
//
//                    javafx.scene.Scene scene = new javafx.scene.Scene(stackPane);
//                    stage.setScene( scene );
//                    stage.setOnCloseRequest(
//                            new javafx.event.EventHandler() {
//                                @Override
//                                public void handle(Event event) {
//                                    getRenderer().setShouldClose(true);
//
//                                    Platform.runLater(new Runnable() {
//                                        public void run() {
//                                            Platform.exit();
//                                        }
//                                    });
//                                }
//                            });
//
//                    stage.show();
//
//
//                    imagePanel.resize(getWindowWidth(),getWindowHeight());
//
//                    latch.countDown();
//                }
//            });
//
//            try {
//                latch.await();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//            setRenderer( Renderer.Factory.createRenderer(getHub(), getApplicationName(), getScene(), 512, 512, imagePanel) );
        } else {
            setRenderer( Renderer.Factory.createRenderer( getHub(), getApplicationName(), getScene(), 512, 512));
        }

        getHub().add(SceneryElement.Renderer, getRenderer());

        PointLight[] lights = new PointLight[2];

        for( int i = 0; i < lights.length; i++ ) {
            lights[i] = new PointLight();
            lights[i].setPosition( new GLVector(20.0f * i, 20.0f * i, 20.0f * i) );
            lights[i].setEmissionColor( new GLVector(1.0f, 1.0f, 1.0f) );
            lights[i].setIntensity( 5000.2f*(i+1) );
            lights[i].setLinear(0.0f);
            lights[i].setQuadratic(0.5f);
            getScene().addChild( lights[i] );
        }

        Camera cam = new DetachedHeadCamera();
        cam.setPosition( new GLVector(0.0f, 0.0f, 5.0f) );
        cam.perspectiveCamera(50.0f, getWindowWidth(), getWindowHeight(), 0.1f, 1000.0f);
        cam.setActive( true );
        getScene().addChild(cam);
        this.camera = cam;

        Box shell = new Box(new GLVector(120.0f, 120.0f, 120.0f), true);
        shell.getMaterial().setDoubleSided( true );
        shell.getMaterial().setDiffuse( new GLVector(0.2f, 0.2f, 0.2f) );
        shell.getMaterial().setSpecular( GLVector.getNullVector(3) );
        shell.getMaterial().setAmbient( GLVector.getNullVector(3) );
        getScene().addChild(shell);


        // Heads up display
//        FontBoard board = new FontBoard();
//        board.setText("SceneryViewer");
//        board.setPosition(new GLVector(0.0f, 0.0f, 0.0f));
//        board.setUpdate(() -> { /*board.setText("Pos("+board.getParent().getPosition().get(0)+","+
//                                                    board.getParent().getPosition().get(1)+","+
//                                                    board.getParent().getPosition().get(2)+")");*/
//                                //board.setText("SceneryViewer");
//                                board.setPosition(board.getParent().getPosition().times(-1.0f));
//                                return null; });
//
//        //getScene().addChild(board);
//        cam.addChild(board);

//        setRepl(new REPL(getScene(), getRenderer()));
//        getRepl().start();
//        getRepl().showConsoleWindow();
        
        
        //initialized = true; // inputSetup is called second, so that needs to toggle initialized
    }
    
    public boolean isInitialized() {
    	return initialized;
    }

    public Camera getCamera() {
        return camera;
    }



    @Override
    public void inputSetup() {
        //setInputHandler((ClearGLInputHandler) viewer.getHub().get(SceneryElement.INPUT));

        getInputHandler().useDefaultBindings("");
        getInputHandler().addBehaviour("object_selection_mode", new SelectCommand("objectSelector",getRenderer(),getScene(), () -> getScene().findObserver(),false, result -> this.selectNode(result) ));
        
        enableArcBallControl();

        setupCameraModeSwitching("C");

        initialized = true;
    }

    private Object selectNode(List<SelectResult> result) {
        if (!result.isEmpty()) {
            Collections.sort(result, new Comparator<SelectResult>() {
                @Override
                public int compare(SelectResult lhs, SelectResult rhs) {
                    return lhs.getDistance() > rhs.getDistance() ? -1 : lhs.getDistance() < rhs.getDistance() ? 1 : 0;
                }
            });
            activeNode = result.get(0).getNode();
            System.out.println( "Selected " + activeNode );
            return activeNode;
        }
        return null;
    }

    public void addBox() {
        addBox( new GLVector(0.0f, 0.0f, 0.0f) );
    }

    public void addBox( GLVector position ) {
        addBox( position, new GLVector(1.0f, 1.0f, 1.0f) );
    }


    public void addBox( GLVector position, GLVector size ) {
        addBox( position, size, new GLVector( 0.9f, 0.9f, 0.9f ), false );
    }

    public void addBox( GLVector position, GLVector size, GLVector color, boolean inside ) {

        Material boxmaterial = new Material();
        boxmaterial.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        boxmaterial.setDiffuse( color );
        boxmaterial.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        boxmaterial.setDoubleSided(true);
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Box box = new Box( size, inside );
        box.setMaterial( boxmaterial );
        box.setPosition( position );

        //System.err.println( "Num elements in scene: " + viewer.getSceneNodes().size() );

        activeNode = box;

        getScene().addChild(box);

        if( defaultArcBall ) enableArcBallControl();

        //System.err.println( "Num elements in scene: " + viewer.getSceneNodes().size() );
    }

    public void addSphere() {
        addSphere( new GLVector(0.0f, 0.0f, 0.0f), 1 );
    }

    public void addSphere( GLVector position, float radius ) {
        addSphere( position, radius, new GLVector( 0.9f, 0.9f, 0.9f ) );
    }

    public void addSphere( GLVector position, float radius, GLVector color ) {
        Material material = new Material();
        material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        material.setDiffuse( color );
        material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Sphere sphere = new Sphere( radius, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( position );

        activeNode = sphere;

        getScene().addChild(sphere);

        if( defaultArcBall ) enableArcBallControl();
    }

    public void addPointLight() {
        Material material = new Material();
        material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        material.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
        material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final PointLight light = new PointLight();
        light.setMaterial( material );
        light.setPosition( new GLVector(0.0f, 0.0f, 0.0f) );

        getScene().addChild(light);
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
                out.write( ("facet normal " + normalsFB.get() + " " + normalsFB.get() + " " + normalsFB.get() + "\n").getBytes() );
                out.write( "outer loop\n".getBytes() );
                for( int v = 0; v < 3; v++ ) {
                    out.write( ( "vertex\t" + verticesFB.get() + " " + verticesFB.get() + " " + verticesFB.get() + "\n" ).getBytes() );
                }
                out.write( "endloop\n".getBytes() );
                out.write( "endfacet\n".getBytes() );
            }
            out.write( "endsolid vcg\n".getBytes() );
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /*
    Reading STL through Scenery
     *
    public static void addSTL( String filename ) {
    	Mesh scMesh = new Mesh();
    	scMesh.readFromSTL( filename );

    	scMesh.generateBoundingBox();

    	System.out.println( "Read STL: " + scMesh.getBoundingBoxCoords() );

    	net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( scMesh );

    	System.out.println( "Loaded and converted mesh: " + opsMesh.getVertices().size() );
    	//((DefaultMesh) opsMesh).centerMesh();

    	addMesh( opsMesh );
    }*/

    public void addSTL( String filename ) {

        Mesh scMesh = new Mesh();
        scMesh.readFromSTL( filename );

        //scMesh.generateBoundingBox();

        //net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( scMesh );

        //((DefaultMesh) opsMesh).centerMesh();

        //addMesh( opsMesh );

        addMesh( scMesh );
    }

    public void addObj( String filename ) {
        Mesh scMesh = new Mesh();
        scMesh.readFromOBJ( filename, false );// Could check if there is a MTL to use to toggle flag

        //net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( scMesh );
        //((DefaultMesh) opsMesh).centerMesh();

        //addMesh( opsMesh );

        addMesh( scMesh );
    }

    public void addMesh( Mesh scMesh ) {
        Material material = new Material();
        material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        material.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
        material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        material.setDoubleSided(false);

        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector(1.0f, 1.0f, 1.0f) );

        activeNode = scMesh;

        getScene().addChild( scMesh );

        if( defaultArcBall ) enableArcBallControl();
    }


    public void addMesh( net.imagej.ops.geom.geom3d.mesh.Mesh mesh ) {
        Mesh scMesh = MeshConverter.getSceneryMesh( mesh );

        System.out.println( "Converting to a scenery mesh");

        Material material = new Material();
        material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        material.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
        material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        material.setDoubleSided(true);

        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector(1.0f, 1.0f, 1.0f) );

        activeNode = scMesh;

        getScene().addChild( scMesh );

        if( defaultArcBall ) enableArcBallControl();

		System.out.println( activeNode.getPosition() );
		System.out.println( activeNode.getBoundingBoxCoords()[0]  );
        System.out.println( activeNode.getBoundingBoxCoords()[1]  );
        System.out.println( activeNode.getBoundingBoxCoords()[2]  );
        System.out.println( activeNode.getBoundingBoxCoords()[3]  );
        System.out.println( activeNode.getBoundingBoxCoords()[4]  );
        System.out.println( activeNode.getBoundingBoxCoords()[5]  );
    }

    public void removeMesh( Mesh scMesh ) {
        getScene().removeChild( scMesh );
    }

    public Node getActiveNode() {
        return activeNode;
    }

    public Thread getAnimationThread() {
        return animationThread;
    }

    public void setAnimationThread( Thread newAnimator ) {
        animationThread = newAnimator;
    }

    public void takeScreenshot() {

        System.out.println("Screenshot temporarily disabled");

        /*
    	float[] bounds = viewer.getRenderer().getWindow().getClearglWindow().getBounds();
    	// if we're in a jpanel, this isn't the way to get bounds
    	try {
			Robot robot = new Robot();

			BufferedImage screenshot = robot.createScreenCapture( new Rectangle( (int)bounds[0], (int)bounds[1], (int)bounds[2], (int)bounds[3] ) );

			ImagePlus imp = new ImagePlus( "SceneryViewer_Screenshot", screenshot );

			imp.show();

		} catch (AWTException e) {
			e.printStackTrace();
		}
		*/
    }

    public void enableArcBallControl() {
        GLVector target;
        if( getActiveNode() == null ) {
            target = new GLVector( 0, 0, 0 );
        } else {
            /*if( getActiveNode() instanceof Mesh ) {
                net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( (Mesh)getActiveNode() );
                RealLocalizable center = MeshUtils.getCenter(opsMesh);
                target = new GLVector( center.getFloatPosition(0), center.getFloatPosition(1), center.getFloatPosition(2) );
            }
            else {
                target = getActiveNode().getPosition();
            }*/
            target = getActiveNode().getPosition();
        }

        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        ArcballCameraControl targetArcball = new ArcballCameraControl("mouse_control", cameraSupplier,
                getRenderer().getWindow().getWidth(),
                getRenderer().getWindow().getHeight(), target);
        targetArcball.setMaximumDistance(Float.MAX_VALUE);
        getInputHandler().addBehaviour("mouse_control", targetArcball);
        getInputHandler().addBehaviour("scroll_arcball", targetArcball);
        getInputHandler().addKeyBinding("scroll_arcball", "scroll");
    }

    public void enableFPSControl() {
        Supplier<Camera> cameraSupplier = () -> getScene().findObserver();
        FPSCameraControl fpsControl = new FPSCameraControl("mouse_control", cameraSupplier,
                getRenderer().getWindow().getWidth(),
                getRenderer().getWindow().getHeight());

        getInputHandler().addBehaviour("mouse_control", fpsControl);
        getInputHandler().removeBehaviour("scroll_arcball");
    }

    public Node[] getSceneNodes() {
        CopyOnWriteArrayList<Node> children = getScene().getChildren();

        return getScene().getChildren().toArray( new Node[children.size()] );
    }

    public void deleteSelectedMesh() {
        getScene().removeChild( getActiveNode() );
    }

    public void dispose() {
        getRenderer().setShouldClose(true);
    }

    public void moveCamera(float[] position) {
        getCamera().setPosition( new GLVector(position[0], position[1], position[2]));
    }

    public void moveCamera(double[] position) {
        getCamera().setPosition( new GLVector((float)position[0], (float)position[1], (float)position[2]));
    }

    public String getName() {
        return getApplicationName();
    }

   

    public void addChild(Node node) {
        getScene().addChild(node);
    }

    public void addVolume(Dataset image,float[] voxelDimensions) {

        IterableInterval img = image.getImgPlus();

        // Right now let's only accept byte types, but adding other types is easy
        if(img.firstElement().getClass() == UnsignedByteType.class) {
            long dimensions[] = new long[3];
            img.dimensions(dimensions);
            int bytesPerVoxel = 1;

            byte[] buffer = new byte[1024 * 1024];
            ByteBuffer byteBuffer = MemoryUtil.memAlloc((int) (bytesPerVoxel * dimensions[0] * dimensions[1] * dimensions[2]));

            // We might need to use a RAI instead to handle multiple image types
            //   but we'll be fine for ArrayImg's
            Cursor<UnsignedByteType> cursor = img.cursor();

            int bytesRead = 1;// to init
            UnsignedByteType t;
            while (cursor.hasNext() && (bytesRead > 0)) {
                bytesRead = 0;
                while (cursor.hasNext() && bytesRead < buffer.length) {
                    cursor.fwd();
                    t = cursor.get();
                    buffer[bytesRead] = t.getCodedSignedByte(t.get());
                    bytesRead++;
                }
                byteBuffer.put(buffer,0,bytesRead);
            }
            byteBuffer.flip();

            Volume v = new Volume();
            v.readFromBuffer(image.getName(),byteBuffer,dimensions[0],dimensions[1],dimensions[2],
                    voxelDimensions[0],voxelDimensions[1],voxelDimensions[2],
                    NativeTypeEnum.Byte,1);
            v.setColormap("plasma");

            getScene().addChild(v);
        }
    }
    
//    public static void main(String... args)
//    {
//        ImageJ ij = new ImageJ();
//
//        if( !ij.ui().isVisible() )
//            ij.ui().showUI();
//
//        SciView viewer = new SciView( "SciView", 800, 600 );
//
//        Thread viewerThread = new Thread(){
//            public void run() {
//                viewer.main();
//            }
//        };
//        viewerThread.start();
//
//    }

}

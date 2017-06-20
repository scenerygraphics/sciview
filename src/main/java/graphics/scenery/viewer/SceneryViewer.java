package graphics.scenery.viewer;

import cleargl.GLVector;
import graphics.scenery.*;
import graphics.scenery.backends.Renderer;
import graphics.scenery.viewer.process.MeshConverter;
import net.imagej.ImageJ;
import net.imglib2.RealLocalizable;
import org.scijava.ui.behaviour.ClickBehaviour;

import java.io.*;
import java.nio.FloatBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

public class SceneryViewer extends SceneryDefaultApplication {

    //static ImageJ ij;

    private Thread animationThread;
    private Node activeNode = null;

    private Boolean defaultArcBall = true;

    Camera camera = null;

    public SceneryViewer() {
        super("SceneryViewer", 800, 600, true);
    }

    public SceneryViewer(String applicationName, int windowWidth, int windowHeight) {
        super(applicationName, windowWidth, windowHeight, true);
    }

    @Override
    public void init() {
        setRenderer( Renderer.Factory.createRenderer( getHub(), getApplicationName(), getScene(), 512, 512));
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

//        setRepl(new REPL(getScene(), getRenderer()));
//        getRepl().start();
//        getRepl().showConsoleWindow();

    }

    public Camera getCamera() {
        return camera;
    }


    @Override
    public void inputSetup() {
        //setInputHandler((ClearGLInputHandler) viewer.getHub().get(SceneryElement.INPUT));
        ClickBehaviour objectSelector = new ClickBehaviour() {

            @Override
            public void click( int x, int y ) {
                System.out.println( "Clicked at x=" + x + " y=" + y );
            }
        };
        getInputHandler().useDefaultBindings("");
        getInputHandler().addBehaviour("object_selection_mode", objectSelector);

        enableArcBallControl();
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

        scMesh.generateBoundingBox();

        net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( scMesh );

        //((DefaultMesh) opsMesh).centerMesh();

        addMesh( opsMesh );
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
        material.setDoubleSided(true);

        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector(1.0f, 1.0f, 1.0f) );

        activeNode = scMesh;

        getScene().addChild( scMesh );

        if( defaultArcBall ) enableArcBallControl();
    }

    public void addMesh( net.imagej.ops.geom.geom3d.mesh.Mesh mesh ) {
        Mesh scMesh = MeshConverter.getSceneryMesh( mesh );

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

//		System.err.println( "Number of nodes in scene: " + SceneryViewer.getSceneNodes().size() );
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
            if( getActiveNode() instanceof Mesh ) {
                net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( (Mesh)getActiveNode() );
                RealLocalizable center = MeshUtils.getCenter(opsMesh);
                target = new GLVector( center.getFloatPosition(0), center.getFloatPosition(1), center.getFloatPosition(2) );
            }
            else {
                target = getActiveNode().getPosition();
            }

        }
/*
        ArcballCameraControl targetArcball = new ArcballCameraControl("mouse_control", getScene().findObserver(),
                getRenderer().getWindow().getWidth(),
                getRenderer().getWindow().getHeight(), target);
        targetArcball.setMaximumDistance(Float.MAX_VALUE);
        getInputHandler().addBehaviour("mouse_control", targetArcball);
        getInputHandler().addBehaviour("scroll_arcball", targetArcball);
        getInputHandler().addKeyBinding("scroll_arcball", "scroll");
        */
    }

    public void enableFPSControl() {
        /*
        FPSCameraControl fpsControl = new FPSCameraControl("mouse_control", getScene().findObserver(),
                getRenderer().getWindow().getWidth(),
                getRenderer().getWindow().getHeight());

        getInputHandler().addBehaviour("mouse_control", fpsControl);
        getInputHandler().removeBehaviour("scroll_arcball");
*/
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

    public static void main(String... args)
    {
        ImageJ ij = new ImageJ();

        if( !ij.ui().isVisible() )
            ij.ui().showUI();

        SceneryViewer viewer = new SceneryViewer( "SceneryViewer", 800, 600 );

        Thread viewerThread = new Thread(){
            public void run() {
                viewer.main();
            }
        };
        viewerThread.start();

    }


    public void addChild(Node node) {
        getScene().addChild(node);
    }
}

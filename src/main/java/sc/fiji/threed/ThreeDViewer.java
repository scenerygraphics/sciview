package sc.fiji.threed;

import cleargl.GLMatrix;
import cleargl.GLVector;
import com.jogamp.opengl.GLAutoDrawable;
import ij.ImagePlus;
import net.imagej.ImageJ;
import net.imglib2.RealLocalizable;
import sc.fiji.threed.process.MeshConverter;

import org.scijava.ui.behaviour.ClickBehaviour;

import graphics.scenery.*;
import graphics.scenery.controls.behaviours.ArcballCameraControl;
import graphics.scenery.controls.behaviours.FPSCameraControl;
import graphics.scenery.backends.Renderer;
import graphics.scenery.backends.opengl.OpenGLRenderer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.FloatBuffer;
import java.util.concurrent.CopyOnWriteArrayList;

public class ThreeDViewer extends SceneryDefaultApplication {
	
	static ImageJ ij;
	
	static ThreeDViewer viewer;
	static Thread animationThread;
	static Mesh aMesh = null;
	
	static Boolean defaultArcBall = true;
	
	public ThreeDViewer() {
		super("ThreeDViewer", 800, 600);
	}
	
    public ThreeDViewer(String applicationName, int windowWidth, int windowHeight) {    	
        super(applicationName, windowWidth, windowHeight);        
    }

    public void init() {

    	setRenderer( Renderer.Factory.createRenderer( getApplicationName(), getScene(), 512, 512));
        getHub().add(SceneryElement.RENDERER, getRenderer());

        PointLight[] lights = new PointLight[2];

        for( int i = 0; i < lights.length; i++ ) {
            lights[i] = new PointLight();
            lights[i].setPosition( new GLVector(2.0f * i, 2.0f * i, 2.0f * i) );
            lights[i].setEmissionColor( new GLVector(1.0f, 0.0f, 1.0f) );
            lights[i].setIntensity( 0.2f*(i+1) );
            getScene().addChild( lights[i] );
        }

        Camera cam = new DetachedHeadCamera();
        cam.setPosition( new GLVector(0.0f, 0.0f, -5.0f) );
        cam.setView( new GLMatrix().setCamera(cam.getPosition(), cam.getPosition().plus(cam.getForward()), cam.getUp()) );
        cam.setProjection( new GLMatrix().setPerspectiveProjectionMatrix( (float) (70.0f / 180.0f * java.lang.Math.PI), 1024f / 1024f, 0.1f, 2000.0f) );
        cam.setActive( true );
        getScene().addChild(cam);
        
        viewer = this;
    }

    public void inputSetup() {
    	//setInputHandler((ClearGLInputHandler) viewer.getHub().get(SceneryElement.INPUT));
    	ClickBehaviour objectSelector = new ClickBehaviour() {

            public void click( int x, int y ) {
            	System.out.println( "Clicked at x=" + x + " y=" + y );                
            }
        };
        viewer.getInputHandler().useDefaultBindings("");
        viewer.getInputHandler().addBehaviour("object_selection_mode", objectSelector);

        enableArcBallControl();
        
    }

    public static void addBox() {    	
    	addBox( new GLVector(0.0f, 0.0f, 0.0f) );    	
    }
    
    public static void addBox( GLVector position ) {
    	addBox( position, new GLVector(10.0f, 10.0f, 10.0f) );    	
    }
    
    
    public static void addBox( GLVector position, GLVector size ) {
    	addBox( position, size, new GLVector( 0.9f, 0.9f, 0.9f ) ); 
    }
    
    public static void addBox( GLVector position, GLVector size, GLVector color ) {    	
    	
    	Material boxmaterial = new Material();
        boxmaterial.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        boxmaterial.setDiffuse( color );
        boxmaterial.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        boxmaterial.setDoubleSided(true);
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Box box = new Box( size );
        box.setMaterial( boxmaterial );
        box.setPosition( position );
        
        //System.err.println( "Num elements in scene: " + viewer.getSceneNodes().size() );
        
        viewer.getScene().addChild(box);
        
        if( defaultArcBall ) enableArcBallControl();
        
        //System.err.println( "Num elements in scene: " + viewer.getSceneNodes().size() );
    }

    public static void addSphere() {
    	addSphere( new GLVector(0.0f, 0.0f, 0.0f), 1 );
    }
    
    public static void addSphere( GLVector position, float radius ) {
    	addSphere( position, radius, new GLVector( 0.9f, 0.9f, 0.9f ) );
    }
    
    public static void addSphere( GLVector position, float radius, GLVector color ) {
    	Material material = new Material();
    	material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
    	material.setDiffuse( color );
    	material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Sphere sphere = new Sphere( radius, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( position );        
        
        viewer.getScene().addChild(sphere);
        
        if( defaultArcBall ) enableArcBallControl();
    }
    
    public static void addPointLight() {
    	Material material = new Material();
    	material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
    	material.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
    	material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final PointLight light = new PointLight();
        light.setMaterial( material );
        light.setPosition( new GLVector(0.0f, 0.0f, 0.0f) );        
        
        viewer.getScene().addChild(light);
    }
    
    /* java.nio.FloatBuffer.array version
    public static void writeSCMesh( String filename, Mesh scMesh ) {
    	File f = new File( filename );
        BufferedOutputStream out;
		try {
			out = new BufferedOutputStream( new FileOutputStream( f ) );
			out.write( "solid STL generated by FIJI\n".getBytes() );
			float[] verts = scMesh.getVertices().array();
			float[] norms = scMesh.getNormals().array();
			for( int k = 0; k < verts.length/9; k++ ) {
				int offset = k * 9;
				out.write( ("facet normal " + norms[offset] + " " + norms[offset+1] + " " + norms[offset+2] + "\n").getBytes() );
				out.write( "outer loop\n".getBytes() );
				for( int v = 0; v < 3; v++ ) {
					int voff = v*3;
					out.write( ( "vertex\t" + verts[offset+voff] + " " + verts[offset+voff+1] + " " + verts[offset+voff+2] + "\n" ).getBytes() );
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
    */
    
    public static void writeSCMesh( String filename, Mesh scMesh ) {
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
    
    public static void addSTL( String filename ) {    	
    	Mesh scMesh = new Mesh();
    	scMesh.readFromSTL( filename );
    	
    	scMesh.generateBoundingBox();
    	
    	System.out.println( "Read STL: " + scMesh.getBoundingBox() );    	
    	
    	net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( scMesh );
    	
    	System.out.println( "Loaded and converted mesh: " + opsMesh.getVertices().size() );
    	//((DefaultMesh) opsMesh).centerMesh();

    	addMesh( opsMesh );
    }
    
    public static void addObj( String filename ) {    	
    	Mesh scMesh = new Mesh();
    	scMesh.readFromOBJ( filename, false );// Could check if there is a MTL to use to toggle flag
    	
    	net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( scMesh );    	
    	//((DefaultMesh) opsMesh).centerMesh();    	

    	addMesh( opsMesh );
    }
    
    public static void addMesh( net.imagej.ops.geom.geom3d.mesh.Mesh mesh ) {
    	Mesh scMesh = MeshConverter.getSceneryMesh( mesh );
    	
    	Material material = new Material();
        material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        material.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
        material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        material.setDoubleSided(true);
    	
        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector(1.0f, 1.0f, 1.0f) );        
        
        aMesh = scMesh;
        	            	
    	viewer.getScene().addChild( scMesh );
    	
    	if( defaultArcBall ) enableArcBallControl();
    	
//		System.err.println( "Number of nodes in scene: " + ThreeDViewer.getSceneNodes().size() );
    }
    
    public static void removeMesh( Mesh scMesh ) {
    	viewer.getScene().removeChild( scMesh );
    }
    
    public static Mesh getSelectedMesh() {
    	return aMesh;
    }
    
    public static Thread getAnimationThread() {
    	return ThreeDViewer.animationThread;
    }
    
    public static void setAnimationThread( Thread newAnimator ) {
    	ThreeDViewer.animationThread = newAnimator;
    }
    
    public static void takeScreenshot() {
    	
    	float[] bounds = viewer.getRenderer().getWindow().getClearglWindow().getBounds();
    	// if we're in a jpanel, this isn't the way to get bounds
    	try {
			Robot robot = new Robot();
			
			BufferedImage screenshot = robot.createScreenCapture( new Rectangle( (int)bounds[0], (int)bounds[1], (int)bounds[2], (int)bounds[3] ) );
			
			ImagePlus imp = new ImagePlus( "ThreeDViewer_Screenshot", screenshot );
			
			imp.show();
			
		} catch (AWTException e) {
			e.printStackTrace();
		}
    }
    
    public static void enableArcBallControl() {
    	GLVector target;
    	if( getSelectedMesh() == null ) {
    		target = new GLVector( 0, 0, 0 );
    	} else {
    		net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( getSelectedMesh() );
    		RealLocalizable center = MeshUtils.getCenter(opsMesh);
    		target = new GLVector( center.getFloatPosition(0), center.getFloatPosition(1), center.getFloatPosition(2) );
    	}
    	
    	ArcballCameraControl targetArcball = new ArcballCameraControl("mouse_control", viewer.getScene().findObserver(), 
    			viewer.getRenderer().getWindow().getClearglWindow().getWindowWidth(), 
    			viewer.getRenderer().getWindow().getClearglWindow().getWindowHeight(), target);
    	targetArcball.setMaximumDistance(Float.MAX_VALUE);
    	viewer.getInputHandler().addBehaviour("mouse_control", targetArcball);
    	viewer.getInputHandler().addBehaviour("scroll_arcball", targetArcball);
    	viewer.getInputHandler().addKeyBinding("scroll_arcball", "scroll");
    }
    
    public static void enableFPSControl() {
    	FPSCameraControl fpsControl = new FPSCameraControl("mouse_control", viewer.getScene().findObserver(), 
    			viewer.getRenderer().getWindow().getClearglWindow().getWindowWidth(), 
    			viewer.getRenderer().getWindow().getClearglWindow().getWindowHeight());
    			
    	viewer.getInputHandler().addBehaviour("mouse_control", fpsControl);
    	viewer.getInputHandler().removeBehaviour("scroll_arcball");

    }
    
    public static ThreeDViewer getViewer() {
    	return viewer;
    }
    
    public static Node[] getSceneNodes() {
    	CopyOnWriteArrayList<Node> children = viewer.getScene().getChildren();
    	
    	return viewer.getScene().getChildren().toArray( new Node[children.size()] );
    }
    
	public static void deleteSelectedMesh() {
		viewer.getScene().removeChild( ThreeDViewer.getSelectedMesh() );
	}
    
	public static void main(String... args)
	{	 
		if( ij == null )
			ij = new ImageJ();
		
		if( !ij.ui().isVisible() )
			ij.ui().showUI();

		ThreeDViewer viewer = new ThreeDViewer( "ThreeDViewer", 800, 600 );
		
        viewer.main();
        
	}
}

package sc.fiji;

import cleargl.*;
import ij.ImagePlus;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imagej.ops.geom.geom3d.mesh.Vertex;
import net.imglib2.RealLocalizable;
import sc.fiji.display.process.MeshConverter;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.nativelib.NativeLoader;

import com.jogamp.opengl.GLAutoDrawable;
import scenery.*;
import scenery.controls.behaviours.TargetArcBallCameraControl;
import scenery.controls.behaviours.FPSCameraControl;
import scenery.rendermodules.opengl.DeferredLightingRenderer;

public class ThreeDViewer extends SceneryDefaultApplication {
	
	static ThreeDViewer viewer;
	static Thread animationThread;
	static Mesh aMesh = null;
	
	public ThreeDViewer() {
		super("ThreeDViewer", 800, 600);
	}
	
    public ThreeDViewer(String applicationName, int windowWidth, int windowHeight) {    	
        super(applicationName, windowWidth, windowHeight);
        try {
			NativeLoader.loadLibrary("jinput-osx");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void init(GLAutoDrawable pDrawable) {

        setDeferredRenderer( new DeferredLightingRenderer( pDrawable.getGL().getGL4(), getGlWindow().getWidth(), getGlWindow().getHeight() ) );
        getHub().add(SceneryElement.RENDERER, getDeferredRenderer());

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
        cam.setProjection( new GLMatrix().setPerspectiveProjectionMatrix( (float) (70.0f / 180.0f * java.lang.Math.PI), 1024f / 1024f, 0.1f, 1000.0f) );
        cam.setActive( true );
        getScene().addChild(cam);
        
//        Box hullbox = new Box(new GLVector(50.0f, 50.0f, 50.0f));
//        Material hullboxMaterial = new Material();
//        hullboxMaterial.setDoubleSided(true);
//        hullbox.setMaterial(hullboxMaterial);
        
//        getScene().addChild(hullbox);

        getDeferredRenderer().initializeScene(getScene());

        //getRepl().addAccessibleObject(getScene());
        //getRepl().addAccessibleObject(getDeferredRenderer());
        //getRepl().showConsoleWindow();

        viewer = this;
    }

    public static void addBox() {
    	addBox( new GLVector(0.0f, 0.0f, 0.0f) );    	
    }
    
    public static void addBox( GLVector position ) {
    	addBox( position, new GLVector(0.0f, 0.0f, 0.0f) );    	
    }
    
    public static void addBox( GLVector position, GLVector size ) {
    	Material boxmaterial = new Material();
        boxmaterial.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        //boxmaterial.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
        boxmaterial.setDiffuse( new GLVector(0.7f, 0.7f, 0.7f) );
        boxmaterial.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        boxmaterial.setDoubleSided(true);
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Box box = new Box( size );
        box.setMaterial( boxmaterial );
        box.setPosition( position );
        
        viewer.getScene().addChild(box);
    }
    
    /*public static void boundingBox( GLVector position, GLVector size ) {
    	float thickness = 0.5f;
    	GLVector botRight = position;
    	GLVector topLeft = position.plus( size );
    	
    	addBox( position, new GLVector(size.x(), size.y(), thickness) );    	
    }*/
    
    public static void addSphere() {
    	addSphere( new GLVector(0.0f, 0.0f, 0.0f) );
    }
    
    public static void addSphere( GLVector position ) {
    	Material material = new Material();
    	material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
    	material.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
    	material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
        //boxmaterial.getTextures().put("diffuse", SceneViewer3D.class.getResource("textures/helix.png").getFile() );

        final Sphere sphere = new Sphere( 1.0f, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( position );        
        
        viewer.getScene().addChild(sphere);
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
    
    public static void writeSCMesh( String filename, Mesh scMesh ) {
    	File f = new File( filename );
        BufferedOutputStream out;
		try {
			out = new BufferedOutputStream( new FileOutputStream( f ) );
			out.write( "solid STL generated by FIJI\n".getBytes() );
			float[] verts = scMesh.getVertices();
			float[] norms = scMesh.getNormals();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
    }
    
    public static void addSTL( String filename ) {    	
    	Mesh scMesh = new Mesh();
    	scMesh.readFromSTL( filename );
    	
    	Material material = new Material();
        material.setAmbient( new GLVector(1.0f, 0.0f, 0.0f) );
        material.setDiffuse( new GLVector(0.0f, 1.0f, 0.0f) );
        material.setSpecular( new GLVector(1.0f, 1.0f, 1.0f) );
    	
        scMesh.setMaterial( material );
        scMesh.setPosition( new GLVector(0.0f, 0.0f, 0.0f) );
        
        aMesh = scMesh;
        
    	viewer.getScene().addChild( scMesh );
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
    	
		System.err.println( "Number of nodes in scene: " + ThreeDViewer.getSceneNodes().size() );
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
    	float[] bounds = viewer.getGlWindow().getBounds();
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
    		RealLocalizable center = ((DefaultMesh) opsMesh).getCenter();
    		target = new GLVector( center.getFloatPosition(0), center.getFloatPosition(1), center.getFloatPosition(2) );
    		
    		addSphere( target );
    		
    		//System.out.println( "Center: " + center.getFloatPosition(0) + ", " + center.getFloatPosition(1) + ", " + center.getFloatPosition(2) );
    		//target = new GLVector( 0, 0, 0 );
    	}
    	
    	TargetArcBallCameraControl targetArcball = new TargetArcBallCameraControl("mouse_control", viewer.getScene().findObserver(), 
    			viewer.getGlWindow().getWidth(), viewer.getGlWindow().getHeight(), target);
    	targetArcball.setMaximumDistance(Float.MAX_VALUE);
    	viewer.getInputHandler().addBehaviour("mouse_control", targetArcball);
    	viewer.getInputHandler().addBehaviour("scroll_arcball", targetArcball);
    	viewer.getInputHandler().addKeyBinding("scroll_arcball", "scroll");
    }
    
    public static void enableFPSControl() {
    	FPSCameraControl fpsControl = new FPSCameraControl("mouse_control", viewer.getScene().findObserver(), 
    			viewer.getGlWindow().getWidth(), viewer.getGlWindow().getHeight());
    			
    	viewer.getInputHandler().addBehaviour("mouse_control", fpsControl);
    	viewer.getInputHandler().removeBehaviour("scroll_arcball");
    	//GLMatrix m = new GLMatrix();
    	//m.mult(pGLMatrix);
        
    }
    
    public static void drawBoundingBox() {
    	if( getSelectedMesh() != null ) {
    		net.imagej.ops.geom.geom3d.mesh.Mesh opsMesh = MeshConverter.getOpsMesh( getSelectedMesh() );
    		
    	}
    }
    
    public static ThreeDViewer getViewer() {
    	return viewer;
    }
    
    public static ArrayList<Node> getSceneNodes() {
    	return viewer.getScene().getChildren();
    }
    
	public static void main(String... args)
	{	
		ThreeDViewer viewer = new ThreeDViewer( "ThreeDViewer", 800, 600 );		
        viewer.main();
        // Add extra keybinds like mouse wheel zoom here=
	}
}

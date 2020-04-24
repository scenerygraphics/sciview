package sc.iview.test;

import cleargl.GLVector;
import graphics.scenery.*;
import io.scif.SCIFIOService;
import net.imagej.ImageJService;
import org.joml.Vector3f;
import org.junit.Assert;
import org.scijava.Context;
import org.scijava.service.SciJavaService;
import org.scijava.thread.ThreadService;
import sc.iview.SciView;
import sc.iview.SciViewService;
import sc.iview.vector.JOMLVector3;

public class SciViewTest {

    //@Test
    public void nodeDeletionTest() throws Exception {
        SceneryBase.xinitThreads();

        System.setProperty( "scijava.log.level:sc.iview", "debug" );
        Context context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class, ThreadService.class);

        SciViewService sciViewService = context.service( SciViewService.class );
        SciView sciView = sciViewService.getOrCreateActiveSciView();

        Node sphere = sciView.addSphere();

        Assert.assertEquals( sciView.getAllSceneNodes().length, 7 );

        sciView.deleteNode(sphere);

        Assert.assertEquals( sciView.getAllSceneNodes().length, 6 );

        sciView.closeWindow();
    }

    //@Test
    public void nestedNodeDeletionTest() throws Exception {
        SceneryBase.xinitThreads();

        System.setProperty( "scijava.log.level:sc.iview", "debug" );
        Context context = new Context( ImageJService.class, SciJavaService.class, SCIFIOService.class, ThreadService.class);

        SciViewService sciViewService = context.service( SciViewService.class );
        SciView sciView = sciViewService.getOrCreateActiveSciView();

        Group group = new Group();

        final Material material = new Material();
        material.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
        material.setDiffuse( new Vector3f( 1.0f, 0.0f, 0.0f ) );
        material.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );

        final Sphere sphere = new Sphere( 1, 20 );
        sphere.setMaterial( material );
        sphere.setPosition( JOMLVector3.convert( new JOMLVector3(0,0,0) ) );
        //sphere.setParent(group);
        group.addChild(sphere);
        sciView.addNode(group);

        Assert.assertEquals( sciView.getAllSceneNodes().length, 7 );

        sciView.deleteNode(group);

        Assert.assertEquals( sciView.getAllSceneNodes().length, 6 );

        sciView.closeWindow();
    }

    // sceneResetTest()


}

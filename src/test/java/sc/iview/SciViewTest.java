/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
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

import graphics.scenery.Group;
import graphics.scenery.Node;
import graphics.scenery.SceneryBase;
import graphics.scenery.Sphere;
import io.scif.SCIFIOService;
import net.imagej.ImageJService;
import org.joml.Vector3f;
import org.junit.Assert;
import org.scijava.Context;
import org.scijava.service.SciJavaService;
import org.scijava.thread.ThreadService;

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


        final Sphere sphere = new Sphere( 1, 20, false );
        sphere.ifMaterial( material -> {
            material.setAmbient( new Vector3f( 1.0f, 0.0f, 0.0f ) );
            material.setDiffuse( new Vector3f( 1.0f, 0.0f, 0.0f ) );
            material.setSpecular( new Vector3f( 1.0f, 1.0f, 1.0f ) );
            return null;
        });
        sphere.spatial().setPosition( new Vector3f(0,0,0) );
        group.addChild(sphere);
        sciView.addNode(group);

        Assert.assertEquals( sciView.getAllSceneNodes().length, 7 );

        sciView.deleteNode(group);

        Assert.assertEquals( sciView.getAllSceneNodes().length, 6 );

        sciView.closeWindow();
    }

    // sceneResetTest()


}

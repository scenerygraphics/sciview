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

package sc.iview

import graphics.scenery.*
import graphics.scenery.SceneryBase.Companion.xinitThreads
import io.scif.SCIFIOService
import io.scif.services.DatasetIOService
import net.imagej.ImageJService
import org.joml.Vector3f
import org.junit.Assert
import org.junit.Test
import org.scijava.Context
import org.scijava.service.SciJavaService
import org.scijava.thread.ThreadService
import sc.iview.SciView.Companion.create
import java.io.FileNotFoundException
import java.util.*

class SciViewTests {

    @Test
    @Throws(Exception::class)
    fun nodeDeletionTest() {
        xinitThreads()
        System.setProperty("scijava.log.level:sc.iview", "debug")
        val context = Context(
            ImageJService::class.java,
            SciJavaService::class.java,
            SCIFIOService::class.java,
            ThreadService::class.java
        )
        val sciViewService = context.service(SciViewService::class.java)
        val sciView = sciViewService.orCreateActiveSciView
        val sphere: Node = sciView.addSphere()
        Assert.assertEquals(sciView.allSceneNodes.size.toLong(), 7)
        sciView.deleteNode(sphere)
        Assert.assertEquals(sciView.allSceneNodes.size.toLong(), 6)
        sciView.closeWindow()
    }

    @Test
    @Throws(Exception::class)
    fun nestedNodeDeletionTest() {
        xinitThreads()
        System.setProperty("scijava.log.level:sc.iview", "debug")
        val context = Context(
            ImageJService::class.java,
            SciJavaService::class.java,
            SCIFIOService::class.java,
            ThreadService::class.java
        )
        val sciViewService = context.service(SciViewService::class.java)
        val sciView = sciViewService.orCreateActiveSciView
        val group = Group()
        val material = Material()
        material.ambient = Vector3f(1.0f, 0.0f, 0.0f)
        material.diffuse = Vector3f(1.0f, 0.0f, 0.0f)
        material.specular = Vector3f(1.0f, 1.0f, 1.0f)
        val sphere = Sphere(1f, 20)
        sphere.material = material
        sphere.position = Vector3f(0f, 0f, 0f)
        //sphere.setParent(group);
        group.addChild(sphere)
        sciView.addNode(group)
        Assert.assertEquals(sciView.allSceneNodes.size.toLong(), 7)
        sciView.deleteNode(group)
        Assert.assertEquals(sciView.allSceneNodes.size.toLong(), 6)
        sciView.closeWindow()
    }

    @Test
    @Throws(Exception::class)
    fun deleteActiveMesh() {
        val sciView = create()
        val sphere: Node = sciView.addSphere()
        sciView.setActiveNode(sphere)
        sciView.deleteActiveNode()
        Assert.assertNull(sciView.activeNode)
    }

    @Test
    @Throws(Exception::class)
    fun deletedNodeNotFindable() {
        val sciView = create()
        val sphere: Node = sciView.addSphere()
        sphere.name = "sphere"
        sciView.deleteNode(sphere)
        Assert.assertNotEquals(sphere, sciView.find("sphere"))
    }

    @Test(expected = FileNotFoundException::class)
    @Throws(Exception::class)
    fun testOpenFunction() {
        val sciView = create()
        sciView.open("ThisShouldNotWork")
    }

    @Test
    fun testAlreadyOpenVolume() {
        //TODO Implement
    }

    @Test(expected = IllegalArgumentException::class)
    @Throws(Exception::class)
    fun testOpenFunctionMockFile() {
        val sciView = create()
        sciView.open("src/test/resources/mockFiles/mockFile")
    }

    @Test
    @Throws(Exception::class)
    fun verifyNullCheckForCenterOnPosition() {
        val sciView = create()
        sciView.centerOnPosition(null)
        Assert.assertNull(null)
    }
}

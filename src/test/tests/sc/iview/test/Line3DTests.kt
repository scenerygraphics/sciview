package sc.iview.test

import junit.framework.Assert.assertNull
import org.junit.Test
import sc.iview.node.Line3D

class Line3DTests {

    //tests if a bounding box becomes null when Line does not implement HasGeometry
    @Test
    fun testBoundingBox() {
        val line = Line3D()
        assertNull(line.generateBoundingBox())
    }
}
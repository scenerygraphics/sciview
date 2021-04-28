package sc.iview

import graphics.scenery.Node
import org.junit.Test
import sc.iview.process.ControlPoints
import kotlin.test.assertTrue

class ControlPointTests {

    @Test
    fun testAddPoint() {
        val controlPoints = ControlPoints()
        val node = Node("testMe")
        controlPoints.addPoint(node)
        assertTrue(controlPoints.getNodes().contains(node))
    }
}
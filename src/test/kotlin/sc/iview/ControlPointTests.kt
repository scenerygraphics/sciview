package sc.iview

import graphics.scenery.Node
import org.joml.Vector3f
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

    @Test
    fun testSetPoints() {
        val controlPoints = ControlPoints()
        val nodeList = arrayOf(Vector3f(1f, 2f, 3f), Vector3f(4f, 5f, 6f))
        controlPoints.setPoints(nodeList)
        assertTrue { controlPoints.getNodes()[0].position == Vector3f(1f, 2f, 3f) }
    }

    @Test
    fun testInitializeSciView() {
        val controlPoints = ControlPoints()
        val sv = SciView.create()
        val numberOfNodes = sv.allSceneNodes.size
        controlPoints.initializeSciView(sv, 0f)
        assertTrue  { sv.allSceneNodes.size == numberOfNodes+1 }
    }
}

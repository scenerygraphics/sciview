package sc.iview

import org.junit.Test
import sc.iview.controls.behaviours.Ruler
import kotlin.test.assertTrue

class RulerTests {

    @Test
    fun testRuler() {
        val sv = SciView.create()
        val numberOfNodes = sv.allSceneNodes.size
        val ruler = Ruler(sv)
        ruler.init(1, 2)
        assertTrue { sv.allSceneNodes.size == numberOfNodes+1 }

    }
}

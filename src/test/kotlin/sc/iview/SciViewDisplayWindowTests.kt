package sc.iview

import org.junit.Test
import sc.iview.display.SciViewDisplayWindow
import kotlin.test.assertEquals

class SciViewDisplayWindowTests {

    @Test
    fun  findDisplayWindowTest() {
        val sv = SciView.create()
        val displayWindow = SciViewDisplayWindow(sv)
        assertEquals(displayWindow.findDisplayContentScreenX(), 0)
    }
}

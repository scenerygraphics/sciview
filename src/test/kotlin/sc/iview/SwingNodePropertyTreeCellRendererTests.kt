package sc.iview

import org.junit.Test
import sc.iview.ui.SwingNodePropertyTreeCellRenderer
import java.awt.Color
import kotlin.test.assertTrue

class SwingNodePropertyTreeCellRendererTests {

    @Test
    fun testColor() {
        val snpcr = SwingNodePropertyTreeCellRenderer()
        assertTrue{snpcr.background == Color.WHITE}
    }
}
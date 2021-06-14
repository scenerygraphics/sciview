package sc.iview

import junit.framework.Assert.*
import org.junit.Test
import sc.iview.ui.SwingMainWindow

class SwingMainWindowTests {

    @Test
    fun toggleSideBarTest() {
        val sv = SciView.create()
        val window = SwingMainWindow(sv)
        if(!window.toggleSidebar()) {
            print("yes")
            assertNull(null)
        }
        else {
            assertNotNull(null)
        }
    }
}

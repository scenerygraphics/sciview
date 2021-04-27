package sc.iview

import junit.framework.Assert.assertNull
import org.junit.Test
import sc.iview.ui.SwingNodePropertyEditor

class SwingNodePropertyEditorTests {
    @Test
    fun  testTrySelectedNode() {
        val sv = SciView.create()
        val swing = SwingNodePropertyEditor(sv)
        assertNull(swing.currentNode)
    }
}
package sc.iview.ui

import org.scijava.ui.swing.widget.SwingInputPanel
import org.scijava.widget.InputPanel
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.JLabel
import javax.swing.JPanel

open class SwingGroupingLabelListener(
    protected val groupName: String,
    protected val panel: SwingInputPanel,
    protected val inputPanel: InputPanel<JPanel, JPanel>,
    protected val label: JLabel) : MouseListener {
    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     * @param e the event to be processed
     */
    override fun mouseClicked(e: MouseEvent?) {
        if(e?.clickCount == 1) {
            panel.component.isVisible = !panel.component.isVisible

            if(panel.component.isVisible) {
                label.text = "<html><strong>▼ ${groupName}</strong></html>"
            } else {
                label.text =
                    """<html><strong><span style="color: gray;">▶</span> ${groupName}</strong></html>"""
            }
            inputPanel.component.revalidate()
        }
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     * @param e the event to be processed
     */
    override fun mousePressed(e: MouseEvent?) {
        // noop
    }

    /**
     * Invoked when a mouse button has been released on a component.
     * @param e the event to be processed
     */
    override fun mouseReleased(e: MouseEvent?) {
        // noop
    }

    /**
     * Invoked when the mouse enters a component.
     * @param e the event to be processed
     */
    override fun mouseEntered(e: MouseEvent?) {
        // noop
    }

    /**
     * Invoked when the mouse exits a component.
     * @param e the event to be processed
     */
    override fun mouseExited(e: MouseEvent?) {
        // noop
    }

}
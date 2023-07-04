package sc.iview.controls.behaviours

import org.scijava.ui.behaviour.ClickBehaviour
import org.scijava.ui.behaviour.DragBehaviour

/**
 * Wraps one Click- and one DragBehavior into one behavior.
 * If the user clicks but does not drag, the click behavior is executed otherwise the drag behavior.
 *
 * @author Jan Tiemann
 */
class ClickAndDragWrapper(val click: ClickBehaviour, val drag: DragBehaviour): DragBehaviour {
    var dragged = false
    override fun init(x: Int, y: Int) {
        dragged = false
    }

    override fun drag(x: Int, y: Int) {
        if (!dragged) {
            dragged = true
            drag.init(x,y)
        }
        drag.drag(x,y)
    }

    override fun end(x: Int, y: Int) {
        if (dragged){
            drag.end(x,y)
        } else {
            click.click(x,y)
        }
    }
}
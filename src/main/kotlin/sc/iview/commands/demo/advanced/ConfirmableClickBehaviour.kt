package sc.iview.commands.demo.advanced

import org.scijava.ui.behaviour.ClickBehaviour
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread


/**
 * [ClickBehaviour] that waits [timeout] for confirmation by re-executing the behaviour.
 * Executes [armedAction] on first invocation, and [confirmAction] on second invocation, if
 * it happens within [timeout].  * If [delayedExecution] is true,
 * the [armedAction] will only be executed after the [timeout] ran out and no second click was registered.
 * @author Ulrik Guenther <hello@ulrik.is>
 */
class ConfirmableClickBehaviour(
    val armedAction: (Long) -> Any,
    val confirmAction: (Long) -> Any,
    var timeout: Long = 3000,
    val delayedExecution: Boolean = false
) : ClickBehaviour {
    /** Whether the action is armed at the moment. Action becomes disarmed after [timeout]. */
    private var armed: Boolean = false

    /** Whether the [confirmAction] was fired. Needed for [delayedExecution]. */
    private var confirmed: AtomicBoolean = AtomicBoolean(false)

    /**
     * Action fired at position [x]/[y]. Parameters not used in VR actions.
     */
    override fun click(x: Int, y: Int) {
        if (!armed) {
            armed = true
            if (!delayedExecution) {
                armedAction.invoke(timeout)
            }

            thread {
                Thread.sleep(timeout)
                armed = false
                // Only trigger the delayed armedAction if no confirmedAction was triggered in the meantime
                if (delayedExecution && !confirmed.get()) {
                    armedAction.invoke(timeout)
                }
            }
        } else {
            confirmed.set(true)
            confirmAction.invoke(timeout)
        }
    }
}
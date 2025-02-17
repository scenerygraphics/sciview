package sc.iview.controls.behaviours

import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.utils.lazyLogger
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Keep track of which VR buttons are currently being pressed. This is useful if you want to assign the same button
 * to different behaviors with different combinations. This class helps with managing the button states.
 * Buttons to track first need to be registered with [registerButtonConfig]. Call [pressButton] and [releaseButton]
 * in your behavior init/end methods. */
class MultiVRButtonStateManager() {
    data class ButtonConfig (
        val button: OpenVRHMD.OpenVRButton,
        val trackerRole: TrackerRole
    )

    val logger by lazyLogger()

    /** List of currently pressed buttons, stored as [ButtonConfig] and whether the button is pressed right now. */
    private val activeButtons = ConcurrentHashMap<ButtonConfig, Boolean>()
    private val twoHandedActive = AtomicBoolean(false)
    private val btnConfig: MutableList<ButtonConfig> = mutableListOf()

    init {
        btnConfig.forEach { config ->
            activeButtons[config] = false
        }
    }

    /** Add a new button configuration that the manager will keep track of. */
    fun registerButtonConfig(button: OpenVRHMD.OpenVRButton, trackerRole: TrackerRole) {
        logger.debug("Registered new button config: $button, $trackerRole")
        btnConfig.add(ButtonConfig(button, trackerRole))
    }

    /** Add a button to the list of pressed buttons. */
    fun pressButton(button: OpenVRHMD.OpenVRButton, role: TrackerRole): Boolean {
        val config = ButtonConfig(button, role)
        if (!btnConfig.contains(config)) { return false }
        activeButtons[config] = true
        updateTwoHandedState()
        return true
    }

    /** Remove a button from the list of pressed buttons. */
    fun releaseButton(button: OpenVRHMD.OpenVRButton, role: TrackerRole) {
        val config = ButtonConfig(button, role)
        activeButtons[config] = false
        updateTwoHandedState()
    }

    private fun updateTwoHandedState() {
        // Check each button type to see if it's pressed on both hands
        val anyMatchingButtonsPressed = btnConfig
            .map { it.button }
            .distinct()
            .any { button ->
                val leftPressed = activeButtons[ButtonConfig(button, TrackerRole.LeftHand)] ?: false
                val rightPressed = activeButtons[ButtonConfig(button, TrackerRole.RightHand)] ?: false
                leftPressed && rightPressed
            }

        twoHandedActive.set(anyMatchingButtonsPressed)
    }

    /** Returns true when the same button is currently pressed on both VR controllers. */
    fun isTwoHandedActive(): Boolean = twoHandedActive.get()

    /** Check if a button is currently being pressed. */
    fun isButtonPressed(button: OpenVRHMD.OpenVRButton, role: TrackerRole): Boolean {
        return activeButtons[ButtonConfig(button, role)] ?: false
    }
}
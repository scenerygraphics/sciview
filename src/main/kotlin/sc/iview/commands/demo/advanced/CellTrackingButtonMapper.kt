package sc.iview.commands.demo.advanced

import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.OpenVRHMD.Manufacturer
import graphics.scenery.controls.OpenVRHMD.OpenVRButton
import graphics.scenery.utils.lazyLogger
import org.scijava.ui.behaviour.Behaviour


/** This input mapping manager provides several preconfigured profiles for different VR controller layouts.
 * The active profile is stored in [currentProfile].
 * To change profile, call [loadProfile] with the new [Manufacturer] type.
 * Note that for Quest-like layouts, the lower button always equals [OpenVRButton.A]
 * and the upper button is always [OpenVRButton.Menu]. */
object CellTrackingButtonMapper {

    var eyeTracking: ButtonConfig? = null
    var controllerTracking: ButtonConfig? = null
    var grabObserver: ButtonConfig? = null
    var grabSpot: ButtonConfig? = null
    var playback: ButtonConfig? = null
    var cycleMenu: ButtonConfig? = null
    var faster: ButtonConfig? = null
    var slower: ButtonConfig? = null
    var stepFwd: ButtonConfig? = null
    var stepBwd: ButtonConfig? = null
    var addDeleteReset: ButtonConfig? = null
    var select: ButtonConfig? = null
    var move_forward_fast: ButtonConfig? = null
    var move_back_fast: ButtonConfig? = null
    var move_left_fast: ButtonConfig? = null
    var move_right_fast: ButtonConfig? = null

    private var currentProfile: Manufacturer = Manufacturer.Oculus

    val logger by lazyLogger(System.getProperty("scenery.LogLevel", "info"))

    init {
        loadProfile(Manufacturer.Oculus)
    }

    private val profiles = mapOf(
        Manufacturer.HTC to mapOf(
            "eyeTracking" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Trigger),
            "controllerTracking" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Trigger),
            "grabObserver" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Side),
            "grabSpot" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Side),
            "playback" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Menu),
            "cycleMenu" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Menu),
            "faster" to null,
            "slower" to null,
            "stepFwd" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Left),
            "stepBwd" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Right),
            "addDeleteReset" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Up),
            "select" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Down),
            "move_forward_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Up),
            "move_back_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Down),
            "move_left_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Left),
            "move_right_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Right),
        ),

        Manufacturer.Oculus to mapOf(
            "eyeTracking" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Trigger),
            "controllerTracking" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Trigger),
            "grabObserver" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Side),
            "grabSpot" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Side),
            "playback" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.A),
            "cycleMenu" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Menu),
            "faster" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Up),
            "slower" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Down),
            "stepFwd" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Left),
            "stepBwd" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Right),
            "addDeleteReset" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.Menu),
            "select" to ButtonConfig(TrackerRole.RightHand, OpenVRButton.A),
            "move_forward_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Up),
            "move_back_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Down),
            "move_left_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Left),
            "move_right_fast" to ButtonConfig(TrackerRole.LeftHand, OpenVRButton.Right),
        )
    )

    /** Load the current profile's button mapping */
    fun loadProfile(p: Manufacturer): Boolean {
        currentProfile = p
        val profile = profiles[currentProfile] ?: return false
        eyeTracking = profile["eyeTracking"]
        controllerTracking = profile["controllerTracking"]
        grabObserver = profile["grabObserver"]
        grabSpot = profile["grabSpot"]
        playback = profile["playback"]
        cycleMenu = profile["cycleMenu"]
        faster = profile["faster"]
        slower = profile["slower"]
        stepFwd = profile["stepFwd"]
        stepBwd = profile["stepBwd"]
        addDeleteReset = profile["addDeleteReset"]
        select = profile["select"]
        move_forward_fast = profile["move_forward_fast"]
        move_back_fast = profile["move_back_fast"]
        move_left_fast = profile["move_left_fast"]
        move_right_fast = profile["move_right_fast"]
        return true
    }

    fun getCurrentMapping(): Map<String, ButtonConfig?>?{
        return profiles[currentProfile]
    }

    fun getMapFromName(name: String): ButtonConfig? {
        return when (name) {
            "eyeTracking" -> eyeTracking
            "controllerTracking" -> controllerTracking
            "grabObserver" -> grabObserver
            "grabSpot" -> grabSpot
            "playback" -> playback
            "cycleMenu" -> cycleMenu
            "faster" -> faster
            "slower" -> slower
            "stepFwd" -> stepFwd
            "stepBwd" -> stepBwd
            "addDeleteReset" -> addDeleteReset
            "select" -> select
            "move_forward_fast" -> move_forward_fast
            "move_back_fast" -> move_back_fast
            "move_left_fast" -> move_left_fast
            "move_right_fast" -> move_right_fast
            else -> null
        }
    }

    /** Sets a keybinding and behavior for an [hmd], using the [name] string, a [behavior]
     * and the keybinding if found in the current profile. */
    fun setKeyBindAndBehavior(hmd: OpenVRHMD, name: String, behavior: Behaviour) {
        val config = getMapFromName(name)
        if (config != null) {
            hmd.addKeyBinding(name, config.r, config.b)
            hmd.addBehaviour(name, behavior)
        } else {
            logger.warn("No valid button mapping found for key '$name' in current profile!")
        }
    }
}


/** Combines the [TrackerRole] ([r]) and the [OpenVRHMD.OpenVRButton] ([b]) into a single configuration. */
data class ButtonConfig (
    /** The [TrackerRole] of this button configuration. */
    var r: TrackerRole,
    /** The [OpenVRButton] of this button configuration. */
    var b: OpenVRButton
)
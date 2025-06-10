package sc.iview.commands.demo.advanced

import graphics.scenery.controls.OpenVRHMD
import graphics.scenery.controls.TrackerRole
import graphics.scenery.controls.OpenVRHMD.Manufacturer
import graphics.scenery.controls.OpenVRHMD.OpenVRButton


/** This input mapping manager provides several preconfigured profiles for different VR controller layouts.
 * The active profile is stored in [currentProfile].
 * To change profile, call [loadProfile] with the new [Manufacturer] type.
 * Note that for Quest-like layouts, the lower button always equals [OpenVRButton.A]
 * and the upper button is always [OpenVRButton.Menu]. */
object CellTrackingButtonMapper {

    lateinit var eyeTracking: ButtonConfig
    lateinit var controllerTracking: ButtonConfig
    lateinit var grabObserver: ButtonConfig
    lateinit var grabSpot: ButtonConfig
    lateinit var playback: ButtonConfig
    lateinit var cycleMenu: ButtonConfig
    lateinit var faster: ButtonConfig
    lateinit var slower: ButtonConfig
    lateinit var stepFwd: ButtonConfig
    lateinit var stepBwd: ButtonConfig
    lateinit var addDeleteReset: ButtonConfig
    lateinit var select: ButtonConfig
    lateinit var move_forward_fast: ButtonConfig
    lateinit var move_back_fast: ButtonConfig
    lateinit var move_left_fast: ButtonConfig
    lateinit var move_right_fast: ButtonConfig

    private var currentProfile: Manufacturer = Manufacturer.Oculus

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
    fun loadProfile(p: Manufacturer) {
        currentProfile = p
        val profile = profiles[currentProfile] ?: return
        eyeTracking = profile["eyeTracking"]?.copy() ?: eyeTracking
        controllerTracking = profile["controllerTracking"]?.copy() ?: controllerTracking
        grabObserver = profile["grabObserver"]?.copy() ?: grabObserver
        grabSpot = profile["grabSpot"]?.copy() ?: grabSpot
        playback = profile["playback"]?.copy() ?: playback
        cycleMenu = profile["cycleMenu"]?.copy() ?: cycleMenu
        faster = profile["faster"]?.copy() ?: faster
        slower = profile["slower"]?.copy() ?: slower
        stepFwd = profile["stepFwd"]?.copy() ?: stepFwd
        stepBwd = profile["stepBwd"]?.copy() ?: stepBwd
        addDeleteReset = profile["addDeleteReset"]?.copy() ?: addDeleteReset
        select = profile["select"]?.copy() ?: select
        move_forward_fast = profile["move_forward_fast"]?.copy() ?: select
        move_back_fast = profile["move_back_fast"]?.copy() ?: select
        move_left_fast = profile["move_left_fast"]?.copy() ?: select
        move_right_fast = profile["move_right_fast"]?.copy() ?: select
    }

    fun getCurrentMapping(): Map<String, ButtonConfig> = mapOf(
        "eyeTracking" to eyeTracking,
        "controllerTracking" to controllerTracking,
        "grabObserver" to grabObserver,
        "grabSpot" to grabSpot,
        "playback" to playback,
        "cycleMenu" to cycleMenu,
        "faster" to faster,
        "slower" to slower,
        "stepFwd" to stepFwd,
        "stepBwd" to stepBwd,
        "addDeleteReset" to addDeleteReset,
        "select" to select,
        "move_forward_fast" to move_forward_fast,
        "move_back_fast" to move_back_fast,
        "move_left_fast" to move_left_fast,
        "move_right_fast" to move_right_fast,
    )
}


/** Combines the [TrackerRole] ([r]) and the [OpenVRHMD.OpenVRButton] ([b]) into a single configuration. */
data class ButtonConfig (
    var r: TrackerRole,
    var b: OpenVRButton
)
package sc.iview.commands.edit

import graphics.scenery.volumes.Volume
import net.imagej.lut.LUTService
import okio.withLock
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.widget.Button
import org.scijava.widget.ChoiceWidget
import org.scijava.widget.NumberWidget

/**
 * Inspector panel for [Volume] nodes.
 */
@Plugin(type = Command::class, initializer = "initValues", visible = false)
class VolumeProperties : InspectorInteractiveCommand() {
    @Parameter
    private lateinit var uiSrv: UIService

    @Parameter
    private lateinit var lutService: LUTService

    /* Basic properties */

    @Parameter(required = false, style = ChoiceWidget.LIST_BOX_STYLE, callback = "refreshSceneNodeInDialog")
    private val sceneNode: String? = null

    @Parameter(label = "Timepoint", callback = "updateNodeProperties", style = NumberWidget.SLIDER_STYLE+",group:Volume")
    private var timepoint = 0

    @Parameter(label = "Play", callback = "playTimeSeries", style = "group:Volume")
    private var playPauseButton: Button? = null

    @Volatile
    @Parameter(label = "Speed", min = "1", max = "10", style = NumberWidget.SCROLL_BAR_STYLE + ",group:Volume", persist = false)
    private var playSpeed = 4

    @Parameter(label = "Pixel-to-world ratio", min = "0.00001f", max = "1000000.0f", style = NumberWidget.SPINNER_STYLE + ",group:Volume", persist = false, callback = "updateNodeProperties")
    private var pixelToWorldRatio: Float = 0.001f

    @Parameter(label = "AO steps", style = NumberWidget.SPINNER_STYLE+"group:Volume", callback = "updateNodeProperties")
    private val occlusionSteps = 0

    @Parameter(label = "Volume slicing mode", callback = "updateNodeProperties", style = ChoiceWidget.LIST_BOX_STYLE+"group:Volume")
    private var slicingMode: String = Volume.SlicingMode.Slicing.name

    @Parameter(label = "Dimensions", callback = "updateNodeProperties", visibility = ItemVisibility.MESSAGE, style = "group:Volume")
    private var dimensions: String = ""


    private val slicingModeChoices = Volume.SlicingMode.entries.toMutableList()

    private var playing = false
    private var timeSeriesPlayer: Thread? = null

    init {
        // For Swing, we have a special extension containing the TransferFunctionEditor.
        hasExtensions["Swing"] = SwingVolumeProperties::class.java
    }

    /** Plays a volume time series, if the volume has more than one timepoint. */
    fun playTimeSeries() {
        if (currentSceneNode !is Volume) {
            return
        }
        val v = currentSceneNode as Volume
        playing = !playing
        if (playing && timeSeriesPlayer == null) {
            timeSeriesPlayer = Thread {
                while (playing) {
                    try {
                        var nextTimepoint = v.currentTimepoint + 1
                        if (nextTimepoint >= v.timepointCount) {
                            nextTimepoint = 0
                        }
                        v.goToTimepoint(nextTimepoint)
                        timepoint = nextTimepoint

                        Thread.sleep(1000L / playSpeed)
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
            info.getMutableInput("playPauseButton", Button::class.java).label = "Pause"
            timeSeriesPlayer!!.start()
        } else {
            try {
                timeSeriesPlayer!!.join(200)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
            timeSeriesPlayer = null
            info.getMutableInput("playPauseButton", Button::class.java).setLabel("Play")
        }
    }


    /** Updates this command fields with the node's current properties. */
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    override fun updateCommandFields() {
        val node = currentSceneNode as? Volume ?: return

        fieldsUpdating.withLock {

            val slicingModeInput = info.getMutableInput("slicingMode", String::class.java)
            val slicingMethods =
                Volume.SlicingMode.entries.map { mode: Volume.SlicingMode -> mode.toString() }.toList()
            slicingModeInput.choices = slicingMethods
            slicingMode = slicingModeChoices[Volume.SlicingMode.values().indexOf(node.slicingMode)].toString()

            pixelToWorldRatio = node.pixelToWorldRatio
            dimensions = "${node.getDimensions().x}x${node.getDimensions().y}x${node.getDimensions().z}"

            val maxTimepoint = node.timepointCount - 1
            if(maxTimepoint > 0) {
                timepoint = node.currentTimepoint
                val timepointInput = info.getMutableInput("timepoint", Integer::class.java)
                timepointInput.minimumValue = Integer.valueOf(0) as Integer
                timepointInput.maximumValue = Integer.valueOf(maxTimepoint) as Integer
            } else {
                // Warning! The java.lang prefix needs to be here, otherwise the compiler
                // reverts to the Kotlin types and you'll end up with interesting error messages
                // like "float does not match type float" ;-)
                maybeRemoveInput("timepoint", java.lang.Integer::class.java)
                maybeRemoveInput("playPauseButton", Button::class.java)
                maybeRemoveInput("playSpeed", java.lang.Integer::class.java)
            }
        }
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? Volume ?: return

        fieldsUpdating.withLock {
            node.goToTimepoint(timepoint)
            val slicingModeIndex = slicingModeChoices.indexOf(Volume.SlicingMode.valueOf(slicingMode))
            if(slicingModeIndex != -1) {
                node.slicingMode = Volume.SlicingMode.entries.toTypedArray()[slicingModeIndex]
            }
            node.pixelToWorldRatio = pixelToWorldRatio
            node.spatial().needsUpdateWorld = true
        }
    }

}
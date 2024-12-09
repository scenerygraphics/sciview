import graphics.scenery.utils.extensions.times
import graphics.scenery.volumes.RAIVolume
import graphics.scenery.volumes.TransferFunction
import org.scijava.command.CommandService
import org.scijava.ui.UIService
import sc.iview.SciView
import sc.iview.commands.demo.advanced.EyeTrackingCommand

//  Test class with hardcoded path to open eyetracking directly.
fun main() {
    val sv = SciView.create()
    val context = sv.scijavaContext
    val uiService = context?.service(UIService::class.java)
    uiService?.showUI()

        sv.open("C:/Software/datasets/MastodonTutorialDataset1/datasethdf5.xml")
        val volumes = sv.findNodes { it.javaClass == RAIVolume::class.java }
        volumes.first().let {
            it as RAIVolume
            it.minDisplayRange = 400f
            it.maxDisplayRange = 1500f
            val tf = TransferFunction()
            tf.addControlPoint(0f, 0f)
            tf.addControlPoint(1f, 1f)
            it.transferFunction = tf
            it.spatial().scale *= 50f
            it.spatial().scale.z *= -1f
        }

    val command = sv.scijavaContext!!.getService(CommandService::class.java)
    val argmap = HashMap<String, Any>()
    command.run(EyeTrackingCommand::class.java, true, argmap)

}
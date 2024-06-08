package sc.iview.commands.edit

import graphics.scenery.volumes.SlicingPlane
import graphics.scenery.volumes.VolumeManager
import net.imagej.lut.LUTService
import org.scijava.command.Command
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService

@Plugin(type = Command::class, initializer = "initValues", visible = false)
class SlicingPlaneProperties : InspectorInteractiveCommand() {
    @Parameter
    private lateinit var uiSrv: UIService

    @Parameter
    private lateinit var lutService: LUTService

    /* Targets properties */
    @Parameter(label = "Sliced volumes", callback = "updateNodeProperties", style = "group:Targets")
    private var slicedVolumes: VolumeSelectorWidget.VolumeSelection = VolumeSelectorWidget.VolumeSelection()

    override fun updateCommandFields() {
        val node = currentSceneNode as? SlicingPlane ?: return

        fieldsUpdating = true

        sciView.hub.get<VolumeManager>()?.let {
            slicedVolumes.availableVolumes = it.nodes
        }
        slicedVolumes.clear()
        slicedVolumes.addAll(node.slicedVolumes)
        fieldsUpdating = false
    }

    /** Updates current scene node properties to match command fields.  */
    override fun updateNodeProperties() {
        val node = currentSceneNode as? SlicingPlane ?: return
        if(fieldsUpdating) {
            return
        }

        val old = node.slicedVolumes
        val new = slicedVolumes
        val removed = old.filter { !new.contains(it) }
        val added = new.filter{!old.contains(it)}
        removed.forEach { node.removeTargetVolume(it) }
        added.forEach{node.addTargetVolume(it)}
    }


}
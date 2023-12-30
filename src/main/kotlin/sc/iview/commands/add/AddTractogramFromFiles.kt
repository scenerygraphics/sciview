package sc.iview.commands.add

import TractogramTools
import org.scijava.command.Command
import org.scijava.command.DynamicCommand
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.io.File

@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Add", weight = MenuWeights.ADD), Menu(label = "Tractogram from file ...", weight = MenuWeights.EDIT_ADD_TRACTOGRAM)])
class AddTractogramFromFiles: DynamicCommand() {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter(label = "Tractogram file", style = "extensions:trx")
    private lateinit var tractogramFile: File

    @Parameter(label = "Parcellation file", style = "extensions:nii.gz")
    private lateinit var parcellationFile: File

    @Parameter(label = "Label map parcellation", style = "extensions:csv")
    private lateinit var labelMapFile: File

    @Parameter(label = "Brain Volume", style = "extensions:nii.gz")
    private lateinit var volumeFile: File

    override fun run(){
        val trx = tractogramFile.path
        val parcellation= parcellationFile.path
        val labelMap = labelMapFile.path
        val streamlineCalcs = TractogramTools(scene = sciView.currentScene, hub = sciView.hub)
        val streamlineComponents = streamlineCalcs.setUp(trx, parcellation, labelMap, volumeFile.path)
        streamlineComponents.tractogramParent.metadata["tractogramCalc"] = streamlineCalcs

        sciView.addNode(streamlineComponents.container)
    }
}
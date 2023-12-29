package sc.iview.commands.add

import TractogramTools
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.command.DynamicCommand
import org.scijava.module.ModuleService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.*
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.io.File

@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Add", weight = MenuWeights.ADD), Menu(label = "Tractogram from file ...", weight = MenuWeights.EDIT_ADD_TRACTOGRAM)])
class AddTractogramFromFiles: DynamicCommand() {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter(label = "Tractogram file", style = "extensions:trx") //TODO: I expected, that it would only show .trx files, but it doesn't
    private lateinit var tractogramFile: File

    @Parameter(label = "Parcellation file", style = "extensions:nii.gz") //TODO: autofill if there is a nifti called parcellation in that folder
    private lateinit var parcellationFile: File

    @Parameter(label = "Label map parcellation", style = "extensions:csv") //TODO: autofill if there is a csv in that folder
    private lateinit var labelMapFile: File

    @Parameter(label = "Brain Volume", style = "extensions:nii.gz")
    private lateinit var volumeFile: File

    //@Parameter(label = "Maximum Streamlines Displayed") //min =1, max=5000?
    //private var maxStreamlines = 5000

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
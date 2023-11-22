package sc.iview.commands.add

//import NiftiReader //origins in local dependency
//import Streamlines //origins in local dependency
import graphics.scenery.Hub
import graphics.scenery.RichNode
import net.imagej.display.process.*
import net.imagej.legacy.plugin.*
import net.imagej.legacy.ui.LegacyInputHarvester
import net.imagej.ops.NamespacePreprocessor
import net.imagej.ops.OpEnvironmentPreprocessor
import org.joml.Quaternionf
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.command.DynamicCommand
import org.scijava.display.ActiveDisplayPreprocessor
import org.scijava.display.DisplayPostprocessor
import org.scijava.module.ModuleService
import org.scijava.module.process.*
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.table.process.ResultsPostprocessor
import org.scijava.ui.*
import org.scijava.ui.awt.widget.AWTInputHarvester
import org.scijava.ui.swing.widget.SwingInputHarvester
import org.scijava.ui.swing.widget.SwingMdiInputHarvester
import sc.iview.ActiveSciViewPreprocessor
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import sc.iview.commands.demo.animation.GameOfLife3D
import sc.iview.ui.CustomPropertyUI
import sc.iview.ui.SwingGroupingInputHarvester
import sc.iview.ui.TractogramUI
import java.io.File
import java.util.ArrayList
import java.util.HashMap
import kotlin.math.PI

@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Add", weight = MenuWeights.ADD), Menu(label = "Tractogram from file ...", weight = MenuWeights.EDIT_ADD_TRACTOGRAM)])
class AddTractogramFromFiles: DynamicCommand() { //TODO: check which class is suited best
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var ui: UIService

    @Parameter
    private lateinit var cs: CommandService

    @Parameter
    private lateinit var ms: ModuleService

    //outcommented since using command without actual files
    /*@Parameter(label = "Tractogram file", style = "extensions:trx") //TODO: I expected, that it would only show .trx files, but it doesn't
    private lateinit var tractogramFile: File

    @Parameter(label = "Parcellation file", style = "extensions:nii.gz") //TODO: autofill if there is a nifti called parcellation in that folder
    private lateinit var parcellationFile: File

    @Parameter(label = "Label map parcellation", style = "extensions:csv") //TODO: autofill if there is a csv in that folder
    private lateinit var labelMapFile: File

    @Parameter(label = "Brain Volume", style = "extensions:nii.gz")
    private lateinit var volume: File*/

    @Parameter(label = "Maximum Streamlines Displayed") //min =1, max=5000?
    private var maxStreamlines = 5000

    override fun run(){
        //val trx = tractogramFile.path //using command without acutal files
        //val parcellation= parcellationFile.path //using command without actual files
        //val labelMap = labelMapFile.path //using command without actual files
        //val s = Streamlines(maxStreamlines) //can only be used with local dependency
        //val comp = getComponents(trx, parcellation, labelMap, volume, s) //this is what is normally used to initialize all components correctly
        val comp = dummyComponents()

        val argmap = HashMap<String, Any>()
        argmap.put("node", comp.tractogramParent)
        //argmap.put("streamlines", comp.streamlines) //can only be used with local dependency
        argmap.put("maximumStreamlines", maxStreamlines)
        /**
         * I tried including all pre- and postprocessing steps in order to recreate what normally is automatically done
         * But I only found a way to add these via the module service, not the command service and that didn't work,
         * as the services then didn't get initialized correctly
         * */
        /*val pre = listOf(ActiveSciViewPreprocessor(), SwingGroupingInputHarvester(), AWTInputHarvester(),
            ActiveChannelCollectionPreprocessor(), ActiveDisplayPreprocessor(), ActiveImagePlusPreprocessor(),
            ActiveImagePreprocessor(), ActiveOverlayPreprocessor(), ActivePositionPreprocessor(),
            CheckInputsPreprocessor(), DebugPreprocessor(), DefaultValuePreprocessor(), FileListPreprocessor(),
            FilePreprocessor(), GatewayPreprocessor(), InitPreprocessor(), LegacyInputHarvester(),
            LoadInputsPreprocessor(), LoggerPreprocessor(), MacroPreprocessor(), MacroRecorderPreprocessor(),
            NamespacePreprocessor(), OpEnvironmentPreprocessor(), OverlayPreprocessor(),
            ResultsTablePreprocessor(), RoiManagerPreprocessor(), SaveInputsPreprocessor(), ServicePreprocessor(),
            SwingInputHarvester(), SwingMdiInputHarvester(), UIPreprocessor(), ValidityPreprocessor())
        val post: List<ModulePostprocessor> = listOf(DebugPostprocessor(),
            DisplayPostprocessor(), MacroRecorderPostprocessor(), ResultsPostprocessor()
        )*/
        //cs.run(TractogramUI::class.java, false, argmap)
        cs.run(TractogramUI::class.java, true, argmap)
        //ms.run(cs.getCommand("TractogramUI"), pre, post, argmap)

        sciView.addNode(comp.container)
    }

    //data class Components(val container: RichNode, val tractogramParent: RichNode, val parcellationObject: RichNode, val streamlines: Streamlines) //can only be used with local dependency
    data class Components(val container: RichNode, val tractogramParent: RichNode, val parcellationObject: RichNode)
    //fun getComponents(trx: String, parcellation: String, labelMap: String, volume: File, s: Streamlines): Components{ //can only be used with local dependency
    fun getComponents(trx: String, parcellation: String, labelMap: String, volume: File): Components{
        val container = RichNode()
        container.spatial().rotation = Quaternionf().rotationX(-PI.toFloat()/2.0f)
        container.name = "brain parent"

        // Load nifti volume from file
        //val volume = s.niftiVolumefromFile(volume.path)
        //container.addChild(volume)

        // Load tractogram from file and add it to the scene - can only be used with local dependency
        /*val streamlinesAndTransform = s.tractogramFromFile(trx)
        val verticesOfStreamlines = streamlinesAndTransform.streamlines //verticesOfStreamlines is private
        //val rotation = streamlinesAndTransform.second
        //val translation = streamlinesAndTransform.third
        // Display random selection of all streamlines
        val tractogram = s.displayableStreamlinesFromVerticesList(verticesOfStreamlines.shuffled()
            .take(s._maximumStreamlineCount) as ArrayList<ArrayList<Vector3f>>
        )*/

        val tractogramParent = RichNode()
        //tractogramParent.spatial().rotation = rotation //these transformations seem to not have any effect, but should be relevant since they're read from the .trx file
        //tractogramParent.spatial().position = Vector3f(0.0f, -translation.y/2.0f, translation.z) * 0.1f
        //tractogramParent.addChild(tractogram) //Can only be used with local dependency
        tractogramParent.name = "tractogram parent"
        container.addChild(tractogramParent)

        //val parcellationObject = s.parcellationFromFile(parcellation, labelMap)
        /*val parcellationObject = NiftiReader.niftiFromFile(parcellation, Hub(), labelMap) as RichNode//TODO: hub should not have to be given here
        parcellationObject.name = "parcellation"
        tractogramParent.addChild(parcellationObject)*/ //can only be used with local dependency
        val parcellationObject = RichNode() //dummy-parcellation object
        //return Components(container, tractogramParent, parcellationObject, s) //Can only be used with local dependency
        return Components(container, tractogramParent, parcellationObject)
    }

    fun dummyComponents(): Components{
        val dummyNode1 = RichNode()
        dummyNode1.name = "dummy1"
        val dummyNode2 = RichNode()
        dummyNode2.name = "dummy2"
        val dummyNode3 = RichNode()
        dummyNode3.name = "dummy3"

        dummyNode1.addChild(dummyNode2)
        dummyNode2.addChild(dummyNode3)
        return Components(dummyNode1, dummyNode2, dummyNode3)
    }
}
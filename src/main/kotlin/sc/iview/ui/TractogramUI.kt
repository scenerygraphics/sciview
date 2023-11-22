package sc.iview.ui

//import Streamlines
import graphics.scenery.Mesh
import graphics.scenery.RichNode
import graphics.scenery.utils.lazyLogger
import graphics.scenery.volumes.Colormap
import net.imagej.widget.HistogramBundle
import org.joml.Vector3f
import org.scijava.ItemIO
import org.scijava.ItemVisibility
import org.scijava.command.Command
import org.scijava.command.CommandModuleItem
import org.scijava.command.CommandService
import org.scijava.command.DynamicCommand
import org.scijava.module.ModuleItem
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.Button
import org.scijava.widget.ChoiceWidget
import org.scijava.widget.NumberWidget
import sc.iview.SciView
import java.io.IOException
import java.util.HashMap
import kotlin.properties.Delegates

@Plugin(type = Command::class)
class TractogramUI : DynamicCommand() {
    @Parameter
    private lateinit var node: RichNode

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var cs: CommandService

    private val allStreamlines: ArrayList<ArrayList<Vector3f>> = ArrayList()
    private val maximumStreamlines: Int = 1000

    //@Parameter(initializer = "HistoInit")
    //private var histogramBundle: HistogramBundle? = null
    //TODO: if no histogram possible, display average curvature and fiber length
    //TODO: at least display parcellation and volume file selected, perhaps with possibility to change it;
    //TODO: display origin of tractogram only as output

    @Parameter(label = "Select brain region", choices = [])
    private var firstBrainRegion: String = "none"

    @Parameter(label = "Select brain region", choices = [])
    private var secondBrainRegion: String = "none"

    @Parameter(label = "Calculate Selection", callback = "selection")
    private lateinit var selection: Button

    @Parameter(label = "Exclusion regions", style = ChoiceWidget.RADIO_BUTTON_HORIZONTAL_STYLE + ",group:selected regions")
    private var exclusion = false

    //TODO: Provide more options to combine regions with AND / OR / NOT, ... criteria

    // Minimum length filter via slider
    @Parameter(label = "Select minimal length of fibers to be displayed", persist = false, callback = "filterLength")
    private var minLength: Float = 0f

    // Maximum length filter via slider
    @Parameter(label = "Select maximal length of fibers to be displayed", persist = false, callback = "filterLength")
    private var maxLength: Float = 0f

    // Maximum length filter via slider
    @Parameter(label = "Select maximal local curvature allowed", persist = false, callback = "filterCurvature")
    private var maxCurvature: Float = 0f

    override fun run() {
        //initializeInputs() //can only be used with local dependency
        dummyInitializeInputs()

        sciView.attachCustomPropertyUIToNode(node!!,
            CustomPropertyUI(
                this,
                listOf(
                    "firstBrainRegion",
                    "secondBrainRegion",
                    "minLength",
                    "maxLength",
                    "selection",
                    "exclusion",
                    "maxCurvature"
                )
            )
        )

        // for all children of node attach customUI with fiber length
        /*val children = tractogram.children
        for (child in children) {
            val argmap = HashMap<String, Any>() //all arguments that should be given to the TractogramUI can be stated here
            argmap.put("streamline", child) //also add files in the same way
            cs.run(StreamlineUI::class.java, false, argmap)
        }*/
    }

    /**
     * Method to test, whether adapting the inputs works with easy adaptions
     * */
    private fun dummyInitializeInputs(){
        val firstBrainRegionItem = info.getMutableInput("firstBrainRegion", String::class.java)
        val secondBrainRegionItem = info.getMutableInput("secondBrainRegion", String::class.java)
        firstBrainRegionItem.choices = listOf("dummy1", "dummy2", "dummy3")
        secondBrainRegionItem.choices = listOf("dummy4", "dummy5", "dummy6")

        val minLengthItem = info.getMutableInput("minLength", java.lang.Float::class.java)
        val maxLengthItem = info.getMutableInput("maxLength", java.lang.Float::class.java)

        minLengthItem.minimumValue = 0f as java.lang.Float
        minLengthItem.maximumValue = 30f as java.lang.Float
        maxLengthItem.minimumValue = 10f as java.lang.Float
        maxLengthItem.maximumValue = 15f as java.lang.Float
        minLengthItem.widgetStyle = NumberWidget.SLIDER_STYLE
        maxLengthItem.widgetStyle = NumberWidget.SLIDER_STYLE
    }

    private fun initializeInputs() {
        // Initialize brain region selection with values from parcellation
        val firstBrainRegionItem = info.getMutableInput("firstBrainRegion", String::class.java)
        val secondBrainRegionItem = info.getMutableInput("secondBrainRegion", String::class.java)
        val parcellation = node.getChildrenByName("parcellation").get(0) // TODO: check, if there is a parcellation
        val brainAreas = parcellation.metadata.get("brainAreas")
        firstBrainRegionItem.choices = (brainAreas as? ArrayList<String>) ?: ArrayList()
        secondBrainRegionItem.choices = (brainAreas as? ArrayList<String>) ?: ArrayList()

        // read fiber length from metadata of tractogram and create sliders with according min/max values
        val tractogram = node.getChildrenByName("tractogram").get(0) //TODO: check, if there is a tractogram
        val minLengthItem = info.getMutableInput("minLength", java.lang.Float::class.java)
        val maxLengthItem = info.getMutableInput("maxLength", java.lang.Float::class.java)
        //val minimumLenght = tractogram.metadata.get("minLength") as java.lang.Float
        val minimumLength = 0f as java.lang.Float
        val maximumLength = tractogram.metadata.get("maxLength") as java.lang.Float //TODO: check if available
        minLengthItem.minimumValue = minimumLength
        minLengthItem.maximumValue = maximumLength
        maxLengthItem.minimumValue = minimumLength
        maxLengthItem.maximumValue = maximumLength
        minLengthItem.widgetStyle = NumberWidget.SLIDER_STYLE
        maxLengthItem.widgetStyle = NumberWidget.SLIDER_STYLE
    }


    fun filterLength() {
        //TODO implement
        print("filterLength")
    }

    fun selection() { //method can only be used with local dependency
        /*//TODO use exclusion-flag -> needs different implementation in StreamlineSelector
        var streamlineSelection = allStreamlines
        // set all children of parcellation invisible
        val parcellation = node.getChildrenByName("parcellation").get(0) // TODO: check, if there is a parcellation
        val children = parcellation.children
        for (child in children) {
            child.visible = false
        }
        if(firstBrainRegion != null){
            val mesh = getBrainMeshFromName(firstBrainRegion) //TODO: check if available
            mesh.visible = true
            streamlineSelection = StreamlineSelector.preciseStreamlineSelection(mesh, streamlineSelection) as ArrayList<ArrayList<Vector3f>>
        }
        if(secondBrainRegion != null){
            val mesh = getBrainMeshFromName(secondBrainRegion) //TODO: check if available
            mesh.visible = true
            streamlineSelection = StreamlineSelector.preciseStreamlineSelection(mesh, streamlineSelection) as ArrayList<ArrayList<Vector3f>>
        }else if(firstBrainRegion == null){
            // TODO: display message, that no selection can be determined without given brain regions
        }
        //TODO: handle empty streamline selection
        val reducedTractogram = Streamlines().displayableStreamlinesFromVerticesList(streamlineSelection.shuffled().take(maximumStreamlines) as ArrayList<ArrayList<Vector3f>>) //TODO: take should take the maximum number of streamlines that should be displayed
        node.getChildrenByName("tractogram").get(0).visible = false
        node.addChild(reducedTractogram)*/
    }

    fun getBrainMeshFromName(name : String) : Mesh{
        //TODO implement
        print("getBrainMeshFromName")
        val mesh = node.getChildrenByName("parcellation").get(0).getChildrenByName(name).get(0) as Mesh
        return mesh
    }

    fun filterCurvature(){
        //TODO implement
        print("filterCurvature")
    }

    //fun histoInit(){
    //    histogramBundle = HistogramBundle()
    //}
}
package sc.iview.ui

import org.scijava.`object`.ObjectService
import org.scijava.convert.ConvertService
import org.scijava.log.LogService
import org.scijava.module.Module
import org.scijava.module.ModuleException
import org.scijava.module.ModuleItem
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.swing.widget.SwingInputHarvester
import org.scijava.widget.*
import java.util.*
import javax.swing.JPanel

@Plugin(type = org.scijava.module.process.PreprocessorPlugin::class, priority = InputHarvester.PRIORITY)
class SwingGroupingInputHarvester : SwingInputHarvester() {

    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var widgetService: WidgetService

    @Parameter
    private lateinit var objectService: ObjectService

    @Parameter
    private lateinit var convertService: ConvertService


    // -- InputHarvester methods --
    @Throws(ModuleException::class)
    override fun buildPanel(inputPanel: InputPanel<JPanel, JPanel>, module: Module) {
        val inputs = module.info.inputs()
        val models = ArrayList<WidgetModel>()
        val sortedInputs = inputs.sortedBy {
            val sortKey = it.label.substringBeforeLast("]").substringAfter("[")
            sortKey
        }

        for (item in sortedInputs) {
            val model = addInput(inputPanel, module, item)
            if (model != null) models.add(model)
        }

        // mark all models as initialized
        for (model in models) model.isInitialized = true

        // compute initial preview
        module.preview()
    }

    // -- Helper methods --
    @Throws(ModuleException::class)
    private fun <T, P, W> addInput(inputPanel: InputPanel<P, W>,
                                   module: Module, item: ModuleItem<T>): WidgetModel? {
        val name = item.name
        if(item.label.contains("[") && item.label.contains("]")) {
            item.label = item.label.substringAfterLast("]")
        }
        val resolved = module.isInputResolved(name)
        if (resolved) return null // skip resolved inputs
        val type = item.type
        val model = widgetService.createModel(inputPanel, module, item, getObjects(type))
        val widgetType: Class<W> = inputPanel.getWidgetComponentType()
        val widget = widgetService.create(model)
        if (widget == null) {
            log.debug("No widget found for input: " + model.item.name)
        }
        if (widget != null && widget.componentType == widgetType) {
            @Suppress("UNCHECKED_CAST") val typedWidget = widget as InputWidget<*, W>
            inputPanel.addWidget(typedWidget)
            return model
        }
        if (item.isRequired) {
            throw ModuleException("A " + type.simpleName +
                    " is required but none exist.")
        }

        // item is not required; we can skip it
        return null
    }

    /** Asks the object service and convert service for valid choices  */
    private fun getObjects(type: Class<*>): List<*> {
        val compatibleInputs = ArrayList(convertService.getCompatibleInputs(type))
        compatibleInputs.addAll(objectService.getObjects(type))
        return compatibleInputs
    }
}
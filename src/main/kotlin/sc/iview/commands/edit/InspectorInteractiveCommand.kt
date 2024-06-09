package sc.iview.commands.edit

import graphics.scenery.Node
import graphics.scenery.volumes.Colormap
import net.imagej.lut.LUTService
import org.scijava.command.InteractiveCommand
import org.scijava.event.EventService
import org.scijava.log.LogService
import org.scijava.module.Module
import org.scijava.module.ModuleItem
import org.scijava.plugin.Parameter
import sc.iview.SciView
import java.util.concurrent.ConcurrentHashMap

abstract class InspectorInteractiveCommand : InteractiveCommand() {
    @Parameter
    protected lateinit var sciView: SciView

    @Parameter
    protected lateinit var events: EventService

    @Parameter
    protected lateinit var log: LogService

    var fieldsUpdating = true
        protected set
    var currentSceneNode: Node? = null
        protected set

    abstract fun updateCommandFields()
    protected abstract fun updateNodeProperties()

    var hasExtensions = hashMapOf<String, Class<*>>()
        protected set

    fun setSceneNode(node: Node?) {
        Colormap.Companion.lutService = sciView.scijavaContext?.getService(LUTService::class.java)

        currentSceneNode = node
        updateCommandFields()
    }

    protected fun initValues() {
//        rebuildSceneObjectChoiceList()
//        refreshSceneNodeInDialog()
        updateCommandFields()
    }

    protected fun <T> maybeRemoveInput(name: String, type: Class<T>) {
        try {
            val item = info.getMutableInput(name, type) ?: return
            info.removeInput(item)
        } catch (npe: NullPointerException) {
            return
        }
    }

    fun addInput(input: ModuleItem<*>, module: Module) {
        super.addInput(input)
        inputModuleMaps[input] = module
    }

    fun getCustomModuleForModuleItem(moduleInfo: ModuleItem<*>): Module? {
        val custom = inputModuleMaps[moduleInfo]
        log.debug("Custom module found: $custom")
        return custom
    }

    companion object {
        const val PI_NEG = "-3.142"
        const val PI_POS = "3.142"

        private val inputModuleMaps = ConcurrentHashMap<ModuleItem<*>, Module>()
    }

    data class UsageCondition(
        val condition: (Node) -> Boolean,
        val commandClass: Class<out InspectorInteractiveCommand>
    )
}
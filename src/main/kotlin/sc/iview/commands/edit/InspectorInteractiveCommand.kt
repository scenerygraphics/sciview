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
import java.util.concurrent.locks.ReentrantLock

/**
 * Custom inspector panels, e.g. for particular [Node] types.
 */
abstract class InspectorInteractiveCommand : InteractiveCommand() {
    @Parameter
    protected lateinit var sciView: SciView

    @Parameter
    protected lateinit var events: EventService

    @Parameter
    protected lateinit var log: LogService

    /** Lock to indicated whether the command's fields or the node's properties are currently being updated. */
    var fieldsUpdating = ReentrantLock()
        protected set
    /** The current node the inspector is focused on. */
    var currentSceneNode: Node? = null
        protected set

    /** Updates this command fields with the node's current properties. */
    abstract fun updateCommandFields()

    /** Updates current scene node properties to match command fields.  */
    protected abstract fun updateNodeProperties()

    /** With this hash map, inspector panels can declare that they include a special version for a particular UI type. */
    var hasExtensions = hashMapOf<String, Class<*>>()
        protected set

    /** Updates the currently-active node for the inspector. */
    fun setSceneNode(node: Node?) {
        Colormap.Companion.lutService = sciView.scijavaContext?.getService(LUTService::class.java)

        currentSceneNode = node
        updateCommandFields()
    }

    /**
     * Remove an input given by [name] of a certain [type].
     */
    protected fun <T> maybeRemoveInput(name: String, type: Class<T>) {
        try {
            val item = info.getMutableInput(name, type) ?: return
            info.removeInput(item)
        } catch (npe: NullPointerException) {
            log.info("Input field $name of type ${type.simpleName} not found, therefore it can't be removed.")
            return
        }
    }

    /**
     * Adds a new [input] to a given [module].
     */
    fun addInput(input: ModuleItem<*>, module: Module) {
        super.addInput(input)
        inputModuleMaps[input] = module
    }

    /**
     * Checks for custom module items based on [moduleInfo].
     */
    fun getCustomModuleForModuleItem(moduleInfo: ModuleItem<*>): Module? {
        val custom = inputModuleMaps[moduleInfo]
        log.debug("Custom module found: $custom")
        return custom
    }

    /**
     * Companion object with helper constants.
     */
    companion object {
        const val PI_NEG = "-3.142"
        const val PI_POS = "3.142"

        private val inputModuleMaps = ConcurrentHashMap<ModuleItem<*>, Module>()
    }

    /**
     * Data class for storing [condition]s for usage of a particular [commandClass]. This
     * class is used to determine which [InspectorInteractiveCommand]s will be displayed
     * for a particular [Node] type.
     */
    data class UsageCondition(
        val condition: (Node) -> Boolean,
        val commandClass: Class<out InspectorInteractiveCommand>
    )
}
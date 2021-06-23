package sc.iview.ui

import org.scijava.module.Module
import org.scijava.module.ModuleItem

/**
 * Class for encapsulation of custom UI elements and their [Module]s.
 *
 * The constructor of the class performs a check upon initialisation to ensure that
 * all the [items] given actually do exist as inputs of the given [module].
 *
 * @author Ulrik Guenther <hello@ulrik.is>
 */
class CustomPropertyUI(val module: Module, val items: List<String>) {
    init {
        val inputNames = module.inputs.map { it.key }
        items.forEach {
            if(it !in inputNames) {
                throw IllegalStateException("Input $it not in list of known inputs for module $module.")
            }
        }
    }

    /**
     * Returns a list of [ModuleItem]s derived from the names given in [items].
     */
    fun getMutableInputs(): List<ModuleItem<*>> {
        return items.map { module.info.getInput(it) }
    }
}
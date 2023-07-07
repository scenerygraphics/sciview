/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2021 SciView developers.
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
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
import org.scijava.ui.swing.widget.SwingInputPanel
import org.scijava.widget.*
import sc.iview.commands.edit.Properties
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.*
import javax.swing.JLabel
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
        val sortedInputs = inputs.groupBy {
            val sortKey = it.widgetStyle.substringAfter("group:").substringBefore(",")
            sortKey
        }

        sortedInputs.forEach { group ->
            // no empty groups, and skip resolved inputs, aka services
            if(group.value.isEmpty() || group.value.all { module.isInputResolved(it.name) }) {
                return@forEach
            }

            val panel = SwingInputPanel()
            val labelPanel = SwingInputPanel()
            val label = JLabel("<html><strong>▼ ${group.key}</strong></html>")

            label.addMouseListener(object: MouseListener {
                /**
                 * Invoked when the mouse button has been clicked (pressed
                 * and released) on a component.
                 * @param e the event to be processed
                 */
                override fun mouseClicked(e: MouseEvent?) {
                    if(e?.clickCount == 1) {
                        panel.component.isVisible = !panel.component.isVisible

                        if(panel.component.isVisible) {
                            label.text = "<html><strong>▼ ${group.key}</strong></html>"
                        } else {
                            label.text = """<html><strong><span style="color: gray;">▶</span> ${group.key}</strong></html>"""
                        }
                        inputPanel.component.revalidate()
                    }
                }

                /**
                 * Invoked when a mouse button has been pressed on a component.
                 * @param e the event to be processed
                 */
                override fun mousePressed(e: MouseEvent?) {
                }

                /**
                 * Invoked when a mouse button has been released on a component.
                 * @param e the event to be processed
                 */
                override fun mouseReleased(e: MouseEvent?) {
                }

                /**
                 * Invoked when the mouse enters a component.
                 * @param e the event to be processed
                 */
                override fun mouseEntered(e: MouseEvent?) {
                }

                /**
                 * Invoked when the mouse exits a component.
                 * @param e the event to be processed
                 */
                override fun mouseExited(e: MouseEvent?) {
                }

            })

            labelPanel.component.add(label)
            inputPanel.component.add(labelPanel.component, "wrap")
            // hidemode 3 ignores the space taken up by components when rendered
            inputPanel.component.add(panel.component, "wrap,hidemode 3")

            for (item in group.value) {
                val model = addInput(panel, module, item)
                if (model != null) {
                    log.info("Adding input ${item.name}/${item.label}")
                    models.add(model)
                } else {
                    log.error("Model for ${item.name}/${item.label} is null!")
                }
            }
        }

        // mark all models as initialized
        for (model in models) model.isInitialized = true

        // compute initial preview
        module.preview()
    }

    // -- Helper methods --
    @Throws(ModuleException::class)
    private fun <T, P, W> addInput(inputPanel: InputPanel<P, W>,
                                   m: Module, item: ModuleItem<T>): WidgetModel? {
        val module = if(m is Properties) {
            m.getCustomModuleForModuleItem(item) ?: m
        } else {
            m
        }

        val name = item.name
        if(item.label.contains("[") && item.label.contains("]")) {
            item.label = item.label.substringAfterLast("]")
        }
        val resolved = module.isInputResolved(name)
        if (resolved && module == m) {
            log.warn("Input ${item.name} is resolved, skipping")
            return null
        } // skip resolved inputs
        val type = item.type
        val model = widgetService.createModel(inputPanel, module, item, getObjects(type))
        val widgetType = inputPanel.widgetComponentType
        val widget = widgetService.create(model)
        if (widget == null) {
            log.warn("No widget found for input: " + model.item.name)
        }
        if (widget != null && widget.componentType == widgetType) {
            @Suppress("UNCHECKED_CAST")
            val typedWidget = widget as InputWidget<*, W>
            inputPanel.addWidget(typedWidget)
            return model
        }
        if (item.isRequired) {
            log.warn("${item.name} is required but doesn't exist")
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

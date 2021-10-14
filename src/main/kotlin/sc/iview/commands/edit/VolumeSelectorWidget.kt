/*
 * #%L
 * SciJava UI components for Java Swing.
 * %%
 * Copyright (C) 2010 - 2020 SciJava developers.
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
package sc.iview.commands.edit

import graphics.scenery.volumes.Volume
import org.scijava.plugin.Plugin
import org.scijava.ui.swing.widget.SwingChoiceWidget
import org.scijava.ui.swing.widget.SwingInputWidget
import org.scijava.widget.InputWidget
import org.scijava.widget.WidgetModel
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*

/**
 *
 * @author FrauZufall
 * @author Jan Tiemann
 */
@Plugin(type = InputWidget::class, priority = SwingChoiceWidget.PRIORITY + 1)
class VolumeSelectorWidget : SwingInputWidget<VolumeSelectorWidget.VolumeSelection>(), ActionListener {
    private var checkBoxes: MutableList<Pair<Volume, JCheckBox>> = ArrayList()


    // -- ActionListener methods --
    override fun actionPerformed(e: ActionEvent) {
        updateModel()
    }

    override fun get(): WidgetModel {
        return super.get()
    }

    // -- InputWidget methods --
    override fun getValue(): VolumeSelection {
        val out = VolumeSelection()
        checkBoxes
            .filter { it.second.isSelected }
            .map { out.add(it.first) }
        return out
    }

    // -- WrapperPlugin methods --
    override fun set(model: WidgetModel) {
        super.set(model)
        val connections = get().value as VolumeSelection
        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.Y_AXIS)
        checkBoxes.clear()
        connections.let {
            for (item in it.availableVolumes) {
                val checkBox = JCheckBox(item.name, false)
                checkBox.addActionListener(this)
                buttonPanel.add(checkBox)
                checkBoxes.add(item to checkBox)
            }
        }
        component!!.add(buttonPanel)
        refreshWidget()
    }

    // -- Typed methods --
    override fun supports(model: WidgetModel): Boolean {
        return super.supports(model) && model.isType(VolumeSelection::class.java)
    }

    // -- AbstractUIInputWidget methods ---
    public override fun doRefresh() {
        val connections: VolumeSelection = get().value as VolumeSelection
        for (checkBox in checkBoxes) {
            if(checkBox.first in connections) {
                checkBox.second.isSelected = true
            }
        }
    }

     class VolumeSelection: ArrayList<Volume>(){
        //Workaround to get those values into the widget
        var availableVolumes: List<Volume> = emptyList()
    }
}

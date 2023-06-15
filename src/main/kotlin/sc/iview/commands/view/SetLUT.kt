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
package sc.iview.commands.view

import graphics.scenery.Node
import net.imglib2.display.ColorTable
import org.scijava.command.Command
import org.scijava.command.CommandService
import org.scijava.command.DynamicCommand
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.prefs.PrefService
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SET_LUT
import sc.iview.commands.demo.basic.VolumeRenderDemo
import java.io.IOException
import java.util.*


/**
 * Command to set the currently used Look Up Table (LUT). This is a colormap for the volume.
 *
 * @author Kyle Harrington
 */
@Suppress("TYPE_INFERENCE_ONLY_INPUT_TYPES_WARNING")
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Set LUT", weight = VIEW_SET_LUT)])
class SetLUT : DynamicCommand() {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter(label = "Node")
    private lateinit var node: Node

    @Parameter(label = "Selected LUT", choices = [], callback = "lutNameChanged", initializer = "initLutName")
    private lateinit var lutName: String

    @Parameter(label = "LUT Selection")
    private lateinit var colorTable: ColorTable

    @Parameter
    private lateinit var prefs: PrefService

    protected fun initLutName() {
        lutName = "Fire.lut"
    }

    protected fun lutNameChanged() {
        try {
            colorTable = sciView.getLUT(lutName)!!
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun initialize() {
        val lutNameItem = info.getMutableInput("lutName", String::class.java)
        lutNameItem.choices = sciView.getAvailableLUTs()

        var persistLutName = prefs.get(SetLUT::class.java, "lutName");

        try {
            colorTable = sciView.getLUT(persistLutName)!!
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        node.metadata["sciview.colormapName"] = lutName
        sciView.setColormap(node, colorTable)
        // Trigger an update to the UI for the colormap
        sciView.publishNode(node)
    }
}

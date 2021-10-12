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
package sc.iview.commands.edit.add

import graphics.scenery.proteins.Protein
import graphics.scenery.proteins.RibbonDiagram
import org.joml.Vector3f
import org.scijava.command.Command
import org.scijava.command.DynamicCommand
import org.scijava.command.InteractiveCommand
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.ui.UIService
import org.scijava.widget.Button
import org.scijava.widget.FileWidget
import sc.iview.SciView
import sc.iview.commands.MenuWeights
import java.io.File
import java.io.FileFilter

/**
 * Command to add a box to the scene
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "Edit", weight = MenuWeights.EDIT), Menu(label = "Add", weight = MenuWeights.EDIT_ADD), Menu(label = "Protein from files ...", weight = MenuWeights.EDIT_ADD_BOX)])
class AddProteinFromFiles : DynamicCommand() {

    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var ui: UIService

    @Parameter(label = "Protein files", style = "extensions:pdb/mmtf/cif")
    private lateinit var files: Array<File>

    @Parameter(label = "Scale")
    private var scale: Float = 0.1f

    override fun run() {
        files.forEach { file ->
            val ribbon = RibbonDiagram(Protein.fromID(file.absolutePath))
            ribbon.name = file.name
            ribbon.spatial().scale = Vector3f(scale)
            sciView.addNode(ribbon, true)
        }
    }
}
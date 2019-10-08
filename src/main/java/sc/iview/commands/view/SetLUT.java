/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2018 SciView developers.
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
package sc.iview.commands.view;

import graphics.scenery.Node;
import net.imagej.lut.LUTService;
import net.imglib2.display.AbstractArrayColorTable;
import net.imglib2.display.ColorTable;
import org.scijava.command.Command;
import org.scijava.command.DynamicCommand;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.io.IOException;
import java.util.ArrayList;

import static sc.iview.commands.MenuWeights.VIEW;
import static sc.iview.commands.MenuWeights.VIEW_SCREENSHOT;
import static sc.iview.commands.MenuWeights.VIEW_SET_LUT;

@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "View", weight = VIEW), //
                 @Menu(label = "Set LUT", weight = VIEW_SET_LUT) })
public class SetLUT extends DynamicCommand {

    @Parameter
    private SciView sciView;

    @Parameter
    private LUTService lutService;

    @Parameter(label = "Node")
    private Node node;

    @Parameter(label = "Selected LUT", choices = {}, callback = "lutNameChanged")
    private String lutName;

    @Parameter(label = "LUT Selection")
    private ColorTable colorTable;

    protected void lutNameChanged() {
        final MutableModuleItem<String> lutNameItem = getInfo().getMutableInput("lutName", String.class);
        try {
            colorTable = lutService.loadLUT( lutService.findLUTs().get( lutName ) );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize() {
        try {
            colorTable = lutService.loadLUT( lutService.findLUTs().get( "Red.lut" ) );
        } catch (IOException e) {
            e.printStackTrace();
        }
        final MutableModuleItem<String> lutNameItem = getInfo().getMutableInput("lutName", String.class );
        lutNameItem.setChoices( new ArrayList( lutService.findLUTs().keySet() ) );
    }

    @Override
    public void run() {
        sciView.setColormap( node, (AbstractArrayColorTable) colorTable);
    }
}

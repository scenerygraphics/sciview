/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2020 SciView developers.
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
package sc.iview.commands.help;


import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.ui.behaviour.InputTrigger;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.HELP;
import static sc.iview.commands.MenuWeights.HELP_HELP;
import static sc.iview.commands.view.NodePropertyEditor.USAGE_TEXT;

/**
 * Command to show input controls
 *
 * @author Kyle Harrington
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Help", weight = HELP), //
                 @Menu(label = "Help", weight = HELP_HELP) })
public class Help implements Command {

    @Parameter
    private SciView sciView;

    @Parameter
    private UIService uiService;

    public String getKeybinds() {
        StringBuilder helpString = new StringBuilder();
        for( InputTrigger trigger : sciView.getSceneryInputHandler().getAllBindings().keySet() ) {
            helpString.append(trigger).append("\t-\t").append(sciView.getSceneryInputHandler().getAllBindings().get(trigger)).append("\n");
        }
        return helpString.toString();
    }

    @Override
    public void run() {
        uiService.showDialog( "<html>" + USAGE_TEXT + "<br><br>" + getKeybinds() + "", "SciView Usage");
    }
}

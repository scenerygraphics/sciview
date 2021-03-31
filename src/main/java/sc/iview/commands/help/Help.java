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
package sc.iview.commands.help;


import org.scijava.command.Command;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import graphics.scenery.controls.InputHandler;
import org.scijava.ui.behaviour.io.InputTriggerConfig;
import sc.iview.SciView;

import javax.swing.*;

import static sc.iview.commands.MenuWeights.HELP;
import static sc.iview.commands.MenuWeights.HELP_HELP;

/**
 * Command to show input controls
 *
 * @author Kyle Harrington
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "Help", weight = HELP), //
                 @Menu(label = "Controls", weight = HELP_HELP) })
public class Help implements Command {

    @Parameter
    private SciView sciView;

    @Override
    public void run() {
        //opens own, not modal!, window with the help
        JFrame frame = new JFrame("SciView Input Controls Overview");
        frame.getContentPane().add( new JScrollPane( new JLabel(
                "<html>" + getBasicUsageText(sciView.publicGetInputHandler()) +
                "<br><br>" + getKeybindingsAsHtmlFrom(sciView.publicGetInputHandler()) )));
        frame.pack();
        frame.setVisible(true);
    }

    public static String getBasicUsageText(final InputHandler inputHandler) {
        //find the current key bindings
        final String lookAround = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("view: freely look around") );
        final String walkW = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("move_forward") );
        final String walkS = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("move_back") );
        final String walkA = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("move_left") );
        final String walkD = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("move_right") );
        final String walkX = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("move_down") );
        final String walkC = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("move_up") );
        final String walkMouse = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("move_withMouse_back/forward/left/right") );
        final String nodeSelect = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("node: choose one from the view panel") );
        final String arcBall = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("view: rotate around selected node") );
        final String arcBallScroll = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("view: zoom outward or toward selected node") );
        final String nodeDrag = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("node: move selected one left, right, up, or down") );
        final String nodeScroll = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("node: move selected one closer or further away") );
        final String nodeRotate = InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings("node: rotate selected one") );

        return
            lookAround+" and move mouse in the 3D view to look around. Walk freely in the scene along the current look with "+walkW+"/"+walkS+" keys,<br>" +
            "left/right-ward with "+walkA+"/"+walkD+" keys, and down/up-ward with "+walkX+"/"+walkC+" keys. "+walkMouse+" and mouse move walks the scene too.<br>" +
            "The "+walkW+","+walkA+","+walkS+","+walkD+","+walkX+","+walkC+" moves can be accelerated by holding Shift (in the default settings), or even more with Ctrl and Shift.<br><br>" +
            //
            "button1 single-click on a node in the Inspector tree selects it, while double-clicking it also centers the 3D view on it.<br>" +
            nodeSelect+" on a node in the 3D view can select it too, just choose it from the pop-up menu that would appear.<br><br>" +
            //
            "Holding "+arcBall+" while dragging mouse centers the 3D view on the selected node first, and then rotates the view<br>" +
            "around it as the mouse move. Doing "+arcBallScroll+" zooms towards and outwards the selected node.<br><br>" +
            //
            "Holding "+nodeDrag+" while dragging mouse moves the node within the scene in the screen plane.<br>" +
            "Doing "+nodeScroll+" moves the node closer or further within the scene. Finally,<br>" +
            "holding "+nodeRotate+" while dragging mouse rotates the node within the scene.<br>";
    }

    public static String getKeybindingsAsPlainTxtFrom(final InputHandler inputHandler) {
        int maxLength = 0; //to aid space-padding per item
        for ( String actionName : inputHandler.getAllBehaviours() )
            maxLength = Math.max( actionName.length(), maxLength );

        char[] rightSpacePadding = new char[maxLength];
        for (int i = 0; i < maxLength; ++i) rightSpacePadding[i] = ' ';

        final StringBuilder helpString = new StringBuilder("SciView Input Controls Overview:\n\n");
        for ( String actionName : inputHandler.getAllBehaviours() )
            helpString
                    .append( actionName )
                    .append( rightSpacePadding,0,maxLength-actionName.length() )
                    .append("\t-\t")
                    .append( InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings( actionName ) ) )
                    .append("\n");

        return helpString.toString();
    }

    public static String getKeybindingsAsHtmlFrom(final InputHandler inputHandler) {
        final StringBuilder helpString = new StringBuilder("SciView Input Controls Overview:<br>\n");
        helpString.append("<table><tr><th>Action Name</th><th>Binding</th></tr>\n");
        for ( String actionName : inputHandler.getAllBehaviours() )
            helpString
                    .append("<tr><td>")
                    .append( actionName )
                    .append("</td><td>")
                    .append( InputTriggerConfig.prettyPrintInputs( inputHandler.getKeyBindings( actionName ) ) )
                    .append("</td></tr>\n");
        helpString.append("</table>\n");

        return helpString.toString();
    }
}

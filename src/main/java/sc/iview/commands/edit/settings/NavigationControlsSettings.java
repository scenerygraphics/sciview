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
package sc.iview.commands.edit.settings;

import org.scijava.command.Command;
import org.scijava.command.InteractiveCommand;
import org.scijava.log.LogService;
import org.scijava.module.MutableModuleItem;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.prefs.PrefService;
import org.scijava.widget.Button;
import org.scijava.widget.NumberWidget;
import sc.iview.SciView;

import static sc.iview.commands.MenuWeights.*;

/**
 * A command for interactively editing step sizes and mouse sensitivity of all navigation controls.
 * @author Vladimir Ulman
 */
@Plugin(type = Command.class, initializer = "refreshWidgets", menuRoot = "SciView",
        menu = { @Menu(label = "Edit", weight = EDIT),
                 @Menu(label = "Settings", weight = EDIT_SETTINGS),
                 @Menu(label = "Controls", weight = EDIT_SETTINGS_CONTROLS) },
        label = "Input Controls Step Sizes and Mouse Sensitivities")
public class NavigationControlsSettings extends InteractiveCommand {

    //these follow SciView.setFPSSpeed()
    private static final float BASE_SPEED_INCR = 0.01f;
    private static final float FAST_TO_SLOW_RATIO = 20.0f;
    private static final float VERY_FAST_TO_SLOW_RATIO = 500.0f;
    private static final float MOUSE_MOVE_INCR = 0.02f;
    private static final float MOUSE_SCROLL_INCR = 0.3f;

    @Parameter
    private SciView sciview;

    @Parameter(label = "Small step size", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processSlowSpeed",
        description = "Normal speed camera movement.",
        min = "" + SciView.FPSSPEED_MINBOUND_SLOW, max = "" + SciView.FPSSPEED_MAXBOUND_SLOW, stepSize = "" + BASE_SPEED_INCR)
    private Float fpsSlowSpeed;

    @Parameter(label = "Large step size", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processFastSpeed",
            description = "Fast camera movement.",
            min = "" + SciView.FPSSPEED_MINBOUND_FAST, max = "" + SciView.FPSSPEED_MAXBOUND_FAST,
            stepSize = "" + (FAST_TO_SLOW_RATIO * BASE_SPEED_INCR))
    private Float fpsFastSpeed;

    @Parameter(label = "Very large step size", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processVeryFastSpeed",
            description = "Very fast camera movement.",
            min = "" + SciView.FPSSPEED_MINBOUND_VERYFAST, max = "" + SciView.FPSSPEED_MAXBOUND_VERYFAST,
            stepSize = "" + (VERY_FAST_TO_SLOW_RATIO * BASE_SPEED_INCR))
    private Float fpsVeryFastSpeed;

    @Parameter(label = "Adjust all step sizes together",
            description = "When locked (checked), all step sizes above are updated simultaneously.")
    private boolean adjustStepsLock = true;

    @Parameter(label = "Mouse move sensitivity", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processMouseMove",
            description = "Mouse movement sensitivity.",
            min = "" + SciView.MOUSESPEED_MINBOUND, max = "" + SciView.MOUSESPEED_MAXBOUND, stepSize = "" + MOUSE_MOVE_INCR)
    private Float mouseMoveSensitivity;

    @Parameter(label = "Mouse scroll sensitivity", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processMouseScroll",
            description = "Mouse scroll sensitivity.",
            min = "" + SciView.MOUSESCROLL_MINBOUND, max = "" + SciView.MOUSESCROLL_MAXBOUND, stepSize = "" + MOUSE_SCROLL_INCR)
    private Float mouseScrollSensitivity;

    private void refreshWidgets() {
        fpsSlowSpeed = sciview.getFPSSpeedSlow();
        fpsFastSpeed = sciview.getFPSSpeedFast();
        fpsVeryFastSpeed = sciview.getFPSSpeedVeryFast();
        mouseMoveSensitivity = sciview.getMouseSpeed();
        mouseScrollSensitivity = sciview.getMouseScrollSpeed();
    }

    private void processSlowSpeed()
    {
        if (adjustStepsLock) {
            sciview.setFPSSpeed(fpsSlowSpeed);
            refreshWidgets();
        }
        else
            sciview.setFPSSpeedSlow( fpsSlowSpeed );

    }
    private void processFastSpeed()
    {
        if (adjustStepsLock) {
            sciview.setFPSSpeed(fpsFastSpeed / FAST_TO_SLOW_RATIO);
            refreshWidgets();
        }
        else
            sciview.setFPSSpeedFast( fpsFastSpeed );
    }
    private void processVeryFastSpeed()
    {
        if (adjustStepsLock) {
            sciview.setFPSSpeed(fpsVeryFastSpeed / VERY_FAST_TO_SLOW_RATIO);
            refreshWidgets();
        }
        else
            sciview.setFPSSpeedVeryFast( fpsVeryFastSpeed );
    }

    private void processMouseMove()
    {
        sciview.setMouseSpeed( mouseMoveSensitivity );
    }
    private void processMouseScroll()
    {
        sciview.setMouseScrollSpeed( mouseScrollSensitivity );
    }

    @Parameter(label = "Refresh", callback = "refreshWidgets",
            description = "Click to refresh the dialog from sciview's current state -- useful when, e.g., step size is changed which keyboard shortcuts.")
    private Button refreshButton;


    @Parameter(label = "Reset", callback = "resetValues",
            description = "Click to set controls back to default values.")
    private Button resetButton;

    private void resetValues() {
        sciview.controls.resetParameters();
        refreshWidgets();
    }
}

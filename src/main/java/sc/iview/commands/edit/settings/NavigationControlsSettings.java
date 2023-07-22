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
@Plugin(type = Command.class, menuRoot = "SciView",
        menu = { @Menu(label = "Edit", weight = EDIT),
                 @Menu(label = "Settings", weight = EDIT_SETTINGS),
                 @Menu(label = "Controls", weight = EDIT_SETTINGS_CONTROLS) },
        label = "Input Controls Step Sizes and Mouse Sensitivities",
        initializer = "setupBoundsFromSciView")
public class NavigationControlsSettings extends InteractiveCommand {

    //these follow SciView.setFPSSpeed()
    private static final float BASE_SPEED_INCR = 0.01f;
    private static final float FAST_TO_SLOW_RATIO = 20.0f;
    private static final float VERY_FAST_TO_SLOW_RATIO = 500.0f;
    private static final float MOUSE_MOVE_INCR = 0.02f;
    private static final float MOUSE_SCROLL_INCR = 0.3f;

    @Parameter(required = false)
    private PrefService ps;

    @Parameter
    private SciView sciView;

    @Parameter(label = "Small step size:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processSlowSpeed",
        description = "How much of a world coordinate the camera moves with W,A,S,D keys or mouse right click&drag.",
        min = "" + SciView.FPSSPEED_MINBOUND_SLOW, max = "" + SciView.FPSSPEED_MAXBOUND_SLOW, stepSize = "" + BASE_SPEED_INCR)
    private Float fpsSlowSpeed;

    @Parameter(label = "Large step size:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processFastSpeed",
            description = "How much of a world coordinate the camera moves with shift+ W,A,S,D keys or shift+ mouse right click&drag.",
            min = "" + SciView.FPSSPEED_MINBOUND_FAST, max = "" + SciView.FPSSPEED_MAXBOUND_FAST,
            stepSize = "" + (FAST_TO_SLOW_RATIO * BASE_SPEED_INCR))
    private Float fpsFastSpeed;

    @Parameter(label = "Very large step size:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processVeryFastSpeed",
            description = "How much of a world coordinate the camera moves with ctrl+shift+ W,A,S,D keys.",
            min = "" + SciView.FPSSPEED_MINBOUND_VERYFAST, max = "" + SciView.FPSSPEED_MAXBOUND_VERYFAST,
            stepSize = "" + (VERY_FAST_TO_SLOW_RATIO * BASE_SPEED_INCR))
    private Float fpsVeryFastSpeed;

    @Parameter(label = "Adjust all step sizes together:",
            description = "When locked (checked), all step sizes above are updated simultaneously.")
    private boolean adjustStepsLock = true;

    @Parameter(label = "Mouse move sensitivity:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processMouseMove",
            description = "Influences proportionally how much of a mouse move is required for an action in SciView.",
            min = "" + SciView.MOUSESPEED_MINBOUND, max = "" + SciView.MOUSESPEED_MAXBOUND, stepSize = "" + MOUSE_MOVE_INCR)
    private Float mouseMoveSensitivity;

    @Parameter(label = "Mouse scroll sensitivity:", style = NumberWidget.SCROLL_BAR_STYLE, callback = "processMouseScroll",
            description = "Influences proportionally how much of a mouse wheel scrolling is required for an action in SciView.",
            min = "" + SciView.MOUSESCROLL_MINBOUND, max = "" + SciView.MOUSESCROLL_MAXBOUND, stepSize = "" + MOUSE_SCROLL_INCR)
    private Float mouseScrollSensitivity;

    /** Initiates GUI, all the spinners (scroll bars) */
    private void setupBoundsFromSciView() {
        //backup the current state of SciView before we eventually override it
        //so that there is something to return to with the "first toggle"
        orig_fpsSlowSpeed = sciView.getControls().getParameters().getFpsSpeedSlow();
        orig_fpsFastSpeed = sciView.getControls().getParameters().getFpsSpeedFast();
        orig_fpsVeryFastSpeed = sciView.getControls().getParameters().getFpsSpeedVeryFast();
        orig_mouseMoveSensitivity = sciView.getControls().getParameters().getMouseSpeedMult();
        orig_mouseScrollSensitivity = sciView.getControls().getParameters().getMouseScrollMult();

        //try to retrieve stored dialog state and push it to SciView
        //so that the SciView and dialog states match
        if (ps == null) return;
        sciView.setFPSSpeedSlow(     ps.getFloat( NavigationControlsSettings.class, "fpsSlowSpeed", orig_fpsSlowSpeed) );
        sciView.setFPSSpeedFast(     ps.getFloat( NavigationControlsSettings.class, "fpsFastSpeed", orig_fpsFastSpeed) );
        sciView.setFPSSpeedVeryFast( ps.getFloat( NavigationControlsSettings.class, "fpsVeryFastSpeed", orig_fpsVeryFastSpeed) );
        sciView.setMouseSpeed(       ps.getFloat( NavigationControlsSettings.class, "mouseMoveSensitivity", orig_mouseMoveSensitivity) );
        sciView.setMouseScrollSpeed( ps.getFloat( NavigationControlsSettings.class, "mouseScrollSensitivity", orig_mouseScrollSensitivity) );
    }

    //updates GUI with fresh values
    private void updateDialogSpeedsAndMouseParams() {
        fpsSlowSpeed = sciView.getControls().getParameters().getFpsSpeedSlow();
        fpsFastSpeed = sciView.getControls().getParameters().getFpsSpeedFast();
        fpsVeryFastSpeed = sciView.getControls().getParameters().getFpsSpeedVeryFast();
        mouseMoveSensitivity = sciView.getControls().getParameters().getMouseSpeedMult();
        mouseScrollSensitivity = sciView.getControls().getParameters().getMouseScrollMult();
    }


    private void processSlowSpeed()
    {
        if (!adjustStepsLock)
            sciView.setFPSSpeedSlow( fpsSlowSpeed );
        else
            sciView.setFPSSpeed( fpsSlowSpeed );
        updateDialogSpeedsAndMouseParams();
    }
    private void processFastSpeed()
    {
        if (!adjustStepsLock)
            sciView.setFPSSpeedFast( fpsFastSpeed );
        else
            sciView.setFPSSpeed( fpsFastSpeed / FAST_TO_SLOW_RATIO );
        updateDialogSpeedsAndMouseParams();
    }
    private void processVeryFastSpeed()
    {
        if (!adjustStepsLock)
            sciView.setFPSSpeedVeryFast( fpsVeryFastSpeed );
        else
            sciView.setFPSSpeed( fpsVeryFastSpeed / VERY_FAST_TO_SLOW_RATIO );
        updateDialogSpeedsAndMouseParams();
    }

    private void processMouseMove()
    {
        sciView.setMouseSpeed( mouseMoveSensitivity );
        //updateDialogSpeedsAndMouseParams();
    }
    private void processMouseScroll()
    {
        sciView.setMouseScrollSpeed( mouseScrollSensitivity );
        //updateDialogSpeedsAndMouseParams();
    }


    @Parameter(label = "Click to re-read current state:", callback = "refreshDialog",
            description = "Changing its state triggers the dialog update -- useful when, e.g., step size is changed which keyboard shortcuts.")
    private Button refreshButton;

    private boolean firstHitOfRefreshButton = true; //the "first toggle" flag

    private float orig_fpsSlowSpeed, orig_fpsFastSpeed, orig_fpsVeryFastSpeed;
    private float orig_mouseMoveSensitivity, orig_mouseScrollSensitivity;

    private void refreshDialog()
    {
        //only the "first toggle" will restore values in SciView
        //as they were at the time this dialog started,
        if (firstHitOfRefreshButton) {
            sciView.setFPSSpeedSlow( orig_fpsSlowSpeed );
            sciView.setFPSSpeedFast( orig_fpsFastSpeed );
            sciView.setFPSSpeedVeryFast( orig_fpsVeryFastSpeed );
            sciView.setMouseSpeed( orig_mouseMoveSensitivity );
            sciView.setMouseScrollSpeed( orig_mouseScrollSensitivity );
            firstHitOfRefreshButton = false;
        }

        //in any case, update the dialog to the current state of SciView
        updateDialogSpeedsAndMouseParams();
    }
}

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
package sc.iview.controls.behaviours;

import graphics.scenery.controls.behaviours.ArcballCameraControl;
import graphics.scenery.controls.behaviours.FPSCameraControl;
import graphics.scenery.controls.behaviours.MovementCommand;
import java.util.List;
import java.util.LinkedList;

/**
 * A container to hold movement step sizes and mouse sensitivity parameters,
 * these shall not be duplicated elsewhere in SciView. It serves three purposes:
 *
 * 1) hold and manage the said parameters
 * 2) provide their current values (mostly to SciView's internal objects)
 * 3) updates registered (scenery's) services immediately whenever any parameter changes
 *
 * Inside-package controls, such as NodeTranslateControl, read out this whenever they are
 * invoked to get current state. Outside-package controls, currently only MovementCommand,
 * ArcballCameraControl, and FPSCameraControl, that have own memory of the same-purposed
 * attributes are synchronized here as long as they are registered here. This way, caller
 * needs to adjusts e.g. mouse movement sensitivity only here, and not explicitly at all
 * relevant controls.
 *
 * @author Vladimir Ulman
 */
public class ControlsParameters
{
    public ControlsParameters()
    {
        this(null,null);
    }

    /**
     * One can optionally register scenery's ArcballCameraControl and FPSCameraControl to be
     * notified of changes. Any of the params may be null.
     */
    public ControlsParameters(final ArcballCameraControl arcballCamCntrl, final FPSCameraControl fpsCamCntrl)
    {
        registerArcballCameraControl(arcballCamCntrl);
        registerFpsCameraControl(fpsCamCntrl);
    }

    // ---------------------- controls from scenery ----------------------
    private ArcballCameraControl arcballCameraControl = null;
    private FPSCameraControl fpsCameraControl = null;

    /**
     * Registers the scenery's ArcballCameraControl to be synchronized with this module.
     * Provide null to unregister, to disable the synchronization.
     */
    public void registerArcballCameraControl(final ArcballCameraControl arcballCamCntrl) {
        arcballCameraControl = arcballCamCntrl;

        //synchronize with the current state of the controls
        if (arcballCameraControl != null) {
            arcballCameraControl.setMouseSpeedMultiplier(mouseSpeedMult);
            arcballCameraControl.setScrollSpeedMultiplier(fpsSpeedSlow * mouseScrollMult);
        }
    }

    public void unregisterArcballCameraControl() {
        arcballCameraControl = null;
    }


    /**
     * Registers the scenery's FPSCameraControl to be synchronized with this module.
     * Provide null to unregister, to disable the synchronization.
     */
    public void registerFpsCameraControl(final FPSCameraControl fpsCamCntrl) {
        fpsCameraControl = fpsCamCntrl;

        //synchronize with the current state of the controls
        if (fpsCameraControl != null) {
            fpsCameraControl.setMouseSpeedMultiplier(mouseSpeedMult);
        }
    }

    public void unregisterFpsCameraControl() {
        fpsCameraControl = null;
    }


    private final List<MovementCommand> slowStepMovers = new LinkedList<>();
    private final List<MovementCommand> fastStepMovers = new LinkedList<>();
    private final List<MovementCommand> veryFastStepMovers = new LinkedList<>();

    public void registerSlowStepMover(final MovementCommand movementCommand) {
        slowStepMovers.add( movementCommand );
    }
    public void registerFastStepMover(final MovementCommand movementCommand) {
        fastStepMovers.add( movementCommand );
    }
    public void registerVeryFastStepMover(final MovementCommand movementCommand) {
        veryFastStepMovers.add( movementCommand );
    }

    public void unregisterSlowStepMover(final MovementCommand movementCommand) {
        slowStepMovers.remove( movementCommand );
    }
    public void unregisterFastStepMover(final MovementCommand movementCommand) {
        fastStepMovers.remove( movementCommand );
    }
    public void unregisterVeryFastStepMover(final MovementCommand movementCommand) {
        veryFastStepMovers.remove( movementCommand );
    }

    // ---------------------- the movement controls in sciview ----------------------
    /** Speeds for input controls: normal step size. */
    private float fpsSpeedSlow = 0.05f;

    /** Speeds for input controls: big step size */
    private float fpsSpeedFast = 1.0f;

    /** Speeds for input controls: very big step size */
    private float fpsSpeedVeryFast = 25.0f;

    /** Speeds for mouse move controls: higher means more sensitive to mouse movement */
    private float mouseSpeedMult = 0.25f;

    /** Speeds for mouse scroll controls: higher means more sensitive to a roll of the scroll wheel */
    private float mouseScrollMult = 2.5f;

    // ---------------------- setters ----------------------
    public void setFpsSpeedSlow(float fpsSpeedSlow) {
        this.fpsSpeedSlow = fpsSpeedSlow;
        slowStepMovers.forEach( m -> m.setSpeed( fpsSpeedSlow ) );

        //notify the scenery controls
        if (arcballCameraControl != null) {
            arcballCameraControl.setScrollSpeedMultiplier(fpsSpeedSlow * mouseScrollMult);
        }
    }

    public void setFpsSpeedFast(float fpsSpeedFast) {
        this.fpsSpeedFast = fpsSpeedFast;
        fastStepMovers.forEach( m -> m.setSpeed( fpsSpeedFast ) );
    }

    public void setFpsSpeedVeryFast(float fpsSpeedVeryFast) {
        this.fpsSpeedVeryFast = fpsSpeedVeryFast;
        veryFastStepMovers.forEach( m -> m.setSpeed( fpsSpeedVeryFast ) );
    }

    public void setMouseSpeedMult(float mouseSpeedMult) {
        this.mouseSpeedMult = mouseSpeedMult;

        //notify the scenery controls
        if (arcballCameraControl != null) {
            arcballCameraControl.setMouseSpeedMultiplier(mouseSpeedMult);
        }
        if (fpsCameraControl != null) {
            fpsCameraControl.setMouseSpeedMultiplier(mouseSpeedMult);
        }
    }

    public void setMouseScrollMult(float mouseScrollMult) {
        this.mouseScrollMult = mouseScrollMult;

        //notify the scenery controls
        if (arcballCameraControl != null) {
            arcballCameraControl.setScrollSpeedMultiplier(fpsSpeedSlow * mouseScrollMult);
        }
    }

    // ---------------------- modifiers ----------------------

    // ---------------------- getters ----------------------
    public float getFpsSpeedSlow() {
        return fpsSpeedSlow;
    }

    public float getFpsSpeedFast() {
        return fpsSpeedFast;
    }

    public float getFpsSpeedVeryFast() {
        return fpsSpeedVeryFast;
    }

    public float getMouseSpeedMult() {
        return mouseSpeedMult;
    }

    public float getMouseScrollMult() {
        return mouseScrollMult;
    }
}

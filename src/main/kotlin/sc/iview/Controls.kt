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
package sc.iview

import graphics.scenery.*
import graphics.scenery.primitives.Line
import graphics.scenery.Node
import graphics.scenery.controls.behaviours.FPSCameraControl
import graphics.scenery.controls.behaviours.MovementCommand
import graphics.scenery.controls.behaviours.SelectCommand
import graphics.scenery.primitives.TextBoard
import graphics.scenery.utils.lazyLogger
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import org.scijava.command.CommandService
import org.scijava.ui.behaviour.Behaviour
import org.scijava.ui.behaviour.ClickBehaviour
import sc.iview.commands.help.Help
import sc.iview.controls.behaviours.*
import sc.iview.controls.behaviours.Ruler
import java.io.File
import java.util.*
import java.util.function.Supplier
import kotlin.math.acos

/**
 * Class for everything control-related in sciview.
 *
 * @author Vladimir Ulman
 * @author Ulrik Guenther
 */
open class Controls(val sciview: SciView) {
    private val logger by lazyLogger()

    companion object {
        const val STASH_BEHAVIOUR_KEY = "behaviour:"
        const val STASH_BINDING_KEY = "binding:"
        const val STASH_CONTROLSPARAMS_KEY = "parameters:"
    }

    private val inputHandler
        get() = sciview.sceneryInputHandler!!

    /**
     * Speeds for input controls
     */
    var parameters: ControlsParameters = ControlsParameters()

    lateinit var targetArcball: AnimatedCenteringBeforeArcBallControl
        protected set
    
    protected var controlStack: Stack<HashMap<String, Any>> = Stack()

    /**
     * Mouse controls for FPS movement and Arcball rotation
     */
    protected var fpsControl: FPSCameraControl? = null

    var objectSelectionLastResult: Scene.RaycastResult? = null
        protected set

    /**
     * This pushes the current input setup onto a stack that allows them to be restored with restoreControls
     * This pushes the current input setup onto a stack that allows them to be restored with restoreControls.
     * It stacks in particular: all keybindings, all Behaviours, and all step sizes and mouse sensitivities
     * (which are held together in [parameters]).
     *
     * *Word of warning:* The stashing memorizes *references only* on currently used controls
     * (such as, e.g., [MovementCommand], [FPSCameraControl] or [NodeTranslateControl]),
     * it *does not* create an extra copy of any control. That said, if you modify any control
     * object despite it was already stashed with this method, the change will be visible in the "stored"
     * control and will not go away after the restore... To be on the safe side for now at least, *create
     * new and modified* controls rather than directly changing them.
     */
    fun stashControls() {
        val controlState = HashMap<String, Any>()
        val handler = inputHandler

        //behaviours:
        for (actionName in handler.getAllBehaviours()) {
            controlState[STASH_BEHAVIOUR_KEY + actionName] = handler.getBehaviour(actionName) as Any
        }

        //bindings:
        for (actionName in handler.getAllBehaviours()) {
            for (trigger in handler.getKeyBindings(actionName)) {
                controlState[STASH_BINDING_KEY + actionName] = trigger.toString()
            }
        }

        //finally, stash the control parameters
        controlState[STASH_CONTROLSPARAMS_KEY] = parameters

        //...and stash it!
        controlStack.push(controlState)
    }

    fun restoreControls() {
        if (controlStack.empty()) {
            logger.warn("Not restoring controls, the controls stash stack is empty!")
            return
        }

        //clear the input handler entirely
        for (actionName in inputHandler.getAllBehaviours()) {
            inputHandler.removeKeyBinding(actionName)
            inputHandler.removeBehaviour(actionName)
        }

        //retrieve the most recent stash with controls
        val controlState = controlStack.pop()
        for (control in controlState.entries) {
            var key: String
            when {
                control.key.startsWith(STASH_BEHAVIOUR_KEY) -> {
                    //processing behaviour
                    key = control.key.substring(STASH_BEHAVIOUR_KEY.length)
                    inputHandler.addBehaviour(key, control.value as Behaviour)
                }
                control.key.startsWith(STASH_BINDING_KEY) -> {
                    //processing key binding
                    key = control.key.substring(STASH_BINDING_KEY.length)
                    inputHandler.addKeyBinding(key, (control.value as String))
                }
                control.key.startsWith(STASH_CONTROLSPARAMS_KEY) -> {
                    //processing mouse sensitivities and step sizes...
                    parameters = control.value as ControlsParameters 
                }
            }
        }
    }

    /*
     * Initial configuration of the scenery InputHandler
     * This is automatically called and should not be used directly
     */
    fun inputSetup() {
        val h = inputHandler
        //when we get here, the Behaviours and key bindings from scenery are already in place

        //possibly, disable some (unused?) controls from scenery
        /*
        h.removeBehaviour( "gamepad_camera_control");
        h.removeKeyBinding("gamepad_camera_control");
        h.removeBehaviour( "gamepad_movement_control");
        h.removeKeyBinding("gamepad_movement_control");
        */

        // node-selection and node-manipulation (translate & rotate) controls
        setObjectSelectionMode()
        setDistanceMeasurer()
        val nodeTranslateControl = NodeTranslateControl(sciview)
        h.addBehaviour("node: move selected one left, right, up, or down", nodeTranslateControl)
        h.addKeyBinding("node: move selected one left, right, up, or down", "ctrl button1")
        h.addBehaviour("node: move selected one closer or further away", nodeTranslateControl)
        h.addKeyBinding("node: move selected one closer or further away", "ctrl scroll")
        h.addBehaviour("node: rotate selected one", NodeRotateControl(sciview))
        h.addKeyBinding("node: rotate selected one", "ctrl shift button1")
        h.addBehaviour("node: delete selected one", ClickBehaviour { _, _ -> sciview.deleteActiveNode(true) })
        h.addKeyBinding("node: delete selected one", "DELETE")


        // within-scene navigation: ArcBall and FPS
        enableArcBallControl()
        enableFPSControl()

        // whole-scene rolling
        h.addBehaviour("view: rotate (roll) clock-wise", SceneRollControl(sciview, +0.05f)) //2.8 deg
        h.addKeyBinding("view: rotate (roll) clock-wise", "R")
        h.addBehaviour("view: rotate (roll) counter clock-wise", SceneRollControl(sciview, -0.05f))
        h.addKeyBinding("view: rotate (roll) counter clock-wise", "shift R")
        h.addBehaviour("view: rotate (roll) with mouse", h.getBehaviour("view: rotate (roll) clock-wise")!!)
        h.addKeyBinding("view: rotate (roll) with mouse", "ctrl button3")

        // adjusters of various controls sensitivities
        h.addBehaviour("moves: step size decrease", ClickBehaviour { _: Int, _: Int -> setFPSSpeed(getFPSSpeedSlow() - 0.01f) })
        h.addKeyBinding("moves: step size decrease", "MINUS")
        h.addBehaviour("moves: step size increase", ClickBehaviour { _: Int, _: Int -> setFPSSpeed(getFPSSpeedSlow() + 0.01f) })
        h.addKeyBinding("moves: step size increase", "EQUALS")
        h.addBehaviour("mouse: move sensitivity decrease", ClickBehaviour { _: Int, _: Int -> setMouseSpeed(getMouseSpeed() - 0.02f) })
        h.addKeyBinding("mouse: move sensitivity decrease", "M MINUS")
        h.addBehaviour("mouse: move sensitivity increase", ClickBehaviour { _: Int, _: Int -> setMouseSpeed(getMouseSpeed() + 0.02f) })
        h.addKeyBinding("mouse: move sensitivity increase", "M EQUALS")
        h.addBehaviour("mouse: scroll sensitivity decrease", ClickBehaviour { _: Int, _: Int -> setMouseScrollSpeed(getMouseScrollSpeed() - 0.3f) })
        h.addKeyBinding("mouse: scroll sensitivity decrease", "S MINUS")
        h.addBehaviour("mouse: scroll sensitivity increase", ClickBehaviour { _: Int, _: Int -> setMouseScrollSpeed(getMouseScrollSpeed() + 0.3f) })
        h.addKeyBinding("mouse: scroll sensitivity increase", "S EQUALS")

        // help window
        h.addBehaviour("show help", ClickBehaviour { _: Int, _: Int ->  sciview.scijavaContext?.getService(CommandService::class.java)?.run(Help::class.java, true) })
        h.addKeyBinding("show help", "F1")

        //update scene inspector
        h.addBehaviour("update scene graph", ClickBehaviour { _: Int, _: Int -> sciview.requestPropEditorRefresh() })
        h.addKeyBinding("update scene graph", "shift ctrl I")

        //ruler
        val ruler = Ruler(sciview)
        h.addBehaviour("ruler: keep the button pressed and drag with the mouse", ruler)
        h.addKeyBinding("ruler: keep the button pressed and drag with the mouse", "E")

        val configFile = File(sciview.getProjectDirectories().configDir, ".keybindings.yaml")
        if( configFile.exists() )
            inputHandler.readFromFile(configFile)
        else
            inputHandler.useDefaultBindings(configFile.absolutePath)
    }

    /*
     * Change the control mode to circle around the active object in an arcball
     */
    private fun enableArcBallControl() {
        val h = inputHandler

        val target: Vector3f = sciview.activeNode?.position ?: Vector3f(0.0f, 0.0f, 0.0f)

        //setup ArcballCameraControl from scenery, register it with SciView's parameters
        val cameraSupplier = { sciview.currentScene.findObserver() }
        val initAction = { _: Int, _: Int -> }
        val scrollAction = { _: Double, _: Boolean, _: Int, _:Int -> }
        
        targetArcball = AnimatedCenteringBeforeArcBallControl(
                initAction,
                scrollAction,
                "view: rotate it around selected node",
                cameraSupplier,
                sciview.getSceneryRenderer()!!.window.width,
                sciview.getSceneryRenderer()!!.window.height,
                { target }
        )
        targetArcball.maximumDistance = Float.MAX_VALUE
        parameters.registerArcballCameraControl(targetArcball)
        h.addBehaviour("view: rotate around selected node", targetArcball)
        h.addKeyBinding("view: rotate around selected node", "shift button1")
        h.addBehaviour("view: zoom outward or toward selected node", targetArcball)
        h.addKeyBinding("view: zoom outward or toward selected node", "shift scroll")
    }
    
    fun enableFPSControl() {
        val h = inputHandler

        // Mouse look around (Lclick) and move around (Rclick)
        //
        //setup FPSCameraControl from scenery, register it with SciView's parameters
        val cameraSupplier = Supplier { sciview.currentScene.findObserver() }
        fpsControl = FPSCameraControl(
                "view: freely look around",
                cameraSupplier,
                sciview.getSceneryRenderer()!!.window.width,
                sciview.getSceneryRenderer()!!.window.height
        )
        
        parameters.registerFpsCameraControl(fpsControl)
        val selectCommand = h.getBehaviour("node: choose one from the view panel") as? ClickBehaviour
        val wrappedFpsControl = selectCommand?.let {
            ClickAndDragWrapper(it, fpsControl!!)
        } ?: fpsControl!!
        h.addBehaviour("view: freely look around", wrappedFpsControl)
        h.addKeyBinding("view: freely look around", "button1")

        //slow and fast camera motion
        h.addBehaviour("move_withMouse_back/forward/left/right", CameraTranslateControl(sciview, 1f))
        h.addKeyBinding("move_withMouse_back/forward/left/right", "button3")
        //
        //fast and very fast camera motion
        h.addBehaviour("move_withMouse_back/forward/left/right_fast", CameraTranslateControl(sciview, 10f))
        h.addKeyBinding("move_withMouse_back/forward/left/right_fast", "shift button3")

        // Keyboard move around (WASD keys)
        //
        //override 'WASD' from Scenery
        var mcW: MovementCommand
        var mcA: MovementCommand
        var mcS: MovementCommand
        var mcD: MovementCommand
        mcW = MovementCommand("forward", { sciview.currentScene.findObserver() }, parameters.fpsSpeedSlow)
        mcS = MovementCommand("back", { sciview.currentScene.findObserver() }, parameters.fpsSpeedSlow)
        mcA = MovementCommand("left", { sciview.currentScene.findObserver() }, parameters.fpsSpeedSlow)
        mcD = MovementCommand("right", { sciview.currentScene.findObserver() }, parameters.fpsSpeedSlow)
        parameters.registerSlowStepMover(mcW)
        parameters.registerSlowStepMover(mcS)
        parameters.registerSlowStepMover(mcA)
        parameters.registerSlowStepMover(mcD)
        h.addBehaviour("move_forward", mcW)
        h.addBehaviour("move_back", mcS)
        h.addBehaviour("move_left", mcA)
        h.addBehaviour("move_right", mcD)
        // 'WASD' keys are registered already in scenery

        //override shift+'WASD' from Scenery
        mcW = MovementCommand("forward", { sciview.currentScene.findObserver() }, parameters.fpsSpeedFast)
        mcS = MovementCommand("back", { sciview.currentScene.findObserver() }, parameters.fpsSpeedFast)
        mcA = MovementCommand("left", { sciview.currentScene.findObserver() }, parameters.fpsSpeedFast)
        mcD = MovementCommand("right", { sciview.currentScene.findObserver() }, parameters.fpsSpeedFast)
        parameters.registerFastStepMover(mcW)
        parameters.registerFastStepMover(mcS)
        parameters.registerFastStepMover(mcA)
        parameters.registerFastStepMover(mcD)
        h.addBehaviour("move_forward_fast", mcW)
        h.addBehaviour("move_back_fast", mcS)
        h.addBehaviour("move_left_fast", mcA)
        h.addBehaviour("move_right_fast", mcD)
        // shift+'WASD' keys are registered already in scenery

        //define additionally shift+ctrl+'WASD'
        mcW = MovementCommand("forward", { sciview.currentScene.findObserver() }, parameters.fpsSpeedVeryFast)
        mcS = MovementCommand("back", { sciview.currentScene.findObserver() }, parameters.fpsSpeedVeryFast)
        mcA = MovementCommand("left", { sciview.currentScene.findObserver() }, parameters.fpsSpeedVeryFast)
        mcD = MovementCommand("right", { sciview.currentScene.findObserver() }, parameters.fpsSpeedVeryFast)
        parameters.registerVeryFastStepMover(mcW)
        parameters.registerVeryFastStepMover(mcS)
        parameters.registerVeryFastStepMover(mcA)
        parameters.registerVeryFastStepMover(mcD)
        h.addBehaviour("move_forward_veryfast", mcW)
        h.addBehaviour("move_back_veryfast", mcS)
        h.addBehaviour("move_left_veryfast", mcA)
        h.addBehaviour("move_right_veryfast", mcD)
        h.addKeyBinding("move_forward_veryfast", "ctrl shift W")
        h.addKeyBinding("move_back_veryfast", "ctrl shift S")
        h.addKeyBinding("move_left_veryfast", "ctrl shift A")
        h.addKeyBinding("move_right_veryfast", "ctrl shift D")

        // Keyboard only move up/down (XC keys)
        //
        //[[ctrl]+shift]+'XC'
        mcW = MovementCommand("up", { sciview.currentScene.findObserver() }, parameters.fpsSpeedSlow)
        mcS = MovementCommand("down", { sciview.currentScene.findObserver() }, parameters.fpsSpeedSlow)
        parameters.registerSlowStepMover(mcW)
        parameters.registerSlowStepMover(mcS)
        h.addBehaviour("move_up", mcW)
        h.addBehaviour("move_down", mcS)
        h.addKeyBinding("move_up", "C")
        h.addKeyBinding("move_down", "X")
        mcW = MovementCommand("up", { sciview.currentScene.findObserver() }, parameters.fpsSpeedFast)
        mcS = MovementCommand("down", { sciview.currentScene.findObserver() }, parameters.fpsSpeedFast)
        parameters.registerFastStepMover(mcW)
        parameters.registerFastStepMover(mcS)
        h.addBehaviour("move_up_fast", mcW)
        h.addBehaviour("move_down_fast", mcS)
        h.addKeyBinding("move_up_fast", "shift C")
        h.addKeyBinding("move_down_fast", "shift X")
        mcW = MovementCommand("up", { sciview.currentScene.findObserver() }, parameters.fpsSpeedVeryFast)
        mcS = MovementCommand("down", { sciview.currentScene.findObserver() }, parameters.fpsSpeedVeryFast)
        parameters.registerVeryFastStepMover(mcW)
        parameters.registerVeryFastStepMover(mcS)
        h.addBehaviour("move_up_veryfast", mcW)
        h.addBehaviour("move_down_veryfast", mcS)
        h.addKeyBinding("move_up_veryfast", "ctrl shift C")
        h.addKeyBinding("move_down_veryfast", "ctrl shift X")
    }

    fun getFPSSpeedSlow(): Float {
        return parameters.fpsSpeedSlow
    }

    fun getFPSSpeedFast(): Float {
        return parameters.fpsSpeedFast
    }

    fun getFPSSpeedVeryFast(): Float {
        return parameters.fpsSpeedVeryFast
    }

    fun getMouseSpeed(): Float {
        return parameters.mouseSpeedMult
    }

    fun getMouseScrollSpeed(): Float {
        return parameters.mouseScrollMult
    }

    //a couple of setters with scene sensible boundary checks
    fun setFPSSpeedSlow(slowSpeed: Float) {
        parameters.fpsSpeedSlow = slowSpeed.coerceIn(SciView.FPSSPEED_MINBOUND_SLOW, SciView.FPSSPEED_MAXBOUND_SLOW)
    }

    fun setFPSSpeedFast(fastSpeed: Float) {
        parameters.fpsSpeedFast = fastSpeed.coerceIn(SciView.FPSSPEED_MINBOUND_FAST, SciView.FPSSPEED_MAXBOUND_FAST)
    }

    fun setFPSSpeedVeryFast(veryFastSpeed: Float) {
        parameters.fpsSpeedVeryFast = veryFastSpeed.coerceIn(SciView.FPSSPEED_MINBOUND_VERYFAST, SciView.FPSSPEED_MAXBOUND_VERYFAST)
    }

    fun setFPSSpeed(newBaseSpeed: Float) {
        // we don't want to escape bounds checking
        // (so we call "our" methods rather than directly the parameters)
        setFPSSpeedSlow(1f * newBaseSpeed)
        setFPSSpeedFast(20f * newBaseSpeed)
        setFPSSpeedVeryFast(500f * newBaseSpeed)

        //report what's been set in the end
        logger.debug("FPS speeds: slow=" + parameters.fpsSpeedSlow
                .toString() + ", fast=" + parameters.fpsSpeedFast
                .toString() + ", very fast=" + parameters.fpsSpeedVeryFast)
    }

    fun setMouseSpeed(newSpeed: Float) {
        parameters.mouseSpeedMult = newSpeed.coerceIn(SciView.MOUSESPEED_MINBOUND, SciView.MOUSESPEED_MAXBOUND)
        logger.debug("Mouse movement speed: " + parameters.mouseSpeedMult)
    }

    fun setMouseScrollSpeed(newSpeed: Float) {
        parameters.mouseScrollMult = newSpeed.coerceIn(SciView.MOUSESCROLL_MINBOUND, SciView.MOUSESCROLL_MAXBOUND)
        logger.debug("Mouse scroll speed: " + parameters.mouseScrollMult)
    }
    
    fun setObjectSelectionMode() {
        val selectAction = { nearest: Scene.RaycastResult, x: Int, y: Int ->
            if (nearest.matches.isNotEmpty()) {
                // copy reference on the last object picking result into "public domain"
                // (this must happen before the ContextPopUpNodeChooser menu!)
                objectSelectionLastResult = nearest

                // Setup the context menu for this picking
                // (in the menu, the user will chose node herself)
                sciview.showContextNodeChooser(x, y)
            }
        }
        setObjectSelectionMode(selectAction)
    }

    fun setObjectSelectionMode(selectAction: Function3<Scene.RaycastResult, Int, Int, Unit>?) {
        val h = inputHandler
        val ignoredObjects = ArrayList<Class<*>>()
        ignoredObjects.add(BoundingGrid::class.java)
        ignoredObjects.add(Camera::class.java) //do not mess with "scene params", allow only "scene data" to be selected
        ignoredObjects.add(DetachedHeadCamera::class.java)
        ignoredObjects.add(DirectionalLight::class.java)
        ignoredObjects.add(PointLight::class.java)
        
        h.addBehaviour("node: choose one from the view panel",
                SelectCommand("objectSelector", sciview.getSceneryRenderer()!!, sciview.currentScene,
                        { sciview.currentScene.findObserver() }, false, ignoredObjects,
                        selectAction!!))
        h.addKeyBinding("node: choose one from the view panel", "double-click button1")
    }

    private var secondNode = false

    fun setDistanceMeasurer() {
        var lastNode: Node? = null
        val distanceAction = { nearest: Scene.RaycastResult, x: Int, y: Int ->
            if (nearest.matches.isNotEmpty()) {
                // copy reference on the last object picking result into "public domain"
                // (this must happen before the ContextPopUpNodeChooser menu!)
                objectSelectionLastResult = nearest

                // Setup the context menu for this picking
                // (in the menu, the user will chose node herself)
                sciview.showContextNodeChooser(x, y)
            }
            val line = Line(simple = true)
            if (!secondNode) {
                if(sciview.activeNode != null) {
                    lastNode = sciview.activeNode!!
                }
            } else {
                if (sciview.activeNode != null) {
                    sciview.allSceneNodes.forEach {
                        if(it.name == "DistanceMeasureTextBoard" || it.name == "distanceMeasureLine") {
                            sciview.deleteNode(it)
                        }
                    }
                    val position0 = lastNode!!.spatialOrNull()!!.position
                    val position1 = sciview.activeNode!!.spatialOrNull()!!.position
                    val lastToPresent = Vector3f()
                    position1.sub(position0, lastToPresent)
                    line.addPoint(position0)
                    line.addPoint(position1)
                    line.name = "distanceMeasureLine"
                    sciview.addNode(line)
                    val distance = lastToPresent.length()
                    val board = TextBoard()
                    board.text = "Distance: $distance"
                    board.name = "DistanceMeasureTextBoard"
                    board.transparent = 0
                    board.fontColor = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
                    board.backgroundColor = Vector4f(1f, 1f, 1f, 1.0f)
                    val boardPosition = Vector3f()
                    position0.add(position1, boardPosition)
                    board.spatial().position = boardPosition.mul(0.5f)
                    if(distance < 5f) {
                        board.spatial().scale = Vector3f(0.5f, 0.5f, 0.5f)
                    }
                    else {
                        board.spatial().scale = Vector3f(distance/10f, distance/10f, distance/10f)
                    }
                    sciview.addNode(board)
                }
            }
            secondNode = !secondNode
        }
        setDistanceMeasurer(distanceAction)
    }

    fun setDistanceMeasurer(selectAction: Function3<Scene.RaycastResult, Int, Int, Unit>?) {
        val h = inputHandler
        val ignoredObjects = ArrayList<Class<*>>()
        ignoredObjects.add(BoundingGrid::class.java)
        ignoredObjects.add(Camera::class.java) //do not mess with "scene params", allow only "scene data" to be selected
        ignoredObjects.add(DetachedHeadCamera::class.java)
        ignoredObjects.add(DirectionalLight::class.java)
        ignoredObjects.add(PointLight::class.java)

        h.addBehaviour("distanceMeasurement: click, choose first node, click again and choose second node",
                SelectCommand("DistanceMeasurer", sciview.getSceneryRenderer()!!, sciview.currentScene,
                        { sciview.currentScene.findObserver() }, false, ignoredObjects,
                        selectAction!!))
        h.addKeyBinding("distanceMeasurement: click, choose first node, click again and choose second node",
                "M")
    }

    fun centerOnPosition(currentPos: Vector3f?) {
        val camera = sciview.camera ?: return

        //desired view direction in world coords
        val worldDirVec = Vector3f(currentPos).sub(camera.position)
        if (worldDirVec.lengthSquared() < 0.01) {
            //ill defined task, happens typically when cam is inside the node which we want center on
            logger.info("Camera is on the spot you want to look at. Please, move the camera away first.")
            return
        }
        val camForwardXZ = Vector2f(camera.forward.x, camera.forward.z)
        val wantLookAtXZ = Vector2f(worldDirVec.x, worldDirVec.z)
        var totalYawAng = camForwardXZ.normalize().dot(wantLookAtXZ.normalize()).toDouble()
        //while mathematically impossible, cumulated numerical inaccuracies have different opinion
        totalYawAng = if (totalYawAng > 1) {
            0.0
        } else {
            acos(totalYawAng)
        }

        //switch direction?
        camForwardXZ[camForwardXZ.y] = -camForwardXZ.x
        if (wantLookAtXZ.dot(camForwardXZ) > 0) totalYawAng *= -1.0
        val camForwardYed = Vector3f(camera.forward)
        Quaternionf().rotateXYZ(0f, (-totalYawAng).toFloat(), 0f).normalize().transform(camForwardYed)
        var totalPitchAng = camForwardYed.normalize().dot(worldDirVec.normalize()).toDouble()
        totalPitchAng = if (totalPitchAng > 1) {
            0.0
        } else {
            acos(totalPitchAng)
        }

        //switch direction?
        if (camera.forward.y > worldDirVec.y) totalPitchAng *= -1.0
        if (camera.up.y < 0) totalPitchAng *= -1.0

        //animation options: control delay between animation frames -- fluency
        val rotPausePerStep: Long = 30 //miliseconds

        //animation options: control max number of steps -- upper limit on total time for animation
        val rotMaxSteps = 999999 //effectively disabled....

        //animation options: the hardcoded 5 deg (0.087 rad) -- smoothness
        //how many steps when max update/move is 5 deg
        val totalDeltaAng = Math.max(Math.abs(totalPitchAng), Math.abs(totalYawAng)).toFloat()
        var rotSteps = Math.ceil(totalDeltaAng / 0.087).toInt()
        if (rotSteps > rotMaxSteps) rotSteps = rotMaxSteps

        /*
        logger.info("centering over "+rotSteps+" steps the pitch " + 180*totalPitchAng/Math.PI
                + " and the yaw " + 180*totalYawAng/Math.PI);
        */

        //angular progress aux variables
        var donePitchAng = 0.0
        var doneYawAng = 0.0
        var deltaAng: Float
        camera.targeted = false
        var i = 1
        while (i <= rotSteps) {

            //this emulates ease-in ease-out animation, both vars are in [0:1]
            var timeProgress = i.toFloat() / rotSteps
            val angProgress = (if (2.let { timeProgress *= it; timeProgress } <= 1) //two cubics connected smoothly into S-shape curve from [0,0] to [1,1]
                timeProgress * timeProgress * timeProgress else 2.let { timeProgress -= it; timeProgress } * timeProgress * timeProgress + 2) / 2

            //rotate now by this ang: "where should I be by now" minus "where I got last time"
            deltaAng = (angProgress * totalPitchAng - donePitchAng).toFloat()
            val pitchQ = Quaternionf().rotateXYZ(-deltaAng, 0f, 0f).normalize()
            deltaAng = (angProgress * totalYawAng - doneYawAng).toFloat()
            val yawQ = Quaternionf().rotateXYZ(0f, deltaAng, 0f).normalize()
            camera.rotation = pitchQ.mul(camera.rotation).mul(yawQ).normalize()
            donePitchAng = angProgress * totalPitchAng
            doneYawAng = angProgress * totalYawAng
            try {
                Thread.sleep(rotPausePerStep)
            } catch (e: InterruptedException) {
                i = rotSteps
            }
            ++i
        }
    }
}

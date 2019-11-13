package sc.iview.ui;

import cleargl.GLVector;
import graphics.scenery.Camera;
import graphics.scenery.Scene;
import graphics.scenery.backends.Renderer;
import graphics.scenery.controls.behaviours.SelectCommand;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function5;
import org.jetbrains.annotations.NotNull;
import org.scijava.ui.behaviour.ClickBehaviour;

import java.util.List;

public class ContextSelectCommand implements ClickBehaviour {
    private final Function5<Integer, Integer, GLVector, GLVector, ? super List<Scene.RaycastResult>, Unit> action;
    private Function0<? extends Camera> getCamera;
    private List<? extends Class<?>> ignoredObjects;
    private boolean debugRaycast = false;

    public ContextSelectCommand(@NotNull String name, @NotNull Renderer renderer, @NotNull Scene scene, @NotNull Function0<? extends Camera> camera, boolean debugRaycast, @NotNull List<? extends Class<?>> ignoredObjects, @NotNull Function5<Integer, Integer, GLVector, GLVector, ? super List<Scene.RaycastResult>, Unit> action) {
        this.getCamera = camera;
        this.debugRaycast = debugRaycast;
        this.ignoredObjects = ignoredObjects;
        this.action = action;
    }


    @Override
    public void click(int x, int y) {
        List<Scene.RaycastResult> matches = getCamera.invoke().getNodesForScreenSpacePosition(x, y, ignoredObjects, debugRaycast);
        GLVector worldPos = new GLVector(0, 0, 0);// FIXME
        GLVector worldDir = new GLVector(1, 0, 0);// FIXME
        action.invoke(x, y, worldPos, worldDir, matches);
    }

}

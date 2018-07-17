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
package sc.iview.commands;

/**
 * Constants for coherent menu ordering.
 *
 * @author Curtis Rueden
 */
public final class MenuWeights {
    private MenuWeights() {
        // NB: Prevent instantiation of utility class.
    }

    public static final double FILE = 0;
    public static final double EDIT = 1;
    public static final double PROCESS = 2;
    public static final double VIEW = 3;
    public static final double DEMO = 4;

    public static final double FILE_OPEN = 0;
    public static final double FILE_EXPORT_STL = 100;

    public static final double EDIT_ADD_BOX = 0;
    public static final double EDIT_ADD_SPHERE = 1;
    public static final double EDIT_ADD_LINE = 2;
    public static final double EDIT_ADD_POINT_LIGHT = 3;
    public static final double EDIT_ADD_LABEL_IMAGE = 4;
    public static final double EDIT_ADD_VOLUME = 5;
    public static final double EDIT_TOGGLE_FLOOR = 50;
    public static final double EDIT_DELETE_OBJECT = 100;
    public static final double EDIT_PROPERTIES = 200;
    public static final double EDIT_SCENE = 300;

    public static final double PROCESS_ISOSURFACE = 0;
    public static final double PROCESS_CONVEX_HULL = 1;
    public static final double PROCESS_MESH_TO_IMAGE = 2;

    public static final double VIEW_ROTATE = 0;
    public static final double VIEW_STOP_ANIMATION = 1;
    public static final double VIEW_TOGGLE_UNLIMITED_FRAMERATE = 2;
    public static final double VIEW_SCREENSHOT = 100;
    public static final double VIEW_SET_LUT = 101;
    public static final double VIEW_TOGGLE_BOUNDING_GRID = 102;
    public static final double VIEW_CENTER_ON_ACTIVE_NODE = 103;
    public static final double VIEW_ARC_BALL_CONTROL = 200;
    public static final double VIEW_FPS_CONTROL = 201;
    public static final double VIEW_RESET_CAMERA_ROTATION = 202;
    public static final double VIEW_RESET_CAMERA_POSITION = 203;
    public static final double VIEW_SAVE_CAMERA_CONFIGURATION = 204;

    public static final double DEMO_LINES = 0;
    public static final double DEMO_MESH = 1;
    public static final double DEMO_MESH_TEXTURE = 2;
    public static final double DEMO_VOLUME_RENDER = 3;
    public static final double DEMO_GAME_OF_LIFE = 4;
}

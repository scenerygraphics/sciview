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
package sc.iview.commands

/**
 * Constants for coherent menu ordering.
 *
 * @author Curtis Rueden
 * @author Kyle Harrington
 */
object MenuWeights {
    const val FILE = 0.0
    const val EDIT = 1.0
    const val PROCESS = 2.0
    const val VIEW = 3.0
    const val DEMO = 4.0
    const val HELP = 4.0
    const val FILE_OPEN = 0.0
    const val FILE_EXPORT_STL = 100.0
    const val EDIT_ADD_BOX = 0.0
    const val EDIT_ADD_SPHERE = 1.0
    const val EDIT_ADD_LINE = 2.0
    const val EDIT_ADD_POINT_LIGHT = 3.0
    const val EDIT_ADD_LABEL_IMAGE = 4.0
    const val EDIT_ADD_VOLUME = 5.0
    const val EDIT_ADD_CAMERA = 6.0
    const val EDIT_ADD_COMPASS = 49.0
    const val EDIT_TOGGLE_FLOOR = 50.0
    const val EDIT_DELETE_OBJECT = 100.0
    const val EDIT_SCIVIEW_SETTINGS = 200.0
    const val PROCESS_ISOSURFACE = 0.0
    const val PROCESS_CONVEX_HULL = 1.0
    const val PROCESS_MESH_TO_IMAGE = 2.0
    const val PROCESS_INTERACTIVE_CONVEX_MESH = 3.0
    const val PROCESS_DRAW_LINES = 4.0
    const val VIEW_ROTATE = 0.0
    const val VIEW_STOP_ANIMATION = 1.0
    const val VIEW_TOGGLE_UNLIMITED_FRAMERATE = 2.0
    const val VIEW_SET_SUPERSAMPLING_FACTOR = 3.0
    const val VIEW_SET_FAR_PLANE = 4.0
    const val VIEW_START_RECORDING_VIDEO = 98.0
    const val VIEW_STOP_RECORDING_VIDEO = 99.0
    const val VIEW_SCREENSHOT = 100.0
    const val VIEW_SET_LUT = 101.0
    const val VIEW_TOGGLE_BOUNDING_GRID = 102.0
    const val VIEW_CENTER_ON_ACTIVE_NODE = 103.0
    const val VIEW_RESET_CAMERA_ROTATION = 202.0
    const val VIEW_RESET_CAMERA_POSITION = 203.0
    const val VIEW_SAVE_CAMERA_CONFIGURATION = 204.0
    const val VIEW_TOGGLE_INSPECTOR = 302.0
    const val VIEW_RENDER_TO_OPENVR = 303.0
    const val VIEW_SET_TRANSFER_FUNCTION = 400.0
    const val DEMO_LINES = 0.0
    const val DEMO_MESH = 1.0
    const val DEMO_MESH_TEXTURE = 2.0
    const val DEMO_VOLUME_RENDER = 3.0
    const val DEMO_GAME_OF_LIFE = 4.0
    const val DEMO_TEXT = 5.0
    const val DEMO_EMBRYO = 6.0
    const val HELP_HELP = 0.0
    const val HELP_ABOUT = 200.0
}
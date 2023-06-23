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
package sc.iview.commands

/**
 * Constants for coherent menu ordering.
 *
 * @author Curtis Rueden
 * @author Kyle Harrington
 */
object MenuWeights {
    const val FILE = 0.0
    const val ADD = 1.0
    const val EDIT = 3.0
    const val PROCESS = 4.0
    const val VIEW = 5.0
    const val DEMO = 6.0
    const val HELP = 7.0
    const val FILE_OPEN = 0.0
    const val FILE_EXPORT = 1.0
    // File/Export
    const val FILE_EXPORT_N5 = 0.0
    const val FILE_EXPORT_STL = 1.0
    const val FILE_EXPORT_XYZ = 2.0
    // Edit
    const val EDIT_SETTINGS = 2.0
    const val EDIT_TOGGLE_FLOOR = 50.0
    const val EDIT_DELETE_OBJECT = 100.0
    const val EDIT_RESET_SCENE = 200.0
    // Edit/Add
    const val EDIT_ADD_BOX = 0.0
    const val EDIT_ADD_CAMERA = 1.0
    const val EDIT_ADD_COMPASS = 2.0
    const val EDIT_ADD_CYLINDER = 3.0
    const val EDIT_ADD_CONE = 4.0
    const val EDIT_ADD_LABELIMAGE = 5.0
    const val EDIT_ADD_LINE = 6.0
    const val EDIT_ADD_POINTLIGHT = 7.0
    const val EDIT_ADD_PROTEIN = 8.0
    const val EDIT_ADD_PROTEIN_FILE = 9.0
    const val EDIT_ADD_SLICING_PLANE = 10.0
    const val EDIT_ADD_SPHERE = 11.0
    const val EDIT_ADD_VOLUME = 12.0

    // Edit/Settings
    const val EDIT_SETTINGS_BINDINGS = 0.0
    const val EDIT_SETTINGS_CONTROLS = 1.0
    const val EDIT_SETTINGS_SCIVIEW = 0.0

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
    // Demo menus (1st level)
    const val DEMO_BASIC = 0.0
    const val DEMO_ANIMATION = 1.0
    const val DEMO_ADVANCED = 2.0
    // Demo/Basic
    const val DEMO_BASIC_LINES = 0.0
    const val DEMO_BASIC_LINE3D = 1.0
    const val DEMO_BASIC_MESH = 2.0
    const val DEMO_BASIC_MULTIMESH = 3.0
    const val DEMO_BASIC_TEXT = 4.0
    const val DEMO_BASIC_IMAGEPLANE = 4.0
    const val DEMO_BASIC_VOLUME = 6.0
    const val DEMO_BASIC_POINTCLOUD = 7.0
    // Demo/Animation
    const val DEMO_ANIMATION_PARTICLE = 0.0
    const val DEMO_ANIMATION_VOLUMETIMESERIES = 1.0
    const val DEMO_ANIMATION_SCENERIGGING = 2.0
    const val DEMO_ANIMATION_GOL3D = 3.0
    // Demo/Advanced
    const val DEMO_ADVANCED_SEGMENTATION = 0.0
    const val DEMO_ADVANCED_CREMI = 1.0
    const val DEMO_ADVANCED_BDVSLICING = 2.0
    const val DEMO_ADVANCED_MESHTEXTURE = 3.0
    // Help
    const val HELP_HELP = 0.0
    const val HELP_ABOUT = 200.0
}

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
package sc.iview.commands.view

import com.google.common.io.Files
import org.scijava.command.Command
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.script.ScriptService
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_SAVE_CAMERA_CONFIGURATION
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException

/**
 * Save the current camera configuration to file.
 *
 * @author Kyle Harrington
 */
@Suppress("UnstableApiUsage")
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Save Camera Configuration", weight = VIEW_SAVE_CAMERA_CONFIGURATION)])
class SaveCameraConfiguration : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private lateinit var saveFile: File

    override fun run() {
        try {
            val cam = sciView.camera ?: return

            val fw = FileWriter(saveFile)
            val bw = BufferedWriter(fw)
            if (!Files.getFileExtension(saveFile.absolutePath).equals("clj", ignoreCase = true)) throw IOException("File must be Clojure (extension = .clj)")
            val pos = cam.spatialOrNull()?.position
            val rot = cam.spatialOrNull()?.rotation
            var scriptContents = "; @SciView sciView\n\n"
            scriptContents += """(.setPosition (.getCamera sciView) (cleargl.GLVector. (float-array [${pos?.x()} ${pos?.y()} ${pos?.z()}])))
"""
            scriptContents += """(.setRotation (.getCamera sciView) (com.jogamp.opengl.math.Quaternion. ${rot?.x()} ${rot?.y()} ${rot?.z()} ${rot?.w()}))
"""
            bw.write(scriptContents)
            bw.close()
            fw.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}

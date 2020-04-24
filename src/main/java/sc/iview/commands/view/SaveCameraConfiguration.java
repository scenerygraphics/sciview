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
package sc.iview.commands.view;

import com.google.common.io.Files;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.script.ScriptService;
import sc.iview.SciView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static sc.iview.commands.MenuWeights.VIEW;
import static sc.iview.commands.MenuWeights.VIEW_SAVE_CAMERA_CONFIGURATION;

/**
 * Save the current camera configuration to file.
 *
 * @author Kyle Harrington
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
menu = {@Menu(label = "View", weight = VIEW), //
        @Menu(label = "Save Camera Configuration", weight = VIEW_SAVE_CAMERA_CONFIGURATION)})
public class SaveCameraConfiguration implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    @Parameter
    private File saveFile;

    @Parameter
    private ScriptService scriptService;

    @Override
    public void run() {
        try {
            FileWriter fw = new FileWriter(saveFile);
            BufferedWriter bw = new BufferedWriter(fw);

            if( !Files.getFileExtension(saveFile.getAbsolutePath()).equalsIgnoreCase("clj") )
                throw new IOException("File must be Clojure (extension = .clj)");

            Vector3f pos = sciView.getCamera().getPosition();
            Quaternionf rot = sciView.getCamera().getRotation();

            String scriptContents = "; @SciView sciView\n\n";
            scriptContents += "(.setPosition (.getCamera sciView) (cleargl.GLVector. (float-array [" + pos.x() + " " + pos.y() + " " + pos.z() + "])))\n";
            scriptContents += "(.setRotation (.getCamera sciView) (com.jogamp.opengl.math.Quaternion. " + rot.x() + " " + rot.y() + " " + rot.z() + " " + rot.w() + "))\n";

            bw.write(scriptContents);

            bw.close();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

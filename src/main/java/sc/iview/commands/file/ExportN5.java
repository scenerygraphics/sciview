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
package sc.iview.commands.file;

import graphics.scenery.Mesh;
import org.janelia.saalfeldlab.n5.N5FSWriter;
import org.janelia.saalfeldlab.n5.N5Writer;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.widget.FileWidget;
import sc.iview.SciView;
import sc.iview.commands.demo.MeshDemo;
import sc.iview.io.N5;
import sc.iview.process.MeshConverter;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import static sc.iview.commands.MenuWeights.FILE;
import static sc.iview.commands.MenuWeights.FILE_EXPORT_STL;

/**
 * Command to export the currently active Node to N5
 *
 * @author Kyle Harrington
 *
 */
@Plugin(type = Command.class, menuRoot = "SciView", //
        menu = { @Menu(label = "File", weight = FILE), //
                 @Menu(label = "Export Node as N5...", weight = FILE_EXPORT_STL) })
public class ExportN5 implements Command {

    @Parameter
    private LogService logService;

    @Parameter
    private SciView sciView;

    @Parameter(style = FileWidget.SAVE_STYLE)
    private File n5File = new File( "" );

    @Parameter(label = "Dataset")
    private String dataset = "/myDataset";

    @Override
    public void run() {
        if( sciView.getActiveNode() instanceof Mesh ) {
            Mesh mesh = ( Mesh ) sciView.getActiveNode();

            if( mesh != null ) {
                try {
                    if( !n5File.exists() )
                        throw new IOException("N5 path does not exist");
                    N5Writer n5 = new N5FSWriter(n5File.getAbsolutePath());

                    N5.save(MeshConverter.toImageJ(mesh), n5, dataset );
                } catch( final Exception e ) {
                    logService.trace( e );
                }
            }

        } else {
            logService.warn("Node is " + sciView.getActiveNode().getNodeType() + " cannot export to N5.");
        }
    }

    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        CommandService command = sv.getScijavaContext().getService(CommandService.class);

        HashMap<String, Object> argmap = new HashMap<>();

        command.run(MeshDemo.class, true, argmap);

        argmap.put("n5File", "/tmp/sciview/test.n5");
        argmap.put("dataset", "/testMesh");
        Thread.sleep(1000);

        command.run(ExportN5.class, true, argmap);
    }
}

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
package sc.iview.commands.demo;

import cleargl.GLVector;
import graphics.scenery.Material;
import graphics.scenery.Node;
import graphics.scenery.volumes.TransferFunction;
import graphics.scenery.volumes.bdv.BDVVolume;
import net.imagej.mesh.Mesh;
import org.apache.commons.io.FileUtils;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo rendering an embryo volume with meshes for nuclei.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Embryo Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Embryo", weight = DEMO_EMBRYO) })
public class EmbryoDemo implements Command {

    @Parameter
    private IOService io;

    @Parameter
    private LogService log;

    @Parameter
    private SciView sciView;

    @Parameter
    private CommandService commandService;

    private String localDirectory = System.getProperty("user.home") + File.separator + "Desktop";

    @Override
    public void run() {
        fetchEmbryoImage(localDirectory);

        BDVVolume v = (BDVVolume) sciView.addBDVVolume(localDirectory + File.separator + "drosophila.xml");
        v.setName( "Embryo Demo" );
        v.setPixelToWorldRatio(0.1f);
        v.setNeedsUpdate(true);
        v.setDirty(true);

        // Set the initial volume transfer function
		/* TODO: TransferFunction behaviour is not yet implemeneted for BDVVolumes
        AtomicReference<Float> rampMax = new AtomicReference<>(0.007f);
        float rampStep = 0.01f;
        AtomicReference<Double> dRampSign = new AtomicReference<>(1.);
        if( rampMax.get() < 0 ) {
            dRampSign.updateAndGet(v1 -> v1 * -1);
        }
        if( rampMax.get() > 0.3 ) {
            dRampSign.updateAndGet(v1 -> v1 * -1);
        }
        rampMax.updateAndGet(v1 -> (float) (v1 + dRampSign.get() * rampStep));
        //System.out.println("RampMax: " + rampMax.get());
        v.setTransferFunction(TransferFunction.ramp(0.0f, rampMax.get()));
        v.setNeedsUpdate(true);
        v.setDirty(true);
		*/
		// use ConverterSetups instead:
		v.getConverterSetups().forEach( s -> s.setDisplayRange( 500.0, 1500.0 ) );

        sciView.centerOnNode( sciView.getActiveNode() );

        //sciView.addSphere();
    }

    public void fetchEmbryoImage(String localDestination) {
        String remoteLocation = "https://fly.mpi-cbg.de/~pietzsch/bdv-examples/";
        String xmlFilename = "drosophila.xml";
        String h5Filename = "drosophila.h5";

        if( !(new File(localDirectory + File.separator + xmlFilename).exists()) ) {
            // File doesnt exist, so fetch
            System.out.println("Fetching data. This may take a moment...");
            try {
                FileUtils.copyURLToFile(new URL(remoteLocation + "/" + xmlFilename),
                        new File(localDestination + File.separator + xmlFilename));
                FileUtils.copyURLToFile(new URL(remoteLocation + "/" + h5Filename),
                        new File(localDestination + File.separator + h5Filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}

/*-
 * #%L
 * Scenery-backed 3D visualization package for ImageJ.
 * %%
 * Copyright (C) 2016 - 2024 sciview developers.
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
package sc.iview.io;

import org.scijava.io.AbstractIOPlugin;
import org.scijava.io.IOPlugin;
import org.scijava.plugin.Plugin;
import org.scijava.util.FileUtils;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import graphics.scenery.primitives.PointCloud;

/** {@link IOPlugin} adapter for Scenery SMLM reader.
 *
 * @author Kyle Harrington
 *
 * */
@Plugin(type = IOPlugin.class, priority = 10)
public class SMLMPointCloudIO extends AbstractIOPlugin<PointCloud> {

    @Override
    public PointCloud open( final String source ) {
        final PointCloud pointCloud = new PointCloud();
        pointCloud.readFromPALM( source );
        return pointCloud;
    }

    @Override
    public Class<PointCloud> getDataType() {
        return PointCloud.class;
    }

    @Override
    public boolean supportsOpen(final String source) {
        if( FileUtils.getExtension(source).toLowerCase().equals(EXTENSION) ) {
            boolean supported = false;
            try {
                FileReader fr = new FileReader( source );
                BufferedReader br = new BufferedReader( fr );

                String header = br.readLine();
                supported = header.contains("frame") && header.contains("x [nm]") && header.contains("y [nm]") &&
                        header.contains("z [nm]") && header.contains("sigma1 [nm]") && header.contains("sigma2 [nm]") &&
                        header.contains("intensity [photon]") && header.contains("offset [photon]") &&
                        header.contains("bkgstd [photon]") && header.contains("uncertainty_xy [nm]") &&
                        header.contains("uncertainty_z [nm]");

                br.close();
                fr.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return supported;
        }
        return false;
    }

    String EXTENSION = "csv";
}

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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.scijava.io.ByteArrayByteBank;
import org.scijava.io.ByteBank;
import org.scijava.util.FileUtils;

/**
 * A helper class to facilitate loading resources from JARs.
 * <p>
 * This class is temporary until the SciJava I/O API evolves to support
 * locations inside of JAR files.
 * </p>
 *
 * @author Curtis Rueden
 */
public final class ResourceLoader {

    private ResourceLoader() {
        // NB: Prevent instantiation of utility class.
    }

    /** Creates a temporary file on disk with the contents of the given resource. */
    public static File createFile( Class<?> c, String resourcePath ) throws IOException {
        final byte[] bytes;
        try (InputStream in = c.getResourceAsStream( resourcePath )) {
            bytes = readStreamFully( in );
        }

        String extension = "." + FileUtils.getExtension( resourcePath );
        File configFile = File.createTempFile( "SciView", extension );
        FileUtils.writeFile( configFile, bytes );
        configFile.deleteOnExit();
        return configFile;
    }

    private static byte[] readStreamFully( final InputStream in ) throws IOException {
        final ByteBank bank = new ByteArrayByteBank();
        byte[] buf = new byte[256 * 1024];
        while( true ) {
            final int r = in.read( buf );
            if( r <= 0 ) break;
            bank.appendBytes( buf, r );
        }
        return bank.toByteArray();
    }
}

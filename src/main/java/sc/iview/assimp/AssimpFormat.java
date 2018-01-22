package sc.iview.assimp;

import net.imagej.mesh.Mesh;
import org.scijava.plugin.HandlerPlugin;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * An interface for reading and writing PLY files
 *
 * PLY specs: http://www.cs.virginia.edu/~gfx/Courses/2001/Advanced.spring.01/plylib/Ply.txt
 *
 * @author Kyle Harrington (University of Idaho, Moscow)
 */
public interface AssimpFormat extends HandlerPlugin<File> {

    String EXTENSION = "*";

    List<Mesh> read(final File assimpFile) throws IOException;

    /** Writes the mesh into a byte[] that can then be saved into a file */
    byte[] writeBinary(final Mesh mesh) throws IOException;

    /** Writes the mesh into a byte[] that can then be saved into a file */
    byte[] writeAscii(final Mesh mesh) throws IOException;
}


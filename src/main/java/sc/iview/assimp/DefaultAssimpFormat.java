package sc.iview.assimp;

import net.imagej.mesh.DefaultMesh;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Triangle;
import net.imagej.mesh.Vertex3;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;
import org.lwjgl.assimp.AIScene;
import org.scijava.plugin.AbstractHandlerPlugin;
import sc.iview.AIDVec3;
import sc.iview.DVec3;

import java.io.File;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.GL_ARRAY_BUFFER_ARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.GL_STATIC_DRAW_ARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.nglBufferDataARB;


/**
 * Created by kharrington on 1/21/18.
 */
public class DefaultAssimpFormat extends AbstractHandlerPlugin<File>
        implements AssimpFormat {

    @Override
    public List<Mesh> read(File assimpFile) throws IOException {
//        String fileName = Thread.currentThread().getContextClassLoader()
//                .getResource("org/lwjgl/demo/opengl/assimp/magnet.obj").getFile();
        //File file = new File(fileName);
        File file = assimpFile;
        // Assimp will be able to find the corresponding mtl file if we call aiImportFile this way.
        System.out.println("Filename: "+file.getAbsolutePath());
        AIScene scene = aiImportFile(file.getAbsolutePath(), aiProcess_JoinIdenticalVertices | aiProcess_Triangulate);
        if (scene == null) {
            throw new IllegalStateException(aiGetErrorString());
        }
        List<Mesh> mshes = constructMeshes(scene); // work like this Model model = new Model(scene);
        return mshes;
    }

    @Override
    public byte[] writeBinary(Mesh mesh) throws IOException {
        return new byte[0];
    }

    @Override
    public byte[] writeAscii(Mesh mesh) throws IOException {
        return new byte[0];
    }

    @Override
    public Class<File> getType() {
        return null;
    }


    public List<Mesh> constructMeshes( AIScene scene ) {
        int meshCount = scene.mNumMeshes();
        PointerBuffer meshesBuffer = scene.mMeshes();
        ArrayList<Mesh> meshes = new ArrayList<>();
        for (int i = 0; i < meshCount; ++i) {
            meshes.add(constructMesh(AIMesh.create(meshesBuffer.get(i))));
        }
        return meshes;
    }

    public Mesh constructMesh(AIMesh aiMesh) {
        DefaultMesh msh = new DefaultMesh();
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        AIFace.Buffer facesBuffer = aiMesh.mFaces();

        // TODO Just start by making arrays from the buffers, then everything is easier
        List<DVec3> verts = new ArrayList<>();
        // First populate
        for( int k = 0; k < verticesBuffer.sizeof(); k++ ) {
            AIVector3D v3d = verticesBuffer.get(k);
            verts.add( (DVec3)(AIDVec3.convert(v3d)) );
        }

        for( AIFace face : facesBuffer ) {
            int[] idcs = face.mIndices().array();
            Triangle tri = new Triangle(msh.getTrianglePool());

            Vertex3 v1 = new Vertex3(msh.getVertex3Pool());
            v1.init(verts.get(idcs[0]).getFloatPosition(0),
                    verts.get(idcs[0]).getFloatPosition(1),
                    verts.get(idcs[0]).getFloatPosition(2));
            Vertex3 v2 = new Vertex3(msh.getVertex3Pool());
            v2.init(verts.get(idcs[1]).getFloatPosition(0),
                    verts.get(idcs[1]).getFloatPosition(1),
                    verts.get(idcs[1]).getFloatPosition(2));
            Vertex3 v3 = new Vertex3(msh.getVertex3Pool());
            v3.init(verts.get(idcs[2]).getFloatPosition(0),
                    verts.get(idcs[2]).getFloatPosition(1),
                    verts.get(idcs[2]).getFloatPosition(2));

            Vertex3 a = new Vertex3(msh.getVertex3Pool());
            a.init(v2.getX()-v1.getX(),v2.getY()-v1.getY(),v2.getZ()-v1.getZ());

            Vertex3 b = new Vertex3(msh.getVertex3Pool());
            b.init(v3.getX()-v1.getX(),v3.getY()-v1.getY(),v3.getZ()-v1.getZ());

            Vertex3 n = new Vertex3(msh.getVertex3Pool());
            n.init( a.getY() * b.getZ() - a.getX() * b.getY(),
                    a.getZ() * b.getX() - a.getX() * b.getZ(),
                    a.getX() * b.getY() - a.getY() * b.getX() );
            float length  = (float) Math.sqrt( n.getX()*n.getX() + n.getY()*n.getY() + n.getZ()*n.getZ() );
            n.setX( n.getX() / length );
            n.setY( n.getY() / length );
            n.setZ( n.getZ() / length );

            tri = tri.init( v1, v2, v3, n );

            msh.addFacet(tri);
        }
        return msh;
    }


}

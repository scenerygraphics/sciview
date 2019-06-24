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
import graphics.scenery.Node;
import graphics.scenery.PointLight;
import graphics.scenery.volumes.bdv.BDVVolume;
import java.io.FileNotFoundException;
import net.imagej.mesh.Mesh;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.mesh.BitTypeVertexInterpolator;
import net.imglib2.Cursor;
import net.imglib2.IterableInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.integer.LongType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.IntervalView;
import net.imglib2.view.Views;
import org.apache.commons.io.FileUtils;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.io.IOService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;
import org.scijava.util.ColorRGB;
import sc.iview.SciView;
import sc.iview.Utils;
import sc.iview.process.MeshConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import static sc.iview.commands.MenuWeights.*;

/**
 * A demo rendering an embryo volume with meshes for nuclei.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Embryo Demo", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "BDV Embryo Segmentation Demo", weight = DEMO_EMBRYO) })
public class EmbryoDemo implements Command {

    @Parameter
    private IOService io;

    @Parameter
    private LogService log;

    @Parameter
    private SciView sciView;

    @Parameter
    private CommandService commandService;

    @Parameter
    private OpService opService;

    @Parameter
    private UIService uiService;

    @Parameter
    private IOService ioService;

    private String localDirectory = System.getProperty("user.home") + File.separator + "Desktop";
    private BDVVolume v;

	private final String xmlFilename = "drosophila.xml";
	private final String h5Filename = "drosophila.h5";
	private final String remoteLocation = "https://fly.mpi-cbg.de/~pietzsch/bdv-examples/";

    @Override
    public void run() {
        fetchEmbryoImage(localDirectory);

        v = (BDVVolume) sciView.addBDVVolume(localDirectory + File.separator + xmlFilename);
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

        System.out.println("Meshing nuclei");

        Img<UnsignedByteType>  filtered = null;
        try {
            filtered = (Img<UnsignedByteType> ) ioService.open("/home/kharrington/Data/Tobi/drosophila_filtered_8bit_v2.tif");
        } catch (IOException e) {
            e.printStackTrace();
        }

//        RandomAccessibleInterval<VolatileUnsignedShortType> volRAI = (RandomAccessibleInterval<VolatileUnsignedShortType>) v.getStack(0, 0, false).resolutions().get(0).getImage();
//        IterableInterval<VolatileUnsignedShortType> volII = Views.iterable(volRAI);
//
//        int isoLevel = 6;
//
//        //Img<UnsignedByteType> volImg = opService.create().img(new long[]{volII.dimension(0), volII.dimension(1), volII.dimension(2)},new UnsignedByteType());
//        Img<UnsignedByteType> volImg = opService.create().img(Intervals.createMinMax(0,0,0,volII.dimension(0), volII.dimension(1), volII.dimension(2)), new UnsignedByteType());
//
//        System.out.println("Populating img: " + volImg);
//
//        Cursor<VolatileUnsignedShortType> c0 = volII.cursor();
//        Cursor<UnsignedByteType> c1 = volImg.cursor();
//        while(c0.hasNext()) {
//            c0.next();
//            c1.next();
//            c1.get().set(c0.get().get().get());
//        }

        Img<UnsignedByteType> volImg = filtered;

        int isoLevel = 75;
        uiService.show(volImg);

        Img<BitType> bitImg = ( Img<BitType> ) opService.threshold().apply( volImg, new UnsignedByteType( isoLevel ) );
        System.out.println("Thresholding done");

        //ImgLabeling<Object, IntType> labels = opService.labeling().cca(bitImg, ConnectedComponents.StructuringElement.FOUR_CONNECTED);

        int start = 1;
        final Iterator< Integer > names = new Iterator< Integer >()
		{
			private int i = start;

			@Override
			public boolean hasNext()
			{
				return true;
			}

			@Override
			public Integer next()
			{
				return i++;
			}

			@Override
			public void remove()
			{}
		};
        final long[] dimensions = new long[] { bitImg.dimension(0), bitImg.dimension(1), bitImg.dimension(2) };
        final Img< LongType > indexImg = ArrayImgs.longs( dimensions );
		final ImgLabeling< Integer, LongType > labeling = new ImgLabeling<>(indexImg);
        ConnectedComponents.labelAllConnectedComponents( bitImg, labeling, names, ConnectedComponents.StructuringElement.FOUR_CONNECTED );

        uiService.show(bitImg);
        uiService.show(indexImg);

        Node[] lights = sciView.getSceneNodes(n -> n instanceof PointLight);
        float y = 0;
        GLVector c = new GLVector(0,0,0);
        float r = 500;
        for( int k = 0; k < lights.length; k++ ) {
            PointLight light = (PointLight) lights[k];
            float x = (float) (c.x() + r * Math.cos( k == 0 ? 0 : Math.PI * 2 * ((float)k / (float)lights.length) ));
            float z = (float) (c.y() + r * Math.sin( k == 0 ? 0 : Math.PI * 2 * ((float)k / (float)lights.length) ));
            light.setLightRadius( 2 * r );
            light.setPosition( new GLVector( x, y, z ) );
        }

        showMeshes(labeling);

//        Mesh m = opService.geom().marchingCubes( bitImg, isoLevel, new BitTypeVertexInterpolator() );
//        System.out.println("Marching cubes done");
//
//        graphics.scenery.Mesh isoSurfaceMesh = MeshConverter.toScenery(m,true);
//        Node scMesh = sciView.addMesh(isoSurfaceMesh);
//
//        isoSurfaceMesh.setName( "Volume Render Demo Isosurface" );
//        isoSurfaceMesh.setScale(new GLVector(v.getPixelToWorldRatio(),
//                v.getPixelToWorldRatio(),
//                v.getPixelToWorldRatio()));

        //sciView.addSphere();
    }

    public void showMeshes( ImgLabeling<Integer, LongType> labeling ) {

        RandomAccessibleInterval<LongType> labelRAI = labeling.getIndexImg();
        IterableInterval<LongType> labelII = Views.iterable(labelRAI);
        HashSet<Long> labelSet = new HashSet<>();
        Cursor<LongType> cur = labelII.cursor();
        while( cur.hasNext() ) {
            cur.next();
            labelSet.add((long) cur.get().get());
        }

        // Create label list and colors
        ArrayList<LongType> labelList = new ArrayList<>();
        ArrayList<ColorRGB> labelColors = new ArrayList<>();
        for( Long l : labelSet ) {
            labelList.add( new LongType(Math.toIntExact(l)) );
            labelColors.add( new ColorRGB((int) (Math.random()*255), (int) (Math.random()*255), (int) (Math.random()*255) ) );
        }

        int numRegions = labelList.size();

//        GLVector vOffset = new GLVector(v.getSizeX() * v.getVoxelSizeX() * v.getPixelToWorldRatio() * 0.5f,
//                        v.getSizeY() * v.getVoxelSizeY() * v.getPixelToWorldRatio() * 0.5f,
//                        v.getSizeZ() * v.getVoxelSizeZ() * v.getPixelToWorldRatio() * 0.5f);


        float xscale = v.getVoxelSizeX() * v.getPixelToWorldRatio() * 0.36f;
        float yscale =  v.getVoxelSizeY() * v.getPixelToWorldRatio() * 0.3f;
        float zscale = v.getVoxelSizeZ() * v.getPixelToWorldRatio() * 0.25f;

        GLVector vHalf = new GLVector(v.getSizeX() * xscale,
                        v.getSizeY() * yscale,
                        v.getSizeZ() * zscale);

        GLVector vOffset = new GLVector(0,0,0);


        System.out.println("Found " + numRegions + " regions");
        System.out.println("Voxel size: " + v.getVoxelSizeX() + " , " + v.getVoxelSizeY() + " " + v.getVoxelSizeZ() + " voxtoworld: " + v.getPixelToWorldRatio());

        Random rng = new Random();

        // Create label images and segmentations
        //for( LabelRegion labelRegion : labelRegions ) {
        for( int k = 0; k < numRegions; k++ ) {
            System.out.println("Starting to process region " + k );
            long id = labelList.get(k).getIntegerLong();

            if( id > 1 ) { // Ignore background

                // get labelColor using ID
                //ColorRGB labelColor = labelColors.get(k);
                //ColorRGB labelColor = colorFromID(id);
                //ColorRGB labelColor = new ColorRGB(255,255,255);
                ColorRGB labelColor = new ColorRGB(rng.nextInt(255),rng.nextInt(255), rng.nextInt(255));

                Img<BitType> labelRegion = opService.create().img(labelII, new BitType());
                cur = labelII.cursor();
                LongType thisLabel = labelList.get(k);
                Cursor<BitType> rCur = labelRegion.cursor();
                long sum = 0;
                while (cur.hasNext()) {
                    cur.next();
                    rCur.next();
                    if (cur.get().valueEquals(thisLabel)) {
                        rCur.get().set(true);
                        sum++;
                    } else {
                        rCur.get().set(false);
                    }
                }
                System.out.println("Label " + k + " has sum voxels = " + sum);

                // FIXME: hack to skip large meshes
                if ( sum > 10 && sum < 10000) {

                    // Find the smallest bounding box
                    int xmin=Integer.MAX_VALUE, ymin=Integer.MAX_VALUE, zmin=Integer.MAX_VALUE;
                    int xmax=Integer.MIN_VALUE, ymax=Integer.MIN_VALUE, zmax=Integer.MIN_VALUE;
                    int x,y,z;
                    rCur = labelRegion.cursor();
                    while(rCur.hasNext()) {
                        rCur.next();
                        if( rCur.get().get() ) {
                            x = rCur.getIntPosition(0);
                            y = rCur.getIntPosition(1);
                            z = rCur.getIntPosition(2);
                            xmin = Math.min(xmin,x);
                            ymin = Math.min(ymin,y);
                            zmin = Math.min(zmin,z);
                            xmax = Math.max(xmax,x);
                            ymax = Math.max(ymax,y);
                            zmax = Math.max(zmax,z);
                        }
                    }

                    IntervalView<BitType> cropLabelRegion = Views.interval(labelRegion, new long[]{xmin, ymin, zmin}, new long[]{xmax, ymax, zmax});

                    Mesh m = opService.geom().marchingCubes(cropLabelRegion, 1, new BitTypeVertexInterpolator());

                    graphics.scenery.Mesh isoSurfaceMesh = MeshConverter.toScenery(m, false);
                    isoSurfaceMesh.recalculateNormals();

                    Node scMesh = sciView.addMesh(isoSurfaceMesh);
                    scMesh.setPosition(scMesh.getPosition().minus(vOffset).plus(new GLVector( -vHalf.x() + xmin* xscale,
                            ymin* yscale,
                            zmin* zscale)));
                    scMesh.setScale(new GLVector(v.getPixelToWorldRatio(),
                            v.getPixelToWorldRatio(),
                            v.getPixelToWorldRatio()));
                    scMesh.getMaterial().setAmbient(Utils.convertToGLVector(labelColor));
                    scMesh.getMaterial().setDiffuse(Utils.convertToGLVector(labelColor));
                    scMesh.setName("region_" + k);
                    scMesh.setNeedsUpdate(true);
                    scMesh.setDirty(true);
                    scMesh.getMetadata().put("mesh_ID", id);

                }
            }
            //scMesh.setName( "region_" + labelRegion.getLabel() );
        }
        sciView.takeScreenshot();

        // Remove all other old meshes
//        for( Node n : previousMeshes ) {
//            sciView.deleteNode( n );
//        }

        // Color code meshes and overlay in volume
    }

    public static ColorRGB colorFromID( Long id ) {
        // Hash to get label colors so they are unique by id
        Random rng = new Random(id);
        return new ColorRGB( rng.nextInt(256), rng.nextInt(256), rng.nextInt(256) );
    }

    public void fetchEmbryoImage(String localDestination) {
        if( !(new File(localDirectory + File.separator + xmlFilename).exists()) ) {
            // File doesnt exist, so fetch
            System.out.println("Fetching data. This may take a moment...");
            try {
                FileUtils.copyURLToFile(new URL(remoteLocation + "/" + xmlFilename),
                        new File(localDestination + File.separator + xmlFilename));
                FileUtils.copyURLToFile(new URL(remoteLocation + "/" + h5Filename),
                        new File(localDestination + File.separator + h5Filename));
            } catch (FileNotFoundException e) {
				System.out.println("Could not download file, " + xmlFilename + " not found on host.");
			} catch (IOException e) {
                e.printStackTrace();
            }
		}

    }
}

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
package sc.iview.commands.demo.advanced;

import bdv.BigDataViewer;
import bdv.cache.SharedQueue;
import bdv.tools.brightness.ConverterSetup;
import bdv.util.volatiles.VolatileTypeMatcher;
import bdv.util.volatiles.VolatileViews;
import bdv.viewer.Source;
import bdv.viewer.SourceAndConverter;
import com.intellij.util.Consumer;
import graphics.scenery.attribute.material.Material;
import graphics.scenery.volumes.Volume;
import io.scif.services.DatasetIOService;
import net.imagej.mesh.Mesh;
import net.imagej.mesh.Meshes;
import net.imagej.ops.OpService;
import net.imglib2.Cursor;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealPoint;
import net.imglib2.Volatile;
import net.imglib2.algorithm.blocks.BlockAlgoUtils;
import net.imglib2.algorithm.blocks.convert.Convert;
import net.imglib2.algorithm.labeling.ConnectedComponents;
import net.imglib2.blocks.PrimitiveBlocks;
import net.imglib2.cache.Cache;
import net.imglib2.cache.CacheLoader;
import net.imglib2.cache.LoaderCache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.img.RandomAccessibleCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.cache.volatiles.CacheHints;
import net.imglib2.cache.volatiles.LoadingStrategy;
import net.imglib2.converter.Converters;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.basictypeaccess.volatiles.VolatileByteAccess;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileByteArray;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.type.volatiles.VolatileARGBType;
import net.imglib2.type.volatiles.VolatileUnsignedShortType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import org.janelia.saalfeldlab.n5.DataType;
import org.janelia.saalfeldlab.n5.DatasetAttributes;
import org.janelia.saalfeldlab.n5.N5Reader;
import org.janelia.saalfeldlab.n5.bdv.MultiscaleDatasets;
import org.janelia.saalfeldlab.n5.bdv.N5Source;
import org.janelia.saalfeldlab.n5.bdv.N5ViewerTreeCellRenderer;
import org.janelia.saalfeldlab.n5.ij.N5Factory;
import org.janelia.saalfeldlab.n5.ij.N5Importer;
import org.janelia.saalfeldlab.n5.imglib2.N5CacheLoader;
import org.janelia.saalfeldlab.n5.imglib2.N5Utils;
import org.janelia.saalfeldlab.n5.metadata.*;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMultichannelMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalMultiscaleMetadata;
import org.janelia.saalfeldlab.n5.metadata.canonical.CanonicalSpatialMetadata;
import org.janelia.saalfeldlab.n5.ui.DataSelection;
import org.janelia.saalfeldlab.n5.ui.DatasetSelectorDialog;
import org.joml.Vector3f;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.log.LogService;
import org.scijava.plugin.Menu;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import sc.iview.SciView;
import sc.iview.process.MeshConverter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

import static bdv.BigDataViewer.createConverterToARGB;
import static bdv.BigDataViewer.wrapWithTransformedSource;
import static org.janelia.saalfeldlab.n5.bdv.N5ViewerCreator.n5vGroupParsers;
import static org.janelia.saalfeldlab.n5.bdv.N5ViewerCreator.n5vParsers;
import static sc.iview.commands.MenuWeights.*;

/**
 * OpenOrganelle demo
 *
 * This borrows heavily from n5-viewer and BVV code.
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command.class, label = "Open Organelle", menuRoot = "SciView", //
        menu = { @Menu(label = "Demo", weight = DEMO), //
                 @Menu(label = "Advanced", weight = DEMO_ADVANCED), //
                 @Menu(label = "Open Organelle (Java)", weight = DEMO_ADVANCED_SEGMENTATION) })
public class OpenOrganelleDemo implements Command {

    @Parameter
    private DatasetIOService datasetIO;

    @Parameter
    private LogService log;

    @Parameter
    private OpService ops;

    @Parameter
    private SciView sciView;

    @Parameter
    private int numSegments = 20;

    private Random rng = new Random();

    private static String containerName = "s3://janelia-cosem-datasets/jrc_mus-kidney/jrc_mus-kidney.n5";

    @Override
    public void run() {
        openN5(sciView);
    }

    static public void openN5(SciView sciView) {
        ExecutorService exec = Executors.newFixedThreadPool( ij.Prefs.getThreads() );
        final DatasetSelectorDialog dialog = new DatasetSelectorDialog(
                new N5Importer.N5ViewerReaderFun(),
                x -> "",
                containerName,
                n5vGroupParsers,
                n5vParsers);

        dialog.setLoaderExecutor( exec );

//		dialog.setRecursiveFilterCallback( new N5ViewerDatasetFilter() );
        dialog.setContainerPathUpdateCallback( x -> containerName = x );
        dialog.setTreeRenderer( new N5ViewerTreeCellRenderer( false ) );

        dialog.run( selection -> {
            addDataSelection(selection, sciView);
        } );
    }

    /**
     * Thanks Tobi!
     * @param rai
     * @return
     */
    static CachedCellImg< UnsignedShortType, ? > convert(
            RandomAccessibleInterval< UnsignedByteType > rai )
    {
        final int[] cellDimensions = { 64, 64, 64 };
        final PrimitiveBlocks< UnsignedByteType > blocks = PrimitiveBlocks.of( rai );
        final CachedCellImg< UnsignedShortType, ? > img = BlockAlgoUtils.cellImg(
                blocks,
                Convert.convert( new UnsignedByteType(), new UnsignedShortType() ),
                new UnsignedShortType(),
                rai.dimensionsAsLongArray(),
                cellDimensions
        );
        return img;
        // return VolatileViews.wrapAsVolatile( img );
    }

    static public < T extends NumericType< T > & NativeType< T >,
            V extends Volatile< T > & NumericType< V >> void buildN5Sources(
            final N5Reader n5,
            final List<N5Metadata> selectedMetadata,
            final SharedQueue sharedQueue,
            final List< ConverterSetup > converterSetups,
            final List< SourceAndConverter< T > > sourcesAndConverters,
            final List<N5Source<T>> sources,
            final List<N5Source<V>> volatileSources) throws IOException
    {
        final ArrayList<MetadataSource<?>> additionalSources = new ArrayList<>();

        int i;
        for ( i = 0; i < selectedMetadata.size(); ++i )
        {
            String[] datasetsToOpen = null;
            AffineTransform3D[] transforms = null;

            final N5Metadata metadata = selectedMetadata.get( i );
            final String srcName = metadata.getName();
            if (metadata instanceof N5SingleScaleMetadata) {
                final N5SingleScaleMetadata singleScaleDataset = (N5SingleScaleMetadata) metadata;
                final String[] tmpDatasets= new String[]{ singleScaleDataset.getPath() };
                final AffineTransform3D[] tmpTransforms = new AffineTransform3D[]{ singleScaleDataset.spatialTransform3d() };

                final MultiscaleDatasets msd = MultiscaleDatasets.sort( tmpDatasets, tmpTransforms );
                datasetsToOpen = msd.getPaths();
                transforms = msd.getTransforms();
            } else if (metadata instanceof N5MultiScaleMetadata) {
                final N5MultiScaleMetadata multiScaleDataset = (N5MultiScaleMetadata) metadata;
                datasetsToOpen = multiScaleDataset.getPaths();
                transforms = multiScaleDataset.spatialTransforms3d();
            } else if (metadata instanceof N5CosemMetadata ) {
                final N5CosemMetadata singleScaleCosemDataset = (N5CosemMetadata) metadata;
                datasetsToOpen = new String[]{ singleScaleCosemDataset.getPath() };
                transforms = new AffineTransform3D[]{ singleScaleCosemDataset.spatialTransform3d() };
            } else if (metadata instanceof CanonicalSpatialMetadata) {
                final CanonicalSpatialMetadata canonicalDataset = (CanonicalSpatialMetadata) metadata;
                datasetsToOpen = new String[]{ canonicalDataset.getPath() };
                transforms = new AffineTransform3D[]{ canonicalDataset.getSpatialTransform().spatialTransform3d() };
            } else if (metadata instanceof N5CosemMultiScaleMetadata ) {
                final N5CosemMultiScaleMetadata multiScaleDataset = (N5CosemMultiScaleMetadata) metadata;
                final MultiscaleDatasets msd = MultiscaleDatasets.sort( multiScaleDataset.getPaths(), multiScaleDataset.spatialTransforms3d() );
                datasetsToOpen = msd.getPaths();
                transforms = msd.getTransforms();
            } else if (metadata instanceof CanonicalMultiscaleMetadata) {
                final CanonicalMultiscaleMetadata multiScaleDataset = (CanonicalMultiscaleMetadata) metadata;
                final MultiscaleDatasets msd = MultiscaleDatasets.sort( multiScaleDataset.getPaths(), multiScaleDataset.spatialTransforms3d() );
                datasetsToOpen = msd.getPaths();
                transforms = msd.getTransforms();
            }
            else if( metadata instanceof N5DatasetMetadata ) {
                final List<MetadataSource<?>> addTheseSources = MetadataSource.buildMetadataSources(n5, (N5DatasetMetadata)metadata);
                if( addTheseSources != null )
                    additionalSources.addAll(addTheseSources);
            }
            else {
                datasetsToOpen = new String[]{ metadata.getPath() };
                transforms = new AffineTransform3D[] { new AffineTransform3D() };
            }

            if( datasetsToOpen == null || datasetsToOpen.length == 0 )
                continue;

            @SuppressWarnings( "rawtypes" )
            final RandomAccessibleInterval[] images = new RandomAccessibleInterval[datasetsToOpen.length];
            for ( int s = 0; s < images.length; ++s )
            {
                CachedCellImg<UnsignedShortType, ?> vimg = convert(N5Utils.openVolatile(n5, datasetsToOpen[s]));
                //final CachedCellImg<?, ?> unsignedShortVimg = Converters.convert(vimg, (i, o) -> o.set(i.getRealDouble()));

                if( vimg.numDimensions() == 2 )
                {
                    images[ s ] = Views.addDimension(vimg, 0, 0);
                }
                else
                {
                    images[ s ] = vimg;
                }
            }

            final RandomAccessibleInterval[] vimages = new RandomAccessibleInterval[images.length];
            for (int s = 0; s < images.length; ++s) {
                final CacheHints cacheHints = new CacheHints(LoadingStrategy.VOLATILE, 0, true);
                vimages[s] = VolatileViews.wrapAsVolatile(images[s], sharedQueue, cacheHints);
            }
            // TODO: Ideally, the volatile views should use a caching strategy
            //   where blocks are enqueued with reverse resolution level as
            //   priority. However, this would require to predetermine the number
            //   of resolution levels, which would man a lot of duplicated code
            //   for analyzing selectedMetadata. Instead, wait until SharedQueue
            //   supports growing numPriorities, then revisit.
            //   See https://github.com/imglib/imglib2-cache/issues/18.
            //   Probably it should look like this:
//			sharedQueue.ensureNumPriorities(images.length);
//			for (int s = 0; s < images.length; ++s) {
//				final int priority = images.length - 1 - s;
//				final CacheHints cacheHints = new CacheHints(LoadingStrategy.BUDGETED, priority, false);
//				vimages[s] = VolatileViews.wrapAsVolatile(images[s], sharedQueue, cacheHints);
//			}

            @SuppressWarnings("unchecked")
            final T type = (T) Util.getTypeFromInterval(images[0]);
            final N5Source<T> source = new N5Source<>(
                    type,
                    srcName,
                    images,
                    transforms);

            @SuppressWarnings("unchecked")
            final V volatileType = (V) VolatileTypeMatcher.getVolatileTypeForType(type);
            final N5Source<V> volatileSource = new N5Source<>(
                    volatileType,
                    srcName,
                    vimages,
                    transforms);

            sources.add(source);
            volatileSources.add(volatileSource);

            addSourceToListsGenericType(source, volatileSource, i + 1, converterSetups, sourcesAndConverters);
        }

        for( final MetadataSource src : additionalSources ) {
//            if( src.numTimePoints() > numTimepoints )
//                numTimepoints = src.numTimePoints();

            addSourceToListsGenericType( src, i + 1, converterSetups, sourcesAndConverters );
        }
    }

    /**
     * Add the given {@code source} to the lists of {@code converterSetups}
     * (using specified {@code setupId}) and {@code sources}. For this, the
     * {@code source} is wrapped with an appropriate Converter to
     * {@link ARGBType} and into a TransformedSource.
     *
     * @param source
     *            source to add.
     * @param setupId
     *            id of the new source for use in {@code SetupAssignments}.
     * @param converterSetups
     *            list of {@link ConverterSetup}s to which the source should be
     *            added.
     * @param sources
     *            list of {@link SourceAndConverter}s to which the source should
     *            be added.
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private static < T > void addSourceToListsGenericType(
            final Source< T > source,
            final int setupId,
            final List< ConverterSetup > converterSetups,
            final List< SourceAndConverter< T > > sources )
    {
        addSourceToListsGenericType( source, null, setupId, converterSetups, sources );
    }

    /**
     * Add the given {@code source} to the lists of {@code converterSetups}
     * (using specified {@code setupId}) and {@code sources}. For this, the
     * {@code source} is wrapped with an appropriate Converter to
     * {@link ARGBType} and into a TransformedSource.
     *
     * @param source
     *            source to add.
     * @param setupId
     *            id of the new source for use in {@code SetupAssignments}.
     * @param converterSetups
     *            list of {@link ConverterSetup}s to which the source should be
     *            added.
     * @param sources
     *            list of {@link SourceAndConverter}s to which the source should
     *            be added.
     */
    @SuppressWarnings( { "rawtypes", "unchecked" } )
    private static < T, V extends Volatile< T > > void addSourceToListsGenericType(
            final Source< T > source,
            final Source< V > volatileSource,
            final int setupId,
            final List< ConverterSetup > converterSetups,
            final List< SourceAndConverter< T > > sources )
    {
        final T type = source.getType();
        if ( type instanceof RealType || type instanceof ARGBType || type instanceof VolatileARGBType)
            addSourceToListsNumericType( ( Source ) source, ( Source ) volatileSource, setupId, converterSetups, ( List ) sources );
        else
            throw new IllegalArgumentException( "Unknown source type. Expected RealType, ARGBType, or VolatileARGBType" );
    }

    /**
     * Add the given {@code source} to the lists of {@code converterSetups}
     * (using specified {@code setupId}) and {@code sources}. For this, the
     * {@code source} is wrapped with an appropriate Converter to
     * {@link ARGBType} and into a TransformedSource.
     *
     * @param source
     *            source to add.
     * @param volatileSource
     *            corresponding volatile source.
     * @param setupId
     *            id of the new source for use in {@code SetupAssignments}.
     * @param converterSetups
     *            list of {@link ConverterSetup}s to which the source should be
     *            added.
     * @param sources
     *            list of {@link SourceAndConverter}s to which the source should
     *            be added.
     */
    private static < T extends NumericType< T >, V extends Volatile< T > & NumericType< V > > void addSourceToListsNumericType(
            final Source< T > source,
            final Source< V > volatileSource,
            final int setupId,
            final List< ConverterSetup > converterSetups,
            final List< SourceAndConverter< T > > sources )
    {
        final SourceAndConverter< V > vsoc = ( volatileSource == null )
                ? null
                : new SourceAndConverter<>( volatileSource, createConverterToARGB( volatileSource.getType() ) );
        final SourceAndConverter< T > soc = new SourceAndConverter<>( source, createConverterToARGB( source.getType() ), vsoc );
        final SourceAndConverter< T > tsoc = wrapWithTransformedSource( soc );

        converterSetups.add( BigDataViewer.createConverterSetup( tsoc, setupId ) );
        sources.add( tsoc );
    }

    static private < T extends NumericType< T > & NativeType< T >,
            V extends Volatile< T > & NumericType< V >> void addDataSelection(DataSelection dataSelection, SciView sciView) {
        // This function should do the same type of job as N5Viewer()
        SharedQueue sharedQueue = new SharedQueue(Math.max(1, Runtime.getRuntime().availableProcessors() / 2));

        final List<ConverterSetup> converterSetups = new ArrayList<>();

        final List<SourceAndConverter< T >> sourcesAndConverters = new ArrayList<>();

        final List<N5Metadata> selected = new ArrayList<>();
        for( final N5Metadata meta : dataSelection.metadata )
        {
            if( meta instanceof N5ViewerMultichannelMetadata)
            {
                final N5ViewerMultichannelMetadata mc = (N5ViewerMultichannelMetadata)meta;
                for( final MultiscaleMetadata<?> m : mc.getChildrenMetadata() )
                    selected.add( m );
            }
            else if ( meta instanceof CanonicalMultichannelMetadata)
            {
                final CanonicalMultichannelMetadata mc = (CanonicalMultichannelMetadata)meta;
                for( final N5Metadata m : mc.getChildrenMetadata() )
                    selected.add( m );
            }
            else
                selected.add( meta );
        }

        final List<N5Source<T>> sources = new ArrayList<>();
        final List<N5Source<V>> volatileSources = new ArrayList<>();

        try {
            buildN5Sources(dataSelection.n5, selected, sharedQueue, converterSetups, sourcesAndConverters, sources, volatileSources);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        /**
         * Notes:
         *
         * MultiResolutionStack3DImp
         * https://github.com/bigdataviewer/bigvolumeviewer-core/blob/9f6a7d66dc4d92a7d9e011a680fdc5facd4706a0/src/main/java/bvv/core/multires/SourceStacks.java#L101
         *
         * TileAccess
         * https://github.com/bigdataviewer/bigvolumeviewer-core/blob/9f6a7d66dc4d92a7d9e011a680fdc5facd4706a0/src/main/java/bvv/core/blocks/TileAccess.java#L133
         *
         * Only UnsignedShortType is supported
         *
         * https://javadoc.scijava.org/ImgLib2/net/imglib2/cache/img/LoadedCellCacheLoader.html
         *
         */



        // use scenery's Volume.fromSpimData to open
        Volume v = sciView.addVolume(
                sourcesAndConverters,
                converterSetups,
                1,
                containerName);

    }


    public static void main(String... args) throws Exception {
        SciView sv = SciView.create();

        // Many functions were made static here out of concern for issue sciview#494
        openN5(sv);
        // TODO pickup here, exit 134 this might be an OOM

//        CommandService command = sv.getScijavaContext().getService(CommandService.class);
//
//        HashMap<String, Object> argmap = new HashMap<String, Object>();
//
//        command.run(OpenOrganelleDemo.class, true, argmap);
    }
}

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
package sc.iview.commands.file

import bdv.cache.SharedQueue
import bdv.tools.brightness.ConverterSetup
import bdv.util.BdvOptions
import bdv.util.volatiles.VolatileViews
import bdv.viewer.SourceAndConverter
import net.imagej.Dataset
import net.imglib2.realtransform.AffineTransform3D
import net.imglib2.type.numeric.RealType
import net.imglib2.type.numeric.integer.*
import net.imglib2.type.numeric.real.DoubleType
import net.imglib2.type.numeric.real.FloatType
import org.janelia.saalfeldlab.n5.DataType
import org.janelia.saalfeldlab.n5.N5FSReader
import org.janelia.saalfeldlab.n5.N5Reader
import org.janelia.saalfeldlab.n5.N5URI
import org.janelia.saalfeldlab.n5.bdv.N5Viewer
import org.janelia.saalfeldlab.n5.imglib2.N5Utils
import org.janelia.saalfeldlab.n5.universe.N5Factory
import org.janelia.saalfeldlab.n5.universe.N5MetadataUtils
import org.janelia.saalfeldlab.n5.universe.metadata.N5Metadata
import org.scijava.ItemVisibility
import org.scijava.command.DynamicCommand
import org.scijava.log.LogService
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import org.scijava.widget.ChoiceWidget
import org.scijava.widget.NumberWidget
import sc.iview.SciView
import sc.iview.commands.MenuWeights.FILE
import sc.iview.commands.MenuWeights.FILE_OPEN
import ucar.units.StandardUnitFormatConstants.T
import java.io.File
import java.io.IOException
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.max


/**
 * Command to open a file in SciView
 *
 * @author Kyle Harrington
 * @author Ulrik Guenther
 */
@Plugin(
    type = DynamicCommand::class,
    menuRoot = "SciView",
    menu = [Menu(label = "File", weight = FILE), Menu(label = "Open N5...", weight = FILE_OPEN)]
)
class OpenN5 : DynamicCommand() {
    @Parameter
    private lateinit var log: LogService

    @Parameter
    private lateinit var sciView: SciView

    // TODO: Find a more extensible way than hard-coding the extensions.
    @Parameter(style = "directory", callback = "refreshDatasets", required = true, persist = true)
    private lateinit var file: File

    @Parameter(required = true, style = ChoiceWidget.LIST_BOX_STYLE, choices = ["(none)"], callback = "refreshVoxelSize", persist = false)
    private lateinit var dataset: String

    private lateinit var reader: N5Reader

    @Parameter(style = NumberWidget.SPINNER_STYLE + ",format:0.000", stepSize = "0.1", persist = false)
    private var voxelSizeX = 1.0f

    @Parameter(style = NumberWidget.SPINNER_STYLE + ",format:0.000", stepSize = "0.1", persist = false)
    private var voxelSizeY = 1.0f

    @Parameter(style = NumberWidget.SPINNER_STYLE + ",format:0.000", stepSize = "0.1", persist = false)
    private var voxelSizeZ = 1.0f

    @Parameter(style = NumberWidget.SPINNER_STYLE + ",format:0.000", stepSize = "0.1", persist = false)
    private var unitScaling = 1.0f

    @Parameter(visibility = ItemVisibility.MESSAGE, persist = false)
    private var unitMessage = ""

    private var multiscaleDatasets = mutableListOf<String>()

    @Suppress("unused")
    private fun refreshDatasets() {
        reader = N5FSReader(file.absolutePath)
        val includedDatasets = reader.deepListDatasets("/")

        val isMultiscale = includedDatasets.map { it.split("/").last() }.all { it.startsWith("s") }
        if(isMultiscale) {
            log.info("Discovered dataset: ${includedDatasets.first()} (multiscale)")
            multiscaleDatasets = includedDatasets.toMutableList()
            info.getMutableInput("dataset", String::class.java).choices = listOf(includedDatasets.first().split("/").first())
            dataset = includedDatasets.first().split("/").first()
        } else {
            log.info("Discovered dataset: ${includedDatasets.joinToString(", ")}")
            info.getMutableInput("dataset", String::class.java).choices = includedDatasets.toMutableList()
            dataset = includedDatasets.first()
        }

        refreshVoxelSize()
    }

    private fun refreshVoxelSize() {
        log.info("dataset is $dataset")
        reader = N5FSReader(file.absolutePath)
        val resolution = reader.getAttribute(dataset, "resolution", FloatArray::class.java)
        if(resolution != null) {
            voxelSizeX = resolution[0]
            voxelSizeY = resolution[1]
            voxelSizeZ = resolution[2]
        }

        val units = reader.getAttribute(dataset, "units", Array<String>::class.java)

        if(units != null) {
            unitScaling = when(units.first()) {
                "nm" -> 0.001f
                "µm" -> 1.0f
                "mm" -> 1000.0f
                else -> 1.0f
            }
        }

        unitMessage = if(units == null || resolution == null) {
            "Dataset is missing resolution or unit information.\nOne voxel will occupy ${voxelSizeX*unitScaling}m in world space."
        } else {
            "Individual voxels will appear in the scene as ${voxelSizeX * unitScaling} m (world units) in size."
        }
    }



    override fun run() {
        try {
            if(multiscaleDatasets.size == 0) {
                val attributes = reader.getDatasetAttributes(dataset)
                val img = when(attributes.dataType) {
                    DataType.UINT8 -> N5Utils.openVolatile<UnsignedByteType>(reader, dataset)
                    DataType.UINT16 -> N5Utils.openVolatile<UnsignedShortType>(reader, dataset)
                    DataType.UINT32 -> N5Utils.openVolatile<UnsignedIntType>(reader, dataset)
                    DataType.UINT64 -> N5Utils.openVolatile<UnsignedLongType>(reader, dataset)
                    DataType.INT8 -> N5Utils.openVolatile<ByteType>(reader, dataset)
                    DataType.INT16 -> N5Utils.openVolatile<ShortType>(reader, dataset)
                    DataType.INT32 -> N5Utils.openVolatile<IntType>(reader, dataset)
                    DataType.INT64 -> N5Utils.openVolatile<LongType>(reader, dataset)
                    DataType.FLOAT32 -> N5Utils.openVolatile<FloatType>(reader, dataset)
                    DataType.FLOAT64 -> N5Utils.openVolatile<DoubleType>(reader, dataset)
                    DataType.OBJECT -> TODO()
                    null -> TODO()
                    DataType.STRING -> TODO()
                }

                val wrapped = VolatileViews.wrapAsVolatile(img)
                sciView.addVolume(
                    wrapped,
                    dataset,
                    voxelDimensions = floatArrayOf(
                        voxelSizeX * unitScaling * 1000.0f,
                        voxelSizeY * unitScaling * 1000.0f,
                        voxelSizeZ * unitScaling * 1000.0f
                    )
                )
            } else {
                N5Opener.openN5(sciView, file.absolutePath)
            }
        } catch(exc: IOException) {
            log.error(exc)
        } catch(exc: IllegalArgumentException) {
            log.error(exc)
        }
    }
}
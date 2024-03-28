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
package sc.iview.commands.view

import org.scijava.command.Command
import org.scijava.plugin.Menu
import org.scijava.plugin.Parameter
import org.scijava.plugin.Plugin
import sc.iview.SciView
import sc.iview.commands.MenuWeights.VIEW
import sc.iview.commands.MenuWeights.VIEW_START_RECORDING_VIDEO
import java.io.File
import kotlin.math.max

/**
 * Command to start recording a video. Currently this will record to ~/Desktop
 *
 * @author Kyle Harrington
 */
@Plugin(type = Command::class, menuRoot = "SciView", menu = [Menu(label = "View", weight = VIEW), Menu(label = "Start recording video", weight = VIEW_START_RECORDING_VIDEO)])
class StartRecordingVideo : Command {
    @Parameter
    private lateinit var sciView: SciView

    @Parameter
    private var bitrate = 10000000 // 10 MBit

    @Parameter(choices = ["VeryLow", "Low", "Medium", "High", "Ultra", "Insane"], style = "listBox")
    private lateinit var videoEncodingQuality // listed as an enum here, cant access from java https://github.com/scenerygraphics/scenery/blob/1a451c2864e5a48e47622d9313fe1681e47d7958/src/main/kotlin/graphics/scenery/utils/H264Encoder.kt#L65
            : String

    @Parameter(style="save")
    private lateinit var filename: File

    @Parameter(label="Use hardware encoding?")
    private var useHWEncoding = true
    // Note that this parameter is affirmative, while scenery's is a negation. We flip this later

    @Parameter(label="Overwrite existing?")
    private var overwrite = false

    override fun run() {
        bitrate = max(0, bitrate)
        sciView.getScenerySettings().set("VideoEncoder.Bitrate", bitrate)
        sciView.getScenerySettings().set("VideoEncoder.Quality", videoEncodingQuality)
        sciView.getScenerySettings().set("VideoEncoder.DisableHWEncoding", !useHWEncoding)
        sciView.toggleRecordVideo(filename.absolutePath, overwrite)
    }
}
package sc.iview.commands.demo.advanced

import org.joml.Quaternionf
import org.joml.Vector3f

/**
 * Data class to store metadata for spines of the hedgehog.
 */
data class SpineMetadata(
		val timepoint: Int,
		val origin: Vector3f,
		val direction: Vector3f,
		val distance: Float,
		val localEntry: Vector3f,
		val localExit: Vector3f,
		val localDirection: Vector3f,
		val headPosition: Vector3f,
		val headOrientation: Quaternionf,
		val position: Vector3f,
		val confidence: Float,
		val samples: List<Float>,
		val samplePosList: List<Vector3f> = ArrayList()
)
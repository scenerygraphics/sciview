package sc.iview.commands.demo.advanced

import org.joml.Vector3f
import org.joml.Matrix4f
import org.joml.Quaternionf
import graphics.scenery.utils.LazyLogger
import graphics.scenery.utils.extensions.*
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.math.sqrt

/**
 * <Description>
 *
 * @author Ulrik Günther <hello@ulrik.is>
 */
class HedgehogAnalysis(val spines: List<SpineMetadata>, val localToWorld: Matrix4f) {

	private val logger by LazyLogger()
	
	val timepoints = LinkedHashMap<Int, ArrayList<SpineMetadata>>()

	var avgConfidence = 0.0f
		private set
	var totalSampleCount = 0
		private set

	data class Track(
		val points: List<Pair<Vector3f, SpineGraphVertex>>,
		val confidence: Float
	)

	init {
		logger.info("Starting analysis with ${spines.size} spines")

		spines.forEach { spine ->
			val timepoint = spine.timepoint
			val current = timepoints[timepoint]

			if(current == null) {
				timepoints[timepoint] = arrayListOf(spine)
			} else {
				current.add(spine)
			}

			avgConfidence += spine.confidence
			totalSampleCount++
		}

		avgConfidence /= totalSampleCount
	}

	private fun localMaxima(list: List<Float>): List<Pair<Int, Float>> =
			list.windowed(3, 1).mapIndexed { index, l ->
				val left = l[0]
				val center = l[1]
				val right = l[2]

				// we have a match at center
				if(left - center < 0 && center - right > 0) {
					index + 1 to center
				} else {
					null
				}
			}.filterNotNull()

	data class SpineGraphVertex(val timepoint: Int,
								val position: Vector3f,
								val worldPosition: Vector3f,
								val index: Int,
								val value: Float,
								val metadata : SpineMetadata,
								var previous: SpineGraphVertex? = null,
								var next: SpineGraphVertex? = null) {

		fun distance(): Float {
			val n = next
			return if(n != null) {
				val t = (n.worldPosition - this.worldPosition)
				sqrt(t.x*t.x + t.y*t.y + t.z*t.z)
			} else {
				0.0f
			}
		}

		fun drop() {
			previous?.next = next
			next?.previous = previous
		}

		override fun toString() : String {
			return "SpineGraphVertex for t=$timepoint, pos=$position,index=$index, worldPos=$worldPosition, value=$value"
		}
	}

	fun Iterable<Float>.stddev() = sqrt((this.map { (it - this.average()) * (it - this.average()) }.sum() / this.count()))

	fun Vector3f.toQuaternionf(forward: Vector3f = Vector3f(0.0f, 0.0f, -1.0f)): Quaternionf {
		val cross = forward.cross(this)
		val q = Quaternionf(cross.x(), cross.y(), cross.z(), this.dot(forward))

		val x = sqrt((q.w + sqrt(q.x*q.x + q.y*q.y + q.z*q.z + q.w*q.w)) / 2.0f)

		return Quaternionf(q.x/(2.0f * x), q.y/(2.0f * x), q.z/(2.0f * x), x)
	}

	fun run(): Track? {

		val startingThreshold = 0.002f
		val localMaxThreshold = 0.001f
		val zscoreThreshold = 2.0f
		val removeTooFarThreshold = 5.0f

		if(timepoints.isEmpty()) {
			return null
		}


		//step1: find the startingPoint by using startingthreshold
		val startingPoint = timepoints.entries.firstOrNull { entry ->
			entry.value.any { metadata -> metadata.samples.filterNotNull().any { it > startingThreshold } }
		} ?: return null

		logger.info("Starting point is ${startingPoint.key}/${timepoints.size} (threshold=$startingThreshold)")

		timepoints.filter { it.key > startingPoint.key }
				.forEach { timepoints.remove(it.key) }

		logger.info("${timepoints.size} timepoints left")


		//step2: find the maxIndices along the spine
		val candidates = timepoints.map { tp ->
			val vs = tp.value.mapIndexedNotNull { i, spine ->
				val maxIndices = localMaxima(spine.samples.filterNotNull())
//				logger.info("Local maxima at ${tp.key}/$i are: ${maxIndices.joinToString(",")}")

				if(maxIndices.isNotEmpty()) {
					maxIndices.
//                  filter the maxIndices which are too far away, which can be removed
					filter { it.first <1200}.
					map { index ->
						val position = Vector3f(spine.localEntry).add((Vector3f(spine.localDirection).mul(index.first.toFloat())))
						val worldPosition = localToWorld.transform((Vector3f(position)).xyzw()).xyz()
						SpineGraphVertex(tp.key,
								position,
								worldPosition,
								index.first,
								index.second,
								spine)

					}
				} else {
					null
				}
			}
			vs
		}.flatten()


		//step3: connect localMaximal points between 2 candidate spines according to the shortest path principle
		// get the initial vertex, this one is assumed to always be in front, and have a local max
		val initial = candidates.first().filter{it.value>startingThreshold}.first()
		var current = initial
		var shortestPath = candidates.drop(1).mapIndexedNotNull { time, vs ->
			val distances = vs
					.filter { it.value > localMaxThreshold }
					.map { vertex ->
						val t = current.worldPosition - vertex.worldPosition
						val distance = t.length()
						vertex to distance
					}
					.sortedBy { it.second }

			val closest = distances.firstOrNull()?.first
			if(closest != null && distances.firstOrNull()?.second!! >0) {
				current.next = closest
				closest.previous = current
				current = closest
				current
			} else {
				null
			}
		}.toMutableList()


		val beforeCount = shortestPath.size
		var avgPathLength = shortestPath.map { it.distance() }.average().toFloat()
		var stdDevPathLength = shortestPath.map { it.distance() }.stddev().toFloat()
		logger.info("Average path length=$avgPathLength, stddev=$stdDevPathLength")

		fun zScore(value: Float, m: Float, sd: Float) = ((value - m)/sd)

		//step4: if some path is longer than multiple average length, it should be removed
		while (shortestPath.any { it.distance() >= removeTooFarThreshold * avgPathLength }) {
			shortestPath = shortestPath.filter { it.distance() < removeTooFarThreshold * avgPathLength }.toMutableList()
			shortestPath.windowed(3, 1, partialWindows = true).forEach {
				it.getOrNull(0)?.next = it.getOrNull(1)
				it.getOrNull(1)?.previous = it.getOrNull(0)
				it.getOrNull(1)?.next = it.getOrNull(2)
				it.getOrNull(2)?.previous = it.getOrNull(1)
			}

		}

		avgPathLength = shortestPath.map { it.distance() }.average().toFloat()
		stdDevPathLength = shortestPath.map { it.distance() }.stddev().toFloat()

		//step5: remove some edges according to zscoreThreshold
		var remaining = shortestPath.count { zScore(it.distance(), avgPathLength, stdDevPathLength) > zscoreThreshold }
		logger.info("Iterating: ${shortestPath.size} vertices remaining, with $remaining failing z-score criterion")
		while(remaining > 0) {
			val outliers = shortestPath
					.filter { zScore(it.distance(), avgPathLength, stdDevPathLength) > zscoreThreshold }
					.map {
						val idx = shortestPath.indexOf(it)
						listOf(idx-1,idx,idx+1)
					}.flatten()

			shortestPath = shortestPath.filterIndexed { index, _ -> index !in outliers }.toMutableList()
			remaining = shortestPath.count { zScore(it.distance(), avgPathLength, stdDevPathLength) > zscoreThreshold }

			shortestPath.windowed(3, 1, partialWindows = true).forEach {
				it.getOrNull(0)?.next = it.getOrNull(1)
				it.getOrNull(1)?.previous = it.getOrNull(0)
				it.getOrNull(1)?.next = it.getOrNull(2)
				it.getOrNull(2)?.previous = it.getOrNull(1)
			}
			logger.info("Iterating: ${shortestPath.size} vertices remaining, with $remaining failing z-score criterion")
		}

		val afterCount = shortestPath.size
		logger.info("Pruned ${beforeCount - afterCount} vertices due to path length")
		val singlePoints = shortestPath
				.groupBy { it.timepoint }
				.mapNotNull { vs -> vs.value.maxByOrNull{ it.metadata.confidence } }
				.filter {
					it.metadata.direction.dot(it.previous!!.metadata.direction) > 0.5f
				}


		logger.info("Returning ${singlePoints.size} points")


		return Track(singlePoints.map { it.position to it}, avgConfidence)
	}

	companion object {
		private val logger by LazyLogger(System.getProperty("scenery.LogLevel", "info"))

		fun fromIncompleteCSV(csv: File, separator: String = ","): HedgehogAnalysis {
			logger.info("Loading spines from incomplete CSV at ${csv.absolutePath}")

			val lines = csv.readLines()
			val spines = ArrayList<SpineMetadata>(lines.size)

			lines.drop(1).forEach { line ->
				val tokens = line.split(separator)
				val timepoint = tokens[0].toInt()
				val confidence = tokens[1].toFloat()
				val samples = tokens.subList(2, tokens.size - 1).map { it.toFloat() }

				val currentSpine = SpineMetadata(
						timepoint,
						Vector3f(0.0f),
						Vector3f(0.0f),
						0.0f,
						Vector3f(0.0f),
						Vector3f(0.0f),
						Vector3f(0.0f),
						Vector3f(0.0f),
						Quaternionf(),
						Vector3f(0.0f),
						confidence,
						samples)

				spines.add(currentSpine)
			}

			return HedgehogAnalysis(spines, Matrix4f())
		}

		private fun String.toVector3f(): Vector3f {
			val array = this.replace("(", "").replace(")", "").trim().split(" ").filterNot { it == ""}

			if (array[0] == "+Inf" || array[0] == "-Inf")
				return Vector3f(0.0f,0.0f,0.0f)

			return Vector3f(array[0].toFloat(),array[1].toFloat(),array[2].toFloat())
		}

		private fun String.toQuaternionf(): Quaternionf {
			val array = this.replace("(", "").replace(")", "").trim().split(" ").filterNot { it == ""}
			return Quaternionf(array[0].toFloat(), array[1].toFloat(), array[2].toFloat(), array[3].toFloat())
		}
		fun fromCSVWithMatrix(csv: File, matrix4f: Matrix4f,separator: String = ";"): HedgehogAnalysis {
			logger.info("Loading spines from complete CSV with Matrix at ${csv.absolutePath}")

			val lines = csv.readLines()
			val spines = ArrayList<SpineMetadata>(lines.size)
			logger.info("lines number: " + lines.size)
			lines.drop(1).forEach { line ->
				val tokens = line.split(separator)
				val timepoint = tokens[0].toInt()
				val origin = tokens[1].toVector3f()
				val direction = tokens[2].toVector3f()
				val localEntry = tokens[3].toVector3f()
				val localExit = tokens[4].toVector3f()
				val localDirection = tokens[5].toVector3f()
				val headPosition = tokens[6].toVector3f()
				val headOrientation = tokens[7].toQuaternionf()
				val position = tokens[8].toVector3f()
				val confidence = tokens[9].toFloat()
				val samples = tokens.subList(10, tokens.size - 1).map { it.toFloat() }

				val currentSpine = SpineMetadata(
						timepoint,
						origin,
						direction,
						0.0f,
						localEntry,
						localExit,
						localDirection,
						headPosition,
						headOrientation,
						position,
						confidence,
						samples)

				spines.add(currentSpine)
			}

			return HedgehogAnalysis(spines, matrix4f)
		}

		fun fromCSV(csv: File, separator: String = ";"): HedgehogAnalysis {
			logger.info("Loading spines from complete CSV at ${csv.absolutePath}")

			val lines = csv.readLines()
			val spines = ArrayList<SpineMetadata>(lines.size)

			lines.drop(1).forEach { line ->
				val tokens = line.split(separator)
				val timepoint = tokens[0].toInt()
				val origin = tokens[1].toVector3f()
				val direction = tokens[2].toVector3f()
				val localEntry = tokens[3].toVector3f()
				val localExit = tokens[4].toVector3f()
				val localDirection = tokens[5].toVector3f()
				val headPosition = tokens[6].toVector3f()
				val headOrientation = tokens[7].toQuaternionf()
				val position = tokens[8].toVector3f()
				val confidence = tokens[9].toFloat()
				val samples = tokens.subList(10, tokens.size - 1).map { it.toFloat() }

				val currentSpine = SpineMetadata(
						timepoint,
						origin,
						direction,
						0.0f,
						localEntry,
						localExit,
						localDirection,
						headPosition,
						headOrientation,
						position,
						confidence,
						samples)

				spines.add(currentSpine)
			}

			return HedgehogAnalysis(spines, Matrix4f())
		}
	}
}

fun main(args: Array<String>) {
	val logger = LoggerFactory.getLogger("HedgehogAnalysisMain")

	val file = File("C:\\Users\\lanru\\Desktop\\BionicTracking-generated-2021-11-29 19.37.43\\Hedgehog_1_2021-11-29 19.38.32.csv")
	val analysis = HedgehogAnalysis.fromCSV(file)
	val results = analysis.run()
	logger.info("Results: \n$results")
}

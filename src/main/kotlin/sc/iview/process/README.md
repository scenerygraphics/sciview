# SciView Volume Data Access Utilities

This module provides utilities for accessing the original data stored in Volume objects.

## VolumeUtils

The `VolumeUtils` class provides static methods for accessing volume data:

- `getOriginalRandomAccessibleInterval(volume)`: Returns the original RandomAccessibleInterval data stored in a Volume.
- `getCurrentView(volume)`: Returns an IntervalView representing the current timepoint of a volume.

## Volume Extensions

For more convenient usage, extension methods are provided for the Volume class:

- `volume.getOriginalRandomAccessibleInterval<T>()`: Extension method to get the original data.
- `volume.getCurrentView<T>()`: Extension method to get the current view based on the timepoint.

## Usage Example

Here's a simple example of using these utilities to apply a marching cubes algorithm to a Volume:

```kotlin
// Get the current Volume
val volume = sciView.activeNode as Volume

// Get the current view based on the timepoint
val view = volume.getCurrentView<UnsignedByteType>()

// Apply your processing
val bitImg = ops.threshold().apply(view, UnsignedByteType(1)) as Img<BitType>
val mesh = ops.geom().marchingCubes(bitImg, 1.0, BitTypeVertexInterpolator())

// Convert to scenery mesh
val sceneryMesh = MeshConverter.toScenery(mesh)

// Add to scene
sciView.addNode(sceneryMesh)
```

See the `VolumeMarchingCubes` command for a complete example.

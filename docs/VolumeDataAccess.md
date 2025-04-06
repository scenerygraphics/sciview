# Accessing Original Data from Volumes in SciView

This document provides an overview of how to access the original data from a Volume in SciView.

## Background

In SciView, a Volume node encapsulates volumetric data but doesn't provide direct access to the underlying data structures needed for processing algorithms like marching cubes. Previously, developers had to use metadata with "magic" keys to access this data, which was not intuitive or type-safe.

## New Methods

We've added utility methods and extension functions to provide cleaner access to the underlying data:

### Using Extension Functions (Recommended)

```kotlin
// Get the active node (assuming it's a Volume)
val volume = sciView.activeNode as? Volume
if (volume == null) {
    // Handle error
    return
}

// Get the original RandomAccessibleInterval
val originalData = volume.getOriginalRandomAccessibleInterval()

// Get the current view (at the current timepoint)
val currentView = volume.getCurrentView()

// Now you can use the data with algorithms like marching cubes
// For example, using the current view with a marching cubes algorithm
val meshes = MarchingCubesRealType.calculate(currentView, threshold)
```

### Using the VolumeData Utility Class

Alternatively, you can use the VolumeData utility class directly:

```kotlin
// Get the original data
val originalData = VolumeData.getOriginalRandomAccessibleInterval(volume)

// Get the current view
val currentView = VolumeData.getCurrentView(volume)
```

## Working with Time Series

For time series data, the `getCurrentView()` method automatically returns the slice at the current timepoint. This avoids the need to manually create a hyperslice like this:

```kotlin
// Old way (no longer needed)
val rai = volume.metadata["RandomAccessibleInterval"] as RandomAccessibleInterval<*>
val tp = volume.currentTimepoint
val view = Views.hyperSlice(rai, 3, tp.toLong())
```

The new approach is simpler:

```kotlin
// New way
val view = volume.getCurrentView()
```

## Type Safety

The returned RandomAccessibleInterval is untyped (`RandomAccessibleInterval<*>`). You'll need to cast it to the appropriate type based on your data:

```kotlin
// Cast to specific type (e.g., UnsignedByteType)
val typedView = volume.getCurrentView() as? RandomAccessibleInterval<UnsignedByteType>
if (typedView == null) {
    // Handle error
    return
}
```

## Examples

See the demo commands in the `sc.iview.commands.demo` package for complete examples:

- `MarchingCubesFromVolumeCommand` - Shows a simple implementation using the new methods
- `MarchingCubesOriginalExample` - Shows how the original example from issue #607 can be simplified

## Benefits

- No more "magic" keys to remember
- Clear, intention-revealing method names
- Automatic handling of time series data
- Better integration with IDE code completion

[![Build status](https://github.com/scenerygraphics/sciView/workflows/build/badge.svg)](https://github.com/scenerygraphics/sciView/actions?workflow=build)
[![Image.sc Forum](https://img.shields.io/badge/dynamic/json.svg?label=forum&url=https%3A%2F%2Fforum.image.sc%2Ftags%2Fsciview.json&query=%24.topic_list.tags.0.topic_count&colorB=brightgreen&suffix=%20topics&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAABPklEQVR42m3SyyqFURTA8Y2BER0TDyExZ+aSPIKUlPIITFzKeQWXwhBlQrmFgUzMMFLKZeguBu5y+//17dP3nc5vuPdee6299gohUYYaDGOyyACq4JmQVoFujOMR77hNfOAGM+hBOQqB9TjHD36xhAa04RCuuXeKOvwHVWIKL9jCK2bRiV284QgL8MwEjAneeo9VNOEaBhzALGtoRy02cIcWhE34jj5YxgW+E5Z4iTPkMYpPLCNY3hdOYEfNbKYdmNngZ1jyEzw7h7AIb3fRTQ95OAZ6yQpGYHMMtOTgouktYwxuXsHgWLLl+4x++Kx1FJrjLTagA77bTPvYgw1rRqY56e+w7GNYsqX6JfPwi7aR+Y5SA+BXtKIRfkfJAYgj14tpOF6+I46c4/cAM3UhM3JxyKsxiOIhH0IO6SH/A1Kb1WBeUjbkAAAAAElFTkSuQmCC)](https://forum.image.sc/tags/sciview)
[![Join the chat](https://img.shields.io/badge/zulip-join_chat-brightgreen.svg)](https://imagesc.zulipchat.com/#narrow/stream/327495-scenery.2Bsciview)

# sciview - interactive visualisation in vr for fiji & imagej2

SciView is an ImageJ/FIJI plugin for 3D visualization of images and meshes. It uses [scenery](https://github.com/scenerygraphics/scenery) as a rendering backend and supports rendering to VR headsets via [OpenVR](https://github.com/ValveSoftware/OpenVR).

![sciview running the Game of Life 3D demo](https://ulrik.is/sciview-gameoflife.gif)

sciview running the [Game of Life 3D](https://en.wikipedia.org/wiki/Game_of_Life) demo (see `Demo > Game of Life 3D`).

[Documentation](https://docs.scenery.graphics/sciview/) is being developed.

## Adding to Fiji

Detailed [installation instructions](https://docs.scenery.graphics/sciview/installation/installing-the-sciview-plugin-for-fiji) can be found in the [sciview gitbook](https://docs.scenery.graphics/sciview/).

Should you experience any issues, [please try the latest development version](https://docs.scenery.graphics/sciview/installation/installing-the-development-sciview-plugin-for-fiji), which includes all the latest features and fixes. 

## Quick Start

![Overview of sciview's user interface](https://gblobscdn.gitbook.com/assets%2F-LqBCy3SBefXis0YnrcI%2F-MK5WLQvMLIvw2GF6Rn2%2F-MK5WMGzmSavDTwlGro2%2Fmain-cheatsheet.jpg?alt=media&token=70c82549-e939-4752-af12-1756492a5f01)

## API Features

### Custom Window Dimensions

SciView now supports setting custom window dimensions via API, which is essential for VR headsets that require specific resolutions:

```kotlin
// Create SciView with custom dimensions
val sciview = SciView.create(1920, 1080)

// Or resize an existing instance
sciview.setWindowSize(2880, 1700)  // Example: Oculus Quest 2 resolution

// Query current dimensions
val (width, height) = sciview.getWindowSize()
```

This feature is particularly useful for:
- VR headset integration requiring exact resolutions
- Multi-monitor setups
- Creating screenshots or recordings at specific resolutions
- Kiosk or presentation modes

## Developers

[Kyle Harrington](https://kyleharrington.com), University of Idaho & [Ulrik Guenther](https://ulrik.is/writing), MPI-CBG

## Contributors

* Curtis Rueden (University of Wisconsin, Madison)
* Aryaman Gupta (MPI-CBG)
* Tobias Pietzsch (MPI-CBG)
* Robert Haase (MPI-CBG)
* Jan Eglinger (FMI Basel)
* Stephan Saalfeld (HHMI Janelia Farm)

## Citation

In case you use sciview or scenery in a scientific publication, please cite this paper:

* Ulrik Günther, Tobias Pietzsch, Aryaman Gupta, Kyle I.S. Harrington, Pavel Tomancak, Stefan Gumhold, and Ivo F. Sbalzarini: scenery — Flexible Virtual Reality Visualisation on the Java VM. _IEEE VIS 2019 (accepted, [arXiv:1906.06726](https://arxiv.org/abs/1906.06726))_.

## Resources

- [Media/videos created with SciView](https://github.com/scenerygraphics/sciview/wiki/Links-to-media-created-with-SciView)

## DevOps

**Triggering uploads to the update site:**

Add either `[SV_IJ_DEPLOY_UNSTABLE]` or `[SV_IJ_DEPLOY_PRIMARY]` to a commit message

## Acknowledgements

This software contains the following 3rd party libraries:

* IntelliJ UI code, by JetBrains (licensed under the Apache Software License 2.0), see `src/main/java/com/intellij`
* trove4j, by the Trove4j team (licensed under the Lesser GNU Public License 3.0), see `src/main/java/gnu`


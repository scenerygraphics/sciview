[![Image.sc forum](https://img.shields.io/badge/dynamic/json.svg?label=forum&url=https%3A%2F%2Fforum.image.sc%2Ftags%2Fsciview.json&query=%24.topic_list.tags.0.topic_count&colorB=brightgreen&suffix=%20topics&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAOCAYAAAAfSC3RAAABPklEQVR42m3SyyqFURTA8Y2BER0TDyExZ+aSPIKUlPIITFzKeQWXwhBlQrmFgUzMMFLKZeguBu5y+//17dP3nc5vuPdee6299gohUYYaDGOyyACq4JmQVoFujOMR77hNfOAGM+hBOQqB9TjHD36xhAa04RCuuXeKOvwHVWIKL9jCK2bRiV284QgL8MwEjAneeo9VNOEaBhzALGtoRy02cIcWhE34jj5YxgW+E5Z4iTPkMYpPLCNY3hdOYEfNbKYdmNngZ1jyEzw7h7AIb3fRTQ95OAZ6yQpGYHMMtOTgouktYwxuXsHgWLLl+4x++Kx1FJrjLTagA77bTPvYgw1rRqY56e+w7GNYsqX6JfPwi7aR+Y5SA+BXtKIRfkfJAYgj14tpOF6+I46c4/cAM3UhM3JxyKsxiOIhH0IO6SH/A1Kb1WBeUjbkAAAAAElFTkSuQmCC)](https://forum.image.sc/tags/sciview)
[![](https://travis-ci.org/scenerygraphics/sciview.svg?branch=master)](https://travis-ci.org/scenerygraphics/sciview)
[![Join the chat at https://gitter.im/scenerygraphics/SciView](https://badges.gitter.im/scenerygraphics/SciView.svg)](https://gitter.im/scenerygraphics/SciView?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

# sciview - interactive visualisation in vr for fiji & imagej2

Formerly known as _ThreeDViewer_, which really confused people.

SciView is an ImageJ/FIJI plugin for 3D visualization of images and meshes. It uses the [scenery](https://github.com/ClearVolume/scenery) as a rendering backend and supports rendering to VR headsets via [OpenVR](https://github.com/ValveSoftware/OpenVR).

![sciview running the Game of Life 3D demo](https://ulrik.is/sciview-gameoflife.gif)

sciview running the [Game of Life 3D](https://en.wikipedia.org/wiki/Game_of_Life) demo (see `Demo > Game of Life 3D`).

## Adding to Fiji

In Fiji, go to `Help -> Update` and activate the `SciView` update site. In case you need to add it manually, the address is `https://sites.imagej.net/SciView`.

## Developers

[Kyle Harrington](https://www.capsidaho.com), University of Idaho & [Ulrik Guenther](https://ulrik.is/writing), MPI-CBG

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

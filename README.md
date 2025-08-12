# QuPath Warpy extension

This repo adds some support for nonlinear deformation and registration in QuPath, by using the [ImgLib2](https://github.com/imglib/imglib2) library.

## Documentation

[https://imagej.net/plugins/bdv/warpy/warpy](https://imagej.net/plugins/bdv/warpy/warpy)

## Installing

In QuPath 0.6, this extension should be installed via the [QuPath BIOP catalog](https://github.com/BIOP/qupath-biop-catalog).

Don't forget to restart QuPath (but not your computer).

## Citing

https://doi.org/10.3389/fcomp.2021.780026

## Building

You can build the QuPath Warpy extension from source with

```bash
gradlew clean build
```

The output will be under `build/libs`.

* `clean` removes anything old
* `build` builds the QuPath extension as a *.jar* file and adds it to `libs` 

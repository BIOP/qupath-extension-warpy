# QuPath Warpy extension

This repo adds some support for non linear deformation and registration in QuPath, by using the [ImgLib2](https://github.com/imglib/imglib2) library.

## Installing

Download the latest `qupath-extension-warpy-[version].zip` file from [releases](https://github.com/biop/qupath-extension-warpy/releases). Unzip it, then and drag its contained files onto the main QuPath window.

If you haven't installed any extensions before, you'll be prompted to select a QuPath user directory.
The extension will then be copied to a location inside that directory.

You might then need to restart QuPath (but not your computer).

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

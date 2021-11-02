# QuPath Warpy extension

This repo adds some support for non linear deformation and registration in QuPath, by using the Imglib2 library.

## Installing

Download the latest `qupath-extension-warpy-[version].jar` file from [releases](https://github.com/biop/qupath-extension-warpy/releases) and drag it onto the main QuPath window.

If you haven't installed any extensions before, you'll be prompted to select a QuPath user directory.
The extension will then be copied to a location inside that directory.

You might then need to restart QuPath (but not your computer).

TODO : explain hwo to install extra needed jars

## Citing

TODO

## Building

You can build the QuPath Warpy extension from source with

```bash
gradlew clean build
```

The output will be under `build/libs`.

* `clean` removes anything old
* `build` builds the QuPath extension as a *.jar* file and adds it to `libs` 

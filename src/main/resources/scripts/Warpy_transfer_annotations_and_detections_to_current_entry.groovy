/**
 * Transfer PathObjects to the currently open image from candidate entries that have been registered with Warpy
 *
 * REQUIREMENTS
 * ============
 * You need to:
 *  - have the QuPath Warpy Extension installed (https://github.com/BIOP/qupath-extension-warpy)
 *  - have an opened image which has been registered with Warpy (https://imagej.net/plugins/bdv/warpy/warpy)
 *  - have some objects detected on one of the images, and have the other image currently active in the viewer
 *
 * This script then transfer all objects from the moving to the fixed image (or vice versa)
 *
 * Support for annotations, detections, cells. TMAs are handled separately, see the appropriate script.

 * @author Nicolas Chiaruttini
 * @author Olivier Burri
 *
 */

// Transfer PathObjects from another image that contains a serialized RealTransform
// result from Warpy

// The current Image Entry that we want to transfer PathObjects to
def targetEntry = getProjectEntry()

// Locate candidate entries can can be transformed into the source entry
def sourceEntries = Warpy.getCandidateSourceEntries( targetEntry )

// Warn in case multiple candidate entries
if(sourceEntries.size() > 1) {
    logger.warn( "Multiple candidate entries found, using the first one" )
}
// Choose one source or transfer from all of them with a for loop
def sourceEntry = sourceEntries[0]

// Recover the RealTransform that was put there by Warpy
def transform = Warpy.getRealTransform( sourceEntry, targetEntry )

// Recover the objects we wish to transform into the target image
// This step ensures you can have control over what gets transferred
def objectsToTransfer = Warpy.getPathObjectsFromEntry( sourceEntry )

// Finally perform the transform of each PathObject
def transferredObjects = Warpy.transformPathObjects( objectsToTransfer, transform )

// Convenience method to add intensity measurements. Does not have to do with transforms directly.
// This packs the addIntensityMeasurements in such a way that it works for RGB and Fluoresence images
def downsample = 1
Warpy.addIntensityMeasurements(transferredObjects, downsample)

// Finally, add the transformed objects to the current image and update the display
addObjects(transferredObjects)

// Make sure that we can see the new objects
fireHierarchyUpdate()

println "Warpy anotations and detections transfer script done"

// Necessary import, requires qupath-extenstion-warpy, see: https://github.com/BIOP/qupath-extension-warpy
import qupath.ext.biop.warpy.*

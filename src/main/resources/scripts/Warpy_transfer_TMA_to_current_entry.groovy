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
 * This script then transfer all TMA objects and all child objects from the moving to the fixed image (or vice versa)
 *
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

// ####### TMAs #######
// If you have TMAs, what we do is import the original TMA grid from the source entry, modify the ROIs,
// transfer all child objects, and finally replace the current TMA grid with the imported one
def tma = Warpy.getTMAGridFromEntry( sourceEntry )
def newGrid = Warpy.transferTMAGrid( tma, transform )

// Transfer the modified grid, including all children
getCurrentHierarchy().setTMAGrid(newGrid)

// Adding intensity measurements to TMA
def downsample = 1
Warpy.addIntensityMeasurements(newGrid.getTMACoreList(), downsample)

// Make sure that we can see the new objects
fireHierarchyUpdate()

println "Warpy anotations and detections transfer script done"

// Necessary import, requires qupath-extenstion-warpy, see: https://github.com/BIOP/qupath-extension-warpy
import qupath.ext.biop.warpy.*

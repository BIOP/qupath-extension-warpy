package qupath.ext.warpy;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.imglib2.RealPoint;
import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSerializer;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Geometry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.analysis.features.ObjectMeasurements;
import qupath.lib.gui.QuPathApp;
import qupath.lib.images.ImageData;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.TransformedServerBuilder;
import qupath.lib.objects.*;
import qupath.lib.objects.hierarchy.TMAGrid;
import qupath.lib.projects.Project;
import qupath.lib.projects.ProjectImageEntry;
import qupath.lib.roi.GeometryTools;
import qupath.lib.roi.ROIs;
import qupath.lib.roi.interfaces.ROI;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static qupath.lib.scripting.QP.*;

/**
 * Utility class which bridges real transformation of the ImgLib2 world and
 * makes it easily usable into JTS world, mainly used by QuPath
 * <p>
 * See initial forum thread : <a href="https://forum.image.sc/t/qupath-arbitrarily-transform-detections-and-annotations/49674">...</a>
 * For documentation regarding this tool, see <a href="https://c4science.ch/w/bioimaging_and_optics_platform_biop/image-processing/wsi_registration_fjii_qupath/">...</a>
 * <p>
 * Extra dependencies required for QuPath:
 * <p>
 * \--- net.imglib2:imglib2-realtransform:3.1.1
 * +--- net.imglib2:imglib2:5.10.0
 * +--- gov.nist.math:jama:1.0.3
 * \--- jitk:jitk-tps:3.0.1
 * +--- com.googlecode.efficient-java-matrix-library:ejml:0.24
 * \--- log4j:log4j:1.2.17
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 * @author Olivier Burri, EPFL, 2021
 */

public class Warpy {

    // Logger class that plays well with QuPath
    final private static Logger logger = LoggerFactory.getLogger(Warpy.class);

    // Pattern to match the transform file
    private static final Pattern transformFilePattern = Pattern.compile("transform_(?<target>\\d+)_(?<source>\\d+)\\.json");

    /**
     * Recovers a list of candidate entries in this project that have a RealTransform file that matches the pattern in
     * 'transformFilePattern'
     * @param targetEntry the entry which should *receive* transformed objects. Typically the active entry.
     * @return a collection of project entries that have a useable and valid RealTransform (forward or valid inverse)
     */
    public static Collection<ProjectImageEntry<?>> getCandidateSourceEntries(ProjectImageEntry<?> targetEntry) {

        Project<?> project = getProject();
        // Find in the targetfolder, the source entries that have a Serialized RealTransform file
        List<? extends ProjectImageEntry<?>> entries = project.getImageList();

        String targetID = targetEntry.getID();

        // Find if there is a forward transform file and return the entry source
        Path targetEntryPath = targetEntry.getEntryPath();
        Collection<ProjectImageEntry<?>> candidateTransformableEntries = new ArrayList<>();
        for (File currentFile : Objects.requireNonNull(targetEntryPath.toFile().listFiles())) {
            Matcher matcher = transformFilePattern.matcher(currentFile.getName());
            if (matcher.matches()) {
                if (matcher.group("target").equals(targetID)) {
                    // Check the source ID and return if
                    String sourceID = matcher.group("source");
                    ProjectImageEntry<?> sourceEntry = getEntryFromID(sourceID);
                    if (sourceEntry != null) candidateTransformableEntries.add(sourceEntry);
                }
            }
        }

        // Find is there are inverse transforms available by going through all entries
        for (ProjectImageEntry<?> entry : entries) {
            if (!entry.equals(targetEntry)) {
                for (File currentFile : Objects.requireNonNull(entry.getEntryPath().toFile().listFiles())) {
                    Matcher matcher = transformFilePattern.matcher(currentFile.getName());
                    if (matcher.matches()) {
                        if (matcher.group("source").equals(targetID)) {
                            // Check the source ID and return it
                            String inverseSourceID = matcher.group("target");
                            ProjectImageEntry<?> inverseSourceEntry = getEntryFromID(inverseSourceID);
                            if (inverseSourceEntry != null) {
                                // Try to get the transform and see if it works
                                RealTransform rt = getRealTransform(currentFile);
                                // This rt should be inverted if we are to use it with this target image
                                if (rt instanceof InvertibleRealTransform) {
                                    // It is invertible, so we can add it as a candidate
                                    candidateTransformableEntries.add(inverseSourceEntry);
                                } else {
                                    logger.info("Found a candidate transform from {} to {}, but it is not invertible. Skipping", targetEntry.getImageName(), inverseSourceEntry.getImageName());
                                }
                            }
                        }
                    }
                }
            }
        }

        // If the list of Paths is not empty, then we have candidates, yay!
        logger.info("Found {} candidate entries for image {}", candidateTransformableEntries.size(), targetEntry.getImageName());
        return candidateTransformableEntries;
    }

    /**
     * Convenience method to recover the entire hierarchy from a project entry
     *
     * @param sourceEntry the entry from which we want to extract the objects
     * @return a collection of PathObjects, in hierarchical form
     */
    public static Collection<PathObject> getPathObjectsFromEntry(ProjectImageEntry<?> sourceEntry) {
        try {
            // Do not return the TMA cores
            return sourceEntry.readHierarchy().getRootObject().getChildObjects().stream().filter(Predicate.not(PathObject::isTMACore)).collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error reading hierarchy from " + sourceEntry.getImageName(), e);
        }
        return null;
    }

    public static TMAGrid getTMAGridFromEntry(ProjectImageEntry<?> sourceEntry) {
        try {
            return sourceEntry.readHierarchy().getTMAGrid();
        } catch (IOException e) {
            logger.error("Error reading TMA grid from " + sourceEntry.getImageName(), e);
        }
        return null;
    }

    /**
     * Transfer the TMA grid from one entry to another
     *
     */
    public static TMAGrid transferTMAGrid(TMAGrid originalGrid, RealTransform transform) {
        CoordinateSequenceFilter transformer = getJTSFilter(transform);


        // The TMA grid will be modified in place and all its objects will be modified
        originalGrid.getTMACoreList().forEach(core -> {

            // For the core, build a new ellipse based on the bounds of the transformed original ellipse
            ROI original_roi = core.getROI();

            Geometry geometry = original_roi.getGeometry();

            GeometryTools.attemptOperation(geometry, (g) -> {
                g.apply(transformer);
                return g;
            });

            // Convert to a new ellipse
            ROI transformed_roi = GeometryTools.geometryToROI(geometry, original_roi.getImagePlane());
            ROI newEllipse = ROIs.createEllipseROI(transformed_roi.getBoundsX(), transformed_roi.getBoundsY(), transformed_roi.getBoundsWidth(), transformed_roi.getBoundsHeight(), original_roi.getImagePlane());

            core.setROI(newEllipse);
            // Now we need to add every object that is inside the TMA Grid
            Collection<PathObject> allObjects = core.getChildObjects();

            Collection<PathObject> newObjects = transformPathObjects(allObjects, transform);

            // Remove all objects from the core
            core.clearChildObjects();

            // Re-add
            core.addChildObjects(newObjects);

        });

        // This is the original grid, to avoid having to recreate everything...
        return originalGrid;
    }

    /**
     * Performs the actual transformation of the desired PathObjects. The transformation goes through all the children
     * of each PathObject and replicates the hierarchy of the original pathObject collection
     *
     * @param objects the source objects to transform
     * @param transform the realtransform to use, acquired using {@link #getRealTransform(ProjectImageEntry, ProjectImageEntry)}
     * @return the same collection of objects (unless some could not be warped) with the same hierarchy
     */
    public static Collection<PathObject> transformPathObjects(Collection<PathObject> objects, RealTransform transform) {

        // Make JTS transformer
        CoordinateSequenceFilter transformer = getJTSFilter(transform);

        // Transforms all objects and add them to a new list
        return objects.stream().map(o-> transformPathObjectAndChildren(o, transformer, true, true) )
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Internal method to ensure an easy way to get an ProjectImageEntry from its ID
     *
     * @param id the ID to get the ImageEntry from
     * @return the corresponding ProjectImageEntry or null if none is found
     */
    private static ProjectImageEntry<?> getEntryFromID(String id) {
        for (ProjectImageEntry<?> entry : getProject().getImageList()) {
            if (entry.getID().equals(id)) return entry;
        }
        return null;
    }

    /**
     * Way to obtain the transform from a source (which has the objects we wish to transfer) to a target (which will receive the new objects)
     *
     * @param sourceEntry the source from which to transform, can be selected using {@link #getCandidateSourceEntries(ProjectImageEntry)}
     * @param targetEntry the target which will the source will be transformed into. This is needed so that when searching for valid transforms,
     *                    Warpy can work out if the serialized transform is a forward one or an inverse transform.
     * @return the RealTransform to use for warping pathObjects
     */
    public static RealTransform getRealTransform(ProjectImageEntry<?> sourceEntry, ProjectImageEntry<?> targetEntry) {

        // Search Forward
        String targetID = targetEntry.getID();
        String sourceID = sourceEntry.getID();
        Path targetEntryPath = targetEntry.getEntryPath();
        for (File currentFile : Objects.requireNonNull(targetEntryPath.toFile().listFiles())) {
            Matcher matcher = transformFilePattern.matcher(currentFile.getName());
            if (matcher.matches()) {
                if (matcher.group("target").equals(targetID)) {
                    // Check the source ID and return if it is a match
                    String potentialSourceID = matcher.group("source");
                    if (sourceID.equals(potentialSourceID)) {
                        return getRealTransform(currentFile);
                    }
                }
            }
        }

        // Search Backwards
        Path sourceEntryPath = sourceEntry.getEntryPath();
        for (File currentFile : Objects.requireNonNull(sourceEntryPath.toFile().listFiles())) {
            Matcher matcher = transformFilePattern.matcher(currentFile.getName());
            if (matcher.matches()) {
                if (matcher.group("source").equals(targetID)) {
                    // Check the source ID and return if it is a match
                    String potentialTargetID = matcher.group("target");
                    if (sourceID.equals(potentialTargetID)) {
                        RealTransform rt = getRealTransform(currentFile);
                        if (rt instanceof InvertibleRealTransform) {
                            return ((InvertibleRealTransform) rt).inverse();
                        } else {
                            logger.error("Could not invert transform from file {}. This error should not exist.", currentFile.getAbsolutePath());
                            return null;
                        }
                    }
                }
            }
        }
        // We found nothing
        return null;
    }

    /**
     * Convenience method to add intensity measurements to an image, regardless of whether it is fluorescent or brightfield.
     * NOTE: We do not hand
     *
     * @param objects the object to add the intensity measurements to
     * @param downsample downsample factor to choose the resolution at which to measure the intensity features
     * @throws Exception an error in case that the objects could not be measured
     */
    public static void addIntensityMeasurements(Collection<PathObject> objects, double downsample) throws Exception {

        ImageServer<BufferedImage> server = (ImageServer<BufferedImage>) getCurrentServer();
        //If the image is RGB, this line can be added to import the correct measurements (DAB, etc.):
        //cf https://forum.image.sc/t/transferring-segmentation-predictions-from-custom-masks-to-qupath/43408/15
        ImageData.ImageType type = getProjectEntry().readImageData().getImageType();

        if (type.equals(ImageData.ImageType.BRIGHTFIELD_H_DAB) ||
                type.equals(ImageData.ImageType.BRIGHTFIELD_H_E) ||
                type.equals(ImageData.ImageType.BRIGHTFIELD_OTHER)) {
            server = new TransformedServerBuilder(server)
                    .deconvolveStains(getCurrentImageData().getColorDeconvolutionStains(), 1, 2)
                    .build();
        }

        addIntensityMeasurements(objects, server, downsample);
    }

    /**
     * Add the standard measurements to the newly created path objects and server
     *
     * @param objects the object to add the intensity measurements to
     * @param server the ImageServer from which to get the intensities
     * @param downsample downsample factor to choose the resolution at which to measure the intensity features
     * @throws Exception an error in case that the objects could not be measured
     */
    public static void addIntensityMeasurements(Collection<PathObject> objects, ImageServer<BufferedImage> server, double downsample) throws Exception {

        List<ObjectMeasurements.Measurements> measurements = Arrays.asList(ObjectMeasurements.Measurements.values());// as List
        List<ObjectMeasurements.Compartments> compartments = Arrays.asList(ObjectMeasurements.Compartments.values());// as List // Won't mean much if they aren't cells...

        for (PathObject object : objects) {
            if (object instanceof PathDetectionObject) {
                ObjectMeasurements.addIntensityMeasurements(server, object, downsample, measurements, compartments);
            }
            addIntensityMeasurements(object.getChildObjects(), server, downsample);
        }
    }

    /**
     * Recursive approach to transform a PathObject and all its children based on the provided CoordinateSequenceFilter
     * see {@link #transformPathObject(PathObject, CoordinateSequenceFilter, boolean, boolean)}
     *
     * @param object           qupath annotation or detection object
     * @param transform        jts free form transformation
     * @param copyMeasurements whether to transfer all the source PathObject Measurements to the resulting PathObject
     */
    private static PathObject transformPathObjectAndChildren(PathObject object, CoordinateSequenceFilter transform, boolean checkGeometryValidity, boolean copyMeasurements) {

        PathObject transformedObject = null;
        try {
            transformedObject = transformPathObject(object, transform, checkGeometryValidity, copyMeasurements);
        } catch (Exception e) {
            logger.error("Could not transform object {}, error is {}", object, e.getLocalizedMessage());
        }

        if (object.hasChildObjects() && transformedObject != null ) {
            logger.info("Transforming {}", object);
            List<PathObject> children = object.getChildObjects().stream()
                    .map(child -> transformPathObjectAndChildren(child, transform, checkGeometryValidity, copyMeasurements))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            transformedObject.addChildObjects(children);
        }

        return transformedObject;
    }

    /**
     * Returns a transformed PathObject (Annotation or detection) based
     * on the original geometry of the input path object
     *
     * @param object           qupath annotation or detection object
     * @param transform        jts free form transformation
     * @param copyMeasurements whether or not to transfer all the source PathObject Measurements to the resulting PathObject
     */
    private static PathObject transformPathObject(PathObject object, CoordinateSequenceFilter transform, boolean checkGeometryValidity, boolean copyMeasurements) throws Exception {

        ROI original_roi = object.getROI();

        Geometry geometry = original_roi.getGeometry();

        GeometryTools.attemptOperation(geometry, (g) -> {
            g.apply(transform);
            return g;
        });

        // Handle the case of a cell
        if (checkGeometryValidity) {
            if (!geometry.isValid()) {
                throw new Exception("Invalid geometry for transformed object" + object);
            }
        }
        // TODO comment a bit more
        ROI transformed_roi = GeometryTools.geometryToROI(geometry, original_roi.getImagePlane());

        PathObject transformedObject;
        switch (object) {
            case PathAnnotationObject pathAnnotationObject ->
                    transformedObject = PathObjects.createAnnotationObject(transformed_roi, pathAnnotationObject.getPathClass(), copyMeasurements ? object.getMeasurementList() : null);
            case PathCellObject pathCellObject -> {
                // Need to transform the nucleus as well
                ROI original_nuc = pathCellObject.getNucleusROI();
                ROI transformed_nuc_roi = null;
                if (original_nuc != null) {

                    Geometry nuc_geometry = original_nuc.getGeometry();

                    GeometryTools.attemptOperation(nuc_geometry, (g) -> {
                        g.apply(transform);
                        return g;
                    });
                    transformed_nuc_roi = GeometryTools.geometryToROI(nuc_geometry, original_roi.getImagePlane());
                }
                transformedObject = PathObjects.createCellObject(transformed_roi, transformed_nuc_roi, object.getPathClass(), copyMeasurements ? object.getMeasurementList() : null);
            }
            case PathDetectionObject pathDetectionObject ->
                    transformedObject = PathObjects.createDetectionObject(transformed_roi, pathDetectionObject.getPathClass(), copyMeasurements ? object.getMeasurementList() : null);
            default -> throw new Exception("Unknown PathObject class for class " + object.getClass().getSimpleName());
        }

        // Return the same ID as the original object
        // Add the name and ID here
        transformedObject.setName(object.getName());
        transformedObject.setID(object.getID());
        transformedObject.setLocked(object.isLocked());

        return transformedObject;
    }

    /**
     * Uses {@link RealTransformSerializer} to deserialize a RealTransform object
     *
     * @param f file to deserialize
     * @return an imglib2 RealTransform object
     */
    public static RealTransform getRealTransform(File f) {
        FileReader fileReader;
        try {
            fileReader = new FileReader(f.getAbsolutePath());
            JsonObject element = new Gson().fromJson(fileReader, JsonObject.class);
            fileReader.close();
            element = (JsonObject) RealTransformSerializer.fixAffineTransform(element); // Fix missing type element in old versions
            return RealTransformSerializer.getRealTransformAdapter().fromJson(element, RealTransform.class);
        } catch (FileNotFoundException e) {
            logger.error("Transform file " + f.getName() + " not found", e);
        } catch (IOException e) {
            logger.error("Error reading transform file " + f.getName(), e);
        }

        return null;
    }

    /**
     * Gets an imglib2 realtransform object and returned the equivalent
     * JTS {@link CoordinateSequenceFilter} operation which can be applied to
     * {@link Geometry}.
     * <p>
     * The 3rd dimension is ignored.
     *
     * @param rt imglib2 realtransform object
     * @return the equivalent JTS {@link CoordinateSequenceFilter} operation which can be applied to {@link Geometry}.
     */
    public static CoordinateSequenceFilter getJTSFilter(RealTransform rt) {
        return new CoordinateSequenceFilter() {
            @Override
            public void filter(CoordinateSequence seq, int i) {
                RealPoint pt = new RealPoint(3);
                pt.setPosition(seq.getOrdinate(i, 0), 0);
                pt.setPosition(seq.getOrdinate(i, 1), 1);
                rt.apply(pt, pt);
                seq.setOrdinate(i, 0, pt.getDoublePosition(0));
                seq.setOrdinate(i, 1, pt.getDoublePosition(1));
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public boolean isGeometryChanged() {
                return true;
            }
        };
    }

    /**
     * Main class for debugging
     * Can be used to start a QuPath project with a project already opened
     * @param args some inputs we do not need
     */
    public static void main(String... args) {
        String projectPath = "\\\\svfas6.epfl.ch\\biop\\public\\luisa.spisak_UPHUELSKEN\\Overlay\\qp\\project.qpproj";
        QuPathApp.launch(QuPathApp.class);//, projectPath);
    }
}

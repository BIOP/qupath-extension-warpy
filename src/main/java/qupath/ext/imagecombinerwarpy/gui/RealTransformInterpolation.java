package qupath.ext.imagecombinerwarpy.gui;

import net.imglib2.realtransform.RealTransform;
import qupath.ext.warpy.WarpyExtension;

/**
 * This file is part of ImageCombinerWarpy a QuPath extension based on the QuPath 'Interactive Image Alignment' tool
 * <p>
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * <p>
 * This class serves as a structure to store the central imglib2 RealTransform object that is a function that transform
 * coordinates from one source image server to a target one. Input and outputs coordinates are expressed in pixel
 * coordinates of the source and target image server.
 * <p>
 * On top of the transformation object itself this class serves as a placeholder for properties that will affect the
 * creation of the transformed image:
 * - {@link RealTransformInterpolation#interpolation} sets how the target image interpolates the pixels of the source image
 * - {@link RealTransformInterpolation#downsampleTransformation()} flags whether the transformation field is interpolated
 * between pixels
 * - if the transformation field is interpolated, {@link RealTransformInterpolation#getTransformationDownsampling()}
 * indicates between each pixel of the FULL RESOLUTION target image server the transformation is computed exactly. The
 * field is then interpolated between these points.
 * <p>
 * An object of this class will be serialized as part of a  {@link RealTransformImageServer} object.
 * <p>
 * Peter Haub (@phaub), Oct 2021
 * Nicolas Chiaruttini (@phaub), Apr 2025
 */
public class RealTransformInterpolation {

	public static final int[] interpolationsModes = InterpolationModes.getOrdinalNumbers();
	public static final String[] interpolationsModeNames = InterpolationModes.getInterpolationTypeName();

	// Stores the version of the Warpy repository when it has been serialized. Useful if backward compatibility breaks
	String version;

	// This field refers to the underlying image interpolation, a better name would be imageInterpolation, but this would break backward compatibility
	final private int interpolation;

	// Whether the transformation is interpolated. If true, the field is computed only exactly at square positions:
	//  - [(0,0):(transformationFieldDownsampling, transformationFieldDownsampling):(server.getWidth():server.getHeight())] and interpolated in between these points
	final private boolean interpolateTransformation;

	// Downsampling for transformation computation in pixel units. Ignored if interpolateTransformationField is false
	final private int transformationDownsampling;

	// Imglib2 realtransform object.
	final private RealTransform transform;
	
	public RealTransformInterpolation(RealTransformInterpolation transformInterpolateSequence) {
		version = WarpyExtension.getWarpyVersion();
		int interpolation = transformInterpolateSequence.getInterpolation();
		if (isValidInterpolation(interpolation)) {
			this.interpolation = interpolation;
		} else {
			this.interpolation = 0; // Nearest neighbor interpolation by default
		}
		this.transform = transformInterpolateSequence.getTransform();
		this.interpolateTransformation = transformInterpolateSequence.downsampleTransformation();
		this.transformationDownsampling = transformInterpolateSequence.getTransformationDownsampling();
	}

	public RealTransformInterpolation(RealTransform transform, int interpolation, boolean interpolateTransformation, int transformationDownsampling) {
		version = WarpyExtension.getWarpyVersion();
		if (isValidInterpolation(interpolation)) {
			this.interpolation = interpolation;
		} else {
			this.interpolation = 0; // Nearest neighbor interpolation by default
		}
		this.transformationDownsampling = transformationDownsampling;
		this.interpolateTransformation = interpolateTransformation;
		this.transform = transform;
	}

	public RealTransform getTransform() {
		return transform;
	}
	
	public int getInterpolation() {
		return interpolation;
	}

	public String getVersion() {
		return version;
	}

	public int getTransformationDownsampling() {
		return transformationDownsampling;
	}

	public boolean downsampleTransformation() {
		return interpolateTransformation;
	}

	public String getInterpolationName() {
		return interpolationsModeNames[interpolation];
	}

	private static boolean isValidInterpolation(int interpolation) {
        for (int interpolationsMode : interpolationsModes) {
            if (interpolationsMode == interpolation)
                return true;
        }
		return false;
	}
}

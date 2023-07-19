/*********************************
 * This file is part of ImageCombinerWarpy ...
 * 
 * .. a QuPath extension based on the QuPath 'Interactive Image Alignment' tool
 *  
 * The ImageCombinerWarpy is thought as an experimental tool.
 * 
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Peter Haub (@phaub), Oct 2021
 * 
 *********************************/

package qupath.ext.imagecombinerwarpy.gui;

import net.imglib2.realtransform.RealTransform;
import qupath.ext.biop.warpy.Warpy;
import qupath.ext.biop.warpy.WarpyExtension;

public class RealTransformInterpolation {

	transient public static final int[] interpolationsModes = InterpolationModes.getOrdinalNumbers();
	transient public static final String[] interpolationsModeNames = InterpolationModes.getInterpolationTypeName();
	
	private int interpolation = 0;
	
	private RealTransform transform;

	String version;

	public RealTransformInterpolation() {
		version = WarpyExtension.getWarpyVersion();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}
	
	public RealTransformInterpolation(RealTransformInterpolation transformInterpolateSequence) {
		version = WarpyExtension.getWarpyVersion();
		int interpolation = transformInterpolateSequence.getInterpolation();
		if (isValidInterpolation(interpolation))
			this.interpolation = interpolation;
		this.transform = transformInterpolateSequence.getTransform();
	}

	public RealTransformInterpolation(RealTransform transform, int interpolation) {
		version = WarpyExtension.getWarpyVersion();
		if (isValidInterpolation(interpolation))
			this.interpolation = interpolation;

		this.transform = transform;
	}

	public RealTransform getTransform() {
		return transform;
	}
	
	public void setTransform(RealTransform transform) {
		this.transform = transform;
	}
	
	public int getInterpolation() {
		return interpolation;
	}

	public String getInterpolationName() {
		return interpolationsModeNames[interpolation];
	}
	
	public void setInterpolation(int interpolation) {
		this.interpolation = interpolation;
	}

	private boolean isValidInterpolation(int interpolation) {
		for (int i=0; i<interpolationsModes.length; i++) {
			if (interpolationsModes[i] == interpolation)
				return true;
		}
		return false;
	}
}

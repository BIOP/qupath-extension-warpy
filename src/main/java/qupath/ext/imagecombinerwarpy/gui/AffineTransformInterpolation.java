/*********************************
 * This file is part of ImageCombinerWarpy ...
 * 
 * .. a QuPath extension based on the QuPath 'Interactive Image Alignment' tool
 *  
 * The ImageCombinerWarpy is thought as an experimental² tool.
 * 
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *
 * Peter Haub (@phaub), Oct 2021
 * 
 *********************************/

package qupath.ext.imagecombinerwarpy.gui;

import java.awt.geom.AffineTransform;

public class AffineTransformInterpolation {

	public static final int[] interpolationsModes = InterpolationModes.getOrdinalNumbers();
	public static final String[] interpolationsModeNames = InterpolationModes.getInterpolationTypeName();
	
	private int interpolation = 0;
	
	private AffineTransform transform;
	
	public AffineTransformInterpolation(AffineTransformInterpolation transforminterpolate) {
		int interpolation = transforminterpolate.getInterpolation();
		if (isValidInterpolation(interpolation))
			this.interpolation = interpolation;
		this.transform = transforminterpolate.getTransform();
	}

	public AffineTransformInterpolation(AffineTransform transform, int interpolation) {
		if (isValidInterpolation(interpolation))
			this.interpolation = interpolation;
		this.transform = transform;
	}

	public AffineTransform getTransform() {
		return transform;
	}
	
	public int getInterpolation() {
		return interpolation;
	}

	public String getInterpolationName() {
		return interpolationsModeNames[interpolation];
	}
	
	private boolean isValidInterpolation(int interpolation) {
		for (int i=0; i<interpolationsModes.length; i++) {
			if (interpolationsModes[i] == interpolation)
				return true;
		}
		return false;
	}
}

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

public class InterpolationModes {

	public enum InterpolationType {
		NEARESTNEIGHBOR, BILINEAR, BICUBIC, CATMULLROM, MITCHELLNETRAVALI, CUBICBSPLINE;

		@Override
		public String toString() {
			switch(this) {
			case NEARESTNEIGHBOR:
				return "NearestNeighbor";
			case BILINEAR:
				return "Bilinear";
			case BICUBIC:
				return "Bicubic";
			case CATMULLROM:
				return "Catmull-Rom";
			case MITCHELLNETRAVALI:
				return "Mitchell-Netravali";
			case CUBICBSPLINE:
				return "Cubic B-spline";
			}
			throw new IllegalArgumentException("Unknown interpolation type " + this);
		}
	}

	public static InterpolationType getInterpolationType(int ordinal) {
		return InterpolationType.values()[ordinal];
	}
	
	public static int[] getOrdinalNumbers() {
		int[] ordinals = new int[InterpolationType.values().length];
		for (int i=0; i<ordinals.length; i++)
			ordinals[i] = i;
		return ordinals;
	}

	public static String[] getInterpolationTypeName() {
		InterpolationType[] types = InterpolationType.values();
		String[] names = new String[types.length];
		for (int i=0; i<names.length; i++)
			names[i] = types[i].toString();
		return names;
	}

	public static boolean isValidInterpolation(InterpolationType interpolationtype) {
		if (!interpolationtype.toString().equals(""))
			return true;
		return false;
	}

}

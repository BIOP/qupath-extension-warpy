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

public class InterpolationHelper {

	static float k1 = (float) (2.0/6.0);
	static float k2 = (float) (1.0/6.0);
	static float k3 = (float) (8.0/6.0);
	
	// Portion from Burger&Burge, Digital Image Processing, 2010
	// https://en.wikipedia.org/wiki/Bicubic_interpolation
	public static float cubic(float x, float a, float b) {
		//float a = 1, b = 0;  // Bicubic
		//float a = 0.5, b = 0;  // Catmull-Rom
		//float a = (float) (1/3.0), b = a;	// Mitchell-Netravali
		//float a = 0, b = 1;	// Cubic B-spline
		
		if (x < 0) x = -x;
		float z = 0;
		if (x < 1)
			z = (-a - 1.5f*b + 2)*x*x*x + (a + 2*b - 3)*x*x + 1 - k1;
		else if (x < 2)
			z = (-a - k2*b)*x*x*x + (5*a + b)*x*x + (-8*a - 2*b)*x + 4*a + k3*b;
		return z;
	}
		
	public static float[] convertToFloatArray(int[] in, float[] out) {
		if ( in == null )
			return null;
		if (out == null || out.length != in.length)
			out = new float[in.length];
		
		for (int i=0; i<out.length; i++)
			out[i] = in[i] & 0xffffffff;
		
		return out;
	}

	public static float[] convertToFloatArray(short[] in, float[] out) {
		if ( in == null )
			return null;
		if (out == null || out.length != in.length)
			out = new float[in.length];
		
		for (int i=0; i<out.length; i++)
			out[i] = in[i] & 0xffff;
		
		return out;
	}
	
	public static float[] convertToFloatArray(byte[] in, float[] out) {
		if ( in == null )
			return null;
		if (out == null || out.length != in.length)
			out = new float[in.length];
		
		for (int i=0; i<out.length; i++)
			out[i] = in[i] & 0xff;
		
		return out;
	}
	

}

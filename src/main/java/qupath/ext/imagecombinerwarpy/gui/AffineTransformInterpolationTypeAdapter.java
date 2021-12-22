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

import java.awt.geom.AffineTransform;
import java.io.IOException;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class AffineTransformInterpolationTypeAdapter extends TypeAdapter<AffineTransformInterpolation> {	
	
	public static AffineTransformInterpolationTypeAdapter INSTANCE = new AffineTransformInterpolationTypeAdapter();
	
	private static Gson gson = new Gson();

	@Override
	public void write(JsonWriter out, AffineTransformInterpolation value) throws IOException {
		gson.toJson(new AffineTransformInterpolationProxy(value), AffineTransformInterpolationProxy.class, out);
	}

	@Override
	public AffineTransformInterpolation read(JsonReader in) throws IOException {
		AffineTransformInterpolationProxy proxy = gson.fromJson(in, AffineTransformInterpolationProxy.class);
		var transform = new AffineTransform();
		proxy.fill(transform);
		int interpolate = proxy.getInterpolation();
		return new AffineTransformInterpolation(transform, interpolate);
	}
	
	static class AffineTransformInterpolationProxy {
		
		public final double m00, m10, m01, m11, m02, m12, mInterpolate;
		
		AffineTransformInterpolationProxy(AffineTransformInterpolation transforminterpolate) {
			double[] flatmatrix = new double[6];
			transforminterpolate.getTransform().getMatrix(flatmatrix);
	        m00 = flatmatrix[0];
	        m10 = flatmatrix[1];
	        m01 = flatmatrix[2];
	        m11 = flatmatrix[3];
            m02 = flatmatrix[4];
            m12 = flatmatrix[5];
            mInterpolate = transforminterpolate.getInterpolation();
		}
		
		void fill(AffineTransform transform) {
			transform.setTransform(m00, m10, m01, m11, m02, m12);
		}
		
		int getInterpolation() {
			return (int)mInterpolate;
		}
	}

}

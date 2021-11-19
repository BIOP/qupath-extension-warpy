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

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.imglib2.realtransform.RealTransformDeSerializer_ICW;

public class RealTransformInterpolationTypeAdapter extends TypeAdapter<RealTransformInterpolationSequence> {	
	public static RealTransformInterpolationTypeAdapter INSTANCE = new RealTransformInterpolationTypeAdapter();
	
	@Override
	public void write(JsonWriter out, RealTransformInterpolationSequence value) throws IOException {
		RealTransformDeSerializer_ICW.getRealTransformAdapter().toJson((Object)value, value.getClass(), out);		
	}

	@Override
	public RealTransformInterpolationSequence read(JsonReader in) throws IOException {
		RealTransformInterpolationSequence realtransformInterpolation = RealTransformDeSerializer_ICW.deserializeInterpolationSequence(in, RealTransformInterpolationSequence.class);		
		return realtransformInterpolation;
	}
}

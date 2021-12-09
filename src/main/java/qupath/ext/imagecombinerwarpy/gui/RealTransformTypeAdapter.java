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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.imglib2.realtransform.RealTransform;
import net.imglib2.realtransform.RealTransformSerializer;

public class RealTransformTypeAdapter extends TypeAdapter<RealTransform> {	
	public static RealTransformTypeAdapter INSTANCE = new RealTransformTypeAdapter();
	
	@Override
	public void write(JsonWriter out, RealTransform value) {
		RealTransformSerializer.getRealTransformAdapter().toJson(value, value.getClass(), out);
	}

	@Override
	public RealTransform read(JsonReader in) {
		RealTransform realtransform = RealTransformSerializer.deserialize(in, RealTransform.class);
		return realtransform;
	}
}

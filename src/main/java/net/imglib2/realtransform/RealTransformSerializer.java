package net.imglib2.realtransform;

import com.google.gson.*;

import jitk.spline.ThinPlateR2LogRSplineKernelTransform;
import net.imglib2.FinalRealInterval;
import net.imglib2.realtransform.inverse.WrappedIterativeInvertibleRealTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.biop.warpy.Warpy;
import qupath.ext.biop.warpy.WarpyExtension;
import qupath.ext.imagecombinerwarpy.gui.RealTransformInterpolation;
import qupath.lib.io.GsonTools;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/**
 * Imglib2 Real transforms adapters already used in bigdataviewer-playground
 * copied again to avoid to import too many dependencies
 * //::dip
 * {@link RealTransformInterpolationAdapter} for usage with the ImageCombinerWarpy (ICW)
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 * @author Peter Haub, ::dip, 2021
 */

public class RealTransformSerializer {

    private static Logger logger = LoggerFactory.getLogger(RealTransformSerializer.class);

    public static void addRealTransformAdapters(GsonBuilder builder) {
        GsonTools.SubTypeAdapterFactory<RealTransform> factoryRealTransform = GsonTools.createSubTypeAdapterFactory(RealTransform.class, "type");
        factoryRealTransform.registerSubtype(ThinplateSplineTransform.class);
        factoryRealTransform.registerSubtype(Wrapped2DTransformAs3D.class);
        factoryRealTransform.registerSubtype(WrappedIterativeInvertibleRealTransform.class);
        factoryRealTransform.registerSubtype(RealTransformSequence.class);
        factoryRealTransform.registerSubtype(InvertibleRealTransformSequence.class);
        factoryRealTransform.registerSubtype(BoundedRealTransform.class);
        factoryRealTransform.registerSubtype(AffineTransform3D.class);

        builder.registerTypeAdapterFactory(factoryRealTransform);
        builder.registerTypeHierarchyAdapter(ThinplateSplineTransform.class, new ThinPlateSplineTransformAdapter());
        builder.registerTypeHierarchyAdapter(Wrapped2DTransformAs3D.class, new Wrapped2DTransformAs3DRealTransformAdapter());
        builder.registerTypeHierarchyAdapter(WrappedIterativeInvertibleRealTransform.class, new WrappedIterativeInvertibleRealTransformAdapter());
        builder.registerTypeHierarchyAdapter(RealTransformSequence.class, new RealTransformSequenceAdapter());
        builder.registerTypeHierarchyAdapter(InvertibleRealTransformSequence.class, new InvertibleRealTransformSequenceAdapter());
        builder.registerTypeHierarchyAdapter(BoundedRealTransform.class, new BoundedRealTransformAdapter());
        builder.registerTypeHierarchyAdapter(AffineTransform3D.class, new AffineTransform3DAdapter());
    }

    public static Gson getRealTransformAdapter() {
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        addRealTransformAdapters(builder);
        return builder.create();
    }
    
    //::dip
    public static class RealTransformInterpolationAdapter implements JsonSerializer<RealTransformInterpolation>,
    JsonDeserializer<RealTransformInterpolation> {

		@Override
		public RealTransformInterpolation deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
		    JsonObject obj = jsonElement.getAsJsonObject();
		    int interpolation = obj.get("interpolation").getAsInt();
		    RealTransformInterpolation rtis = new RealTransformInterpolation();
		    rtis.setInterpolation(interpolation);
            String version = obj.get("version").getAsString();
            rtis.setVersion(version);
            if (!version.equals(WarpyExtension.getWarpyVersion())) {
                logger.warn("Warpy version "+WarpyExtension.getWarpyVersion()+" different from ImageServer "+version);
            }
            RealTransform transform = RealTransformSerializer.getRealTransformAdapter().fromJson(obj.get("transform"), RealTransform.class);
            rtis.setTransform(transform);
		    return rtis;
		}
		
		@Override
		public JsonElement serialize(RealTransformInterpolation rtis, Type type, JsonSerializationContext jsonSerializationContext) {
		    JsonObject obj = new JsonObject();
            obj.addProperty("interpolation", rtis.getInterpolation());
            obj.addProperty("version", rtis.getVersion());
            obj.add("transform", jsonSerializationContext.serialize(rtis.getTransform(), RealTransform.class));
		    return obj;
		}
    }

    public static class ThinPlateSplineTransformAdapter implements JsonSerializer<ThinplateSplineTransform>,
            JsonDeserializer<ThinplateSplineTransform> {

        @Override
        public ThinplateSplineTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();
            double[][] srcPts = context.deserialize(obj.get("srcPts"), double[][].class);
            double[][] tgtPts = context.deserialize(obj.get("tgtPts"), double[][].class);
            return new ThinplateSplineTransform(srcPts, tgtPts);
        }

        @Override
        public JsonElement serialize(ThinplateSplineTransform thinplateSplineTransform, Type type, JsonSerializationContext jsonSerializationContext) {
            ThinPlateR2LogRSplineKernelTransform kernel = getKernel(thinplateSplineTransform);

            assert kernel != null;
            double[][] srcPts = getSrcPts(kernel);
            double[][] tgtPts = getTgtPts(kernel);

            JsonObject obj = new JsonObject();
            obj.add("srcPts", jsonSerializationContext.serialize(srcPts));
            obj.add("tgtPts", jsonSerializationContext.serialize(tgtPts));
            return obj;
        }

        public static ThinPlateR2LogRSplineKernelTransform getKernel(ThinplateSplineTransform thinplateSplineTransform) {
            try {
                Field kernelField = ThinplateSplineTransform.class.getDeclaredField("tps");
                kernelField.setAccessible(true);
                return (ThinPlateR2LogRSplineKernelTransform) kernelField.get(thinplateSplineTransform);
            } catch (Exception e) {
                e.printStackTrace();
            }
            logger.error("Could not get kernel from ThinplateSplineTransform");
            return null;
        }

        public static double[][] getSrcPts(ThinPlateR2LogRSplineKernelTransform kernel) {
            return kernel.getSourceLandmarks();
        }

        public static double[][] getTgtPts(ThinPlateR2LogRSplineKernelTransform kernel) {
            double[][] srcPts = kernel.getSourceLandmarks(); // srcPts

            int nbLandmarks = kernel.getNumLandmarks();
            int nbDimensions = kernel.getNumDims();

            double[][] tgtPts = new double[nbDimensions][nbLandmarks];

            for (int i = 0;i<nbLandmarks;i++) {
                double[] srcPt = new double[nbDimensions];
                for (int d = 0; d<nbDimensions; d++) {
                    srcPt[d] = srcPts[d][i];
                }
                double[] tgtPt = kernel.apply(srcPt);
                for (int d = 0; d<nbDimensions; d++) {
                    tgtPts[d][i] = tgtPt[d];
                }
            }
            return tgtPts;
        }
    }

    public static class Wrapped2DTransformAs3DRealTransformAdapter implements JsonSerializer<Wrapped2DTransformAs3D>,
            JsonDeserializer<Wrapped2DTransformAs3D> {

        @Override
        public Wrapped2DTransformAs3D deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();
            RealTransform rt = jsonDeserializationContext.deserialize(obj.get("wrappedTransform"), RealTransform.class);

            if (!(rt instanceof InvertibleRealTransform)) {
                logger.error("Wrapped transform not invertible -> deserialization impossible...");
                // TODO : see if autowrapping works ?
                return null;
            }
            return new Wrapped2DTransformAs3D((InvertibleRealTransform) rt);
        }

        @Override
        public JsonElement serialize(Wrapped2DTransformAs3D wrapped2DTransformAs3D, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            obj.add("wrappedTransform", jsonSerializationContext.serialize(wrapped2DTransformAs3D.getTransform(), RealTransform.class));
            return obj;
        }

    }

    public static class BoundedRealTransformAdapter implements JsonSerializer<BoundedRealTransform>,
            JsonDeserializer<BoundedRealTransform> {

        @Override
        public BoundedRealTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();

            RealTransform rt = jsonDeserializationContext.deserialize(obj.get("realTransform"), RealTransform.class);

            if (!(rt instanceof InvertibleRealTransform)) {
                logger.error("Error during deserialization of BoundedRealTransform : The serialized transform is not invertible");
                return null;
            }

            double[] min = jsonDeserializationContext.deserialize(obj.get("interval_min"), double[].class);
            double[] max = jsonDeserializationContext.deserialize(obj.get("interval_max"), double[].class);
            FinalRealInterval fri = new FinalRealInterval(min, max);
            return new BoundedRealTransform((InvertibleRealTransform) rt, fri);
        }

        @Override
        public JsonElement serialize(BoundedRealTransform brt, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            FinalRealInterval fri = new FinalRealInterval(brt.getInterval());
            obj.add("realTransform", jsonSerializationContext.serialize(brt.getTransform(), RealTransform.class));
            obj.add("interval_min", jsonSerializationContext.serialize(fri.minAsDoubleArray()));
            obj.add("interval_max", jsonSerializationContext.serialize(fri.maxAsDoubleArray()));
            return obj;
        }

    }

    public static class WrappedIterativeInvertibleRealTransformAdapter implements JsonSerializer<WrappedIterativeInvertibleRealTransform>,
            JsonDeserializer<WrappedIterativeInvertibleRealTransform> {

        @Override
        public WrappedIterativeInvertibleRealTransform deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();
            RealTransform rt = jsonDeserializationContext.deserialize(obj.get("wrappedTransform"), RealTransform.class);
            WrappedIterativeInvertibleRealTransform ixfm = new WrappedIterativeInvertibleRealTransform<>(rt);
            ixfm.getOptimzer().setTolerance( 0.000001 );   // keeps running until error is < 0.000001
            ixfm.getOptimzer().setMaxIters( 1000 ); // or 1000 iterations
            return ixfm;
        }

        @Override
        public JsonElement serialize(WrappedIterativeInvertibleRealTransform wrappedIterativeInvertibleRealTransform, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            obj.add("wrappedTransform", jsonSerializationContext.serialize(wrappedIterativeInvertibleRealTransform.getTransform(), RealTransform.class));
            return obj;
        }
    }

    public static class AffineTransform3DAdapter implements JsonSerializer<AffineTransform3D>,
            JsonDeserializer<AffineTransform3D> {

        @Override
        public AffineTransform3D deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            double[] rowPackedCopy =
                    jsonDeserializationContext.deserialize(jsonElement.getAsJsonObject().get("affinetransform3d"), double[].class);
            AffineTransform3D at3d = new AffineTransform3D();
            at3d.set(rowPackedCopy);
            return at3d;
        }

        @Override
        public JsonElement serialize(AffineTransform3D affineTransform3D, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            obj.add("affinetransform3d", jsonSerializationContext.serialize(affineTransform3D.getRowPackedCopy()));
            return obj;
        }
    }

    public static class RealTransformSequenceAdapter implements JsonSerializer<RealTransformSequence>,
            JsonDeserializer<RealTransformSequence> {

        @Override
        public RealTransformSequence deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();

            int nTransform = obj.get("size").getAsInt();
            RealTransformSequence rts = new RealTransformSequence();
            for (int iTransform = 0; iTransform<nTransform; iTransform++) {
                RealTransform transform = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), RealTransform.class);
                rts.add(transform);
            }

            return rts;
        }

        // Could not serialize because of RealTransformSequence#transforms field protected access,
        // this would need to change the class of package location, but serialization within QuPath is not required anyway
        // (for the moment)
        @Override
        public JsonElement serialize(RealTransformSequence rts, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            obj.addProperty("size", rts.transforms.size());
            for (int iTransform = 0; iTransform<rts.transforms.size(); iTransform++) {
                obj.add("realTransform_"+iTransform, jsonSerializationContext.serialize(rts.transforms.get(iTransform), RealTransform.class));
            }
            return obj;
        }
    }

    public static class InvertibleRealTransformSequenceAdapter implements JsonSerializer<InvertibleRealTransformSequence>,
            JsonDeserializer<InvertibleRealTransformSequence> {

        @Override
        public InvertibleRealTransformSequence deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject obj = jsonElement.getAsJsonObject();

            int nTransform = obj.get("size").getAsInt();

            InvertibleRealTransformSequence irts = new InvertibleRealTransformSequence();

            for (int iTransform = 0; iTransform<nTransform; iTransform++) {
                RealTransform transform = jsonDeserializationContext.deserialize(obj.get("realTransform_"+iTransform), RealTransform.class);
                if (transform instanceof InvertibleRealTransform) {
                    irts.add((InvertibleRealTransform) transform);
                } else {
                    logger.error("Deserialization error: "+transform+" of class "+transform.getClass().getSimpleName()+" is not invertible!");
                    return null;
                }
            }
            return irts;
        }

        @Override
        public JsonElement serialize(InvertibleRealTransformSequence irts, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject obj = new JsonObject();
            obj.addProperty("size", irts.transforms.size());
            for (int iTransform = 0; iTransform<irts.transforms.size(); iTransform++) {
                obj.add("realTransform_"+iTransform, jsonSerializationContext.serialize(irts.transforms.get(iTransform), RealTransform.class));
            }
            return obj;
        }
    }


    /**
     * Fix RealTransform serialization where type:"AffineTransform3D" has been missing
     * Function is there for legacy reasons.
     * @param element
     * @return a converted serialized tree where the affinetransform3d serialization has been fixed
     */
    public static JsonElement fixAffineTransform(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject object  = (JsonObject) element;
            object.entrySet().forEach(e -> {
                if (e.getKey().equals("affinetransform3d")) {
                    if (!object.keySet().contains("type")) { // If type is missing
                        object.addProperty("type", "AffineTransform3D"); // Let's add it!
                    }
                } else {
                    e.setValue(fixAffineTransform(e.getValue()));
                }
            });
        } else if (element.isJsonArray()) {
            JsonArray array = (JsonArray) element;
            JsonArray converted = new JsonArray();
            for (JsonElement e : array) {
                converted.add(fixAffineTransform(e));
            }
            element = converted;
        }
        return element;
    }

}

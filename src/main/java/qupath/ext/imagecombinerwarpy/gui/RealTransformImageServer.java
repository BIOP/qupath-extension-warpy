/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2018 - 2020 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

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
 * This is a modified version of AffineTransformImageServer.java by @author Pete Bankhead
 * @author Peter Haub
 * 
 *
 * Peter Haub (@phaub), Oct 2021
 * 
 *********************************/

package qupath.ext.imagecombinerwarpy.gui;

import java.awt.Polygon;
import java.awt.Shape;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.imglib2.realtransform.InvertibleRealTransform;
import net.imglib2.realtransform.RealTransform;
import qupath.lib.awt.common.AwtTools;
import qupath.ext.imagecombinerwarpy.gui.InterpolationModes.InterpolationType;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.images.servers.TransformingImageServer;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;

/**
 * ImageServer that dynamically applies an RealTransform to an existing ImageServer.
 * <p>
 * Warning! This is incomplete and will be changed/removed in the future.
 * 
 *
 */
public class RealTransformImageServer extends TransformingImageServer<BufferedImage> {
	
	private static Logger logger = LoggerFactory.getLogger(RealTransformImageServer.class);
	
	private ImageServerMetadata metadata;
	
	private transient ImageRegion region;
	private RealTransformInterpolation rtis;
	private RealTransform realtransform;
	private RealTransform realtransformInverse;
	
	private InterpolationType interpolationMode = InterpolationType.NEARESTNEIGHBOR;
	
	private double globalScale = 1.0;	
	private double[] dsLevels;
	
	
	protected RealTransformImageServer(final ImageServer<BufferedImage> server, RealTransformInterpolation rtis, int interpolation) throws NoninvertibleTransformException {
		super(server);
		
		logger.trace("Creating server for {} and Real transform {}", server, rtis);
				
		this.rtis = rtis;
		
		this.interpolationMode = InterpolationModes.getInterpolationType(rtis.getInterpolation());

		if (rtis.getTransform() instanceof InvertibleRealTransform) {
			this.realtransform = rtis.getTransform();
			this.realtransformInverse = ((InvertibleRealTransform) realtransform).inverse();
		}
		else {
			throw new NoninvertibleTransformException("realtransform not invertible");
		}

		double[][] bounds = new double[4][3];
		double[][] boundsTR = new double[4][3];
		int[] polyX = new int[4];
		int[] polyY = new int[4];
				
		bounds[1][0] = server.getWidth();
		bounds[2][1] = server.getHeight();
		bounds[3][0] = server.getWidth();
		bounds[3][1] = server.getHeight();
		
		for (int i=0; i<4; i++) {
			realtransformInverse.apply(bounds[i], boundsTR[i]);
			polyX[i] = (int)boundsTR[i][0];
			polyY[i] = (int)boundsTR[i][1];
		}		
		Rectangle2D boundsTransformed = (new Polygon(polyX, polyY, 4)).getBounds2D();
				
		double lOrig = Math.sqrt((double) (server.getWidth())*(double) (server.getWidth()) + (double) (server.getHeight())*(double) (server.getHeight()));
		double lRT = Math.sqrt((boundsTR[3][0]-boundsTR[0][0])*(boundsTR[3][0]-boundsTR[0][0]) + (boundsTR[3][1]-boundsTR[0][1])*(boundsTR[3][1]-boundsTR[0][1]));
		double scale = lRT / lOrig;

		PixelCalibration cal = server.getPixelCalibration();
		var calUpdated = cal.createScaledInstance(scale, scale);

		this.region = ImageRegion.createInstance(
				(int)boundsTransformed.getMinX(),
				(int)boundsTransformed.getMinY(),
				(int)Math.ceil(boundsTransformed.getWidth()),
				(int)Math.ceil(boundsTransformed.getHeight()), 0, 0);
		
		var levelBuilder = new ImageServerMetadata.ImageResolutionLevel.Builder(region.getWidth(), region.getHeight());
		boolean fullServer = server.getWidth() == region.getWidth() && server.getHeight() == region.getHeight();
		int i = 0;
		do {
			var originalLevel = server.getMetadata().getLevel(i);
			if (fullServer)
				levelBuilder.addLevel(originalLevel);
			else
				levelBuilder.addLevelByDownsample(originalLevel.getDownsample());
			i++;
		} while (i < server.nResolutions() && 
				region.getWidth() >= server.getMetadata().getPreferredTileWidth() && 
				region.getHeight() >= server.getMetadata().getPreferredTileHeight());
		
		// TODO: Apply RealTransform to pixel sizes! Perhaps create a Shape or point and transform that?
		var builder = new ImageServerMetadata.Builder(server.getMetadata())
				.width(region.getWidth())
				.height(region.getHeight())
				//.name(String.format("%s (%s)", server.getMetadata().getName(), realtransform.toString()))
				.name(String.format("%s (%s)", server.getMetadata().getName(), rtis.toString()))
				.levels(levelBuilder.build());
		
		// TODO: Handle pixel sizes in units other than microns
		if (!calUpdated.equals(server.getPixelCalibration())) {
			if (!calUpdated.equals(PixelCalibration.getDefaultInstance()))
				logger.debug("Pixel calibration updated to {}", calUpdated);
			builder.pixelSizeMicrons(calUpdated.getPixelWidthMicrons(), calUpdated.getPixelHeightMicrons());
		}
				
		metadata = builder.build();
		
		globalScale = scale;		
		dsLevels = server.getPreferredDownsamples();
	}
	

    private static Rectangle2D createTransformedBounds(Shape shape, RealTransform rtransform, int nSteps){
    	Rectangle2D bounds = shape.getBounds2D();
    	
    	double minX = Double.MAX_VALUE;
    	double minY = Double.MAX_VALUE;
    	double maxX = 0;
    	double maxY = 0;
    	
    	double[] p = new double[3];
    	double[] p2 = new double[3];
    	
    	double[][] ePair = new double[4][2];
    	
    	ePair[0][0] = bounds.getMinX(); 
    	ePair[0][1] = bounds.getMinY(); 
    	ePair[1][0] = bounds.getMaxX(); 
    	ePair[1][1] = bounds.getMinY(); 
    	ePair[2][0] = bounds.getMaxX(); 
    	ePair[2][1] = bounds.getMaxY(); 
    	ePair[3][0] = bounds.getMinX(); 
    	ePair[3][1] = bounds.getMaxY(); 

    	int e2;
    	for (int e=0; e<4; e++) {
    		e2 = e+1;
    		if (e2 > 3)
    			e2 = 0;
    		
    		double lx = ePair[e2][0] - ePair[e][0];
    		double ly = ePair[e2][1] - ePair[e][1];
    		double dx = lx / nSteps;
    		double dy = ly / nSteps;
    		
    		for (int n=0; n<=nSteps; n++) {
    			p[0] = ePair[e][0] + dx*n;
    			p[1] = ePair[e][1] + dy*n;
    			
    			rtransform.apply(p, p2);
    			
    			if (p2[0] < minX) minX = p2[0];
    			else if (p2[0] > maxX) maxX = p2[0];
    			if (p2[1] < minY) minY = p2[1];
    			else if (p2[1] > maxY) maxY = p2[1];    			
    		}
    	}
    			
		minX = Math.floor(minX);
		maxX = Math.ceil(maxX);
		minY = Math.floor(minY);
		maxY = Math.ceil(maxY);

		int[] polyX = {(int)minX, (int)maxX, (int)maxX, (int)minX,};
		int[] polyY = {(int)minY, (int)minY, (int)maxY, (int)maxY,};
		
		Rectangle2D boundsTransformed = (new Polygon(polyX, polyY, 4)).getBounds2D();
		
    	return boundsTransformed;
    }
 

	@Override
	protected String createID() {
		return getClass().getName() + ": + " + getWrappedServer().getPath() + " " + "realtransform"; //GsonTools.getInstance().toJson(realtransform, realtransform.getClass()); 
	}

	private static double getBestDownsample(double[] dsLevels, double ds) {
		if (ds >= dsLevels[dsLevels.length-1])
			return dsLevels[dsLevels.length-1];
		
		for (int i=dsLevels.length-2; i>=0; i--) {
			if (ds >= dsLevels[i]) {
				// return next higher level to avoid downsampling
				return dsLevels[i+1];
			}
		}
		return dsLevels[0];
	}
	
	
	@Override
	public BufferedImage readRegion(RegionRequest request) throws IOException {

		RealTransform transform2 = realtransform.copy();

		double downsample = request.getDownsample();
		
		var bounds = AwtTools.getBounds(request);
		
		// Rectangular request is not necessarily rectangular after spline transformation
		// Find transformed request by checking n points
		// along all 4 rectangle edges for potential distortions
		var boundsTR = createTransformedBounds(bounds, transform2, 10);
		
		var wrappedServer = getWrappedServer();

		int padFactor = 4; //2;
		//Rotation rotation = null;
		//if (wrappedServer instanceof TransformingImageServer)
		//	rotation = getRotation(wrappedServer, null);		
		//if (rotation != null && rotation != Rotation.ROTATE_NONE)
		//	padFactor = 4;
		
		double scaledDownsample = downsample / globalScale;		
		double downsampleTR = getBestDownsample(dsLevels, scaledDownsample);

		//double maxDownsample = wrappedServer.getDownsampleForResolution(wrappedServer.nResolutions()-1);
		//double downsampleTR = Math.min(downsample, maxDownsample);
		
		// Pad slightly (With padFactor=4 black stripes are avoided  - especially for large rotations and when images are loaded with rotation)
		int pad = (int) Math.ceil(downsampleTR * padFactor);
		int minX = Math.max(0, (int)boundsTR.getMinX()-pad);
		int minY = Math.max(0, (int)boundsTR.getMinY()-pad);		
		int maxX = Math.min(wrappedServer.getWidth(), (int)Math.ceil(boundsTR.getMaxX()+pad));
		int maxY = Math.min(wrappedServer.getHeight(), (int)Math.ceil(boundsTR.getMaxY()+pad));

		var requestTR = RegionRequest.createInstance(
				wrappedServer.getPath(),
				downsampleTR,
				minX, minY, maxX - minX, maxY - minY,
				request.getZ(),
				request.getT()
				);
		
		// Source
		BufferedImage img = getWrappedServer().readRegion(requestTR);
		if (img == null)
			return img;

		// Target
		int w = (int)(request.getWidth() / downsample);
		int h = (int)(request.getHeight() / downsample);
				
		var rasterTransform = img.getRaster();
		var raster = rasterTransform.createCompatibleWritableRaster(w, h); // Target
		
		int nBands = rasterTransform.getNumBands();
		
		Object elements = null;
		double[] dbl = new double[3];
		double[] dbl2 = new double[3];
		
		float ka, kb;
		float p1, p2, p3, p4, pA, pB, pOut;
		float[][] pixelsFloat = null;
		int[][] pixelsInt = null;
		int index;
		int xx, yy;
				
		float cubicA = 1, cubicB = 0;  // InterpolationType.BICUBIC
		if (interpolationMode != InterpolationType.CATMULLROM) {
			cubicA = 0.5f; cubicB = 0;
		}
		if (interpolationMode != InterpolationType.MITCHELLNETRAVALI) {
			cubicA = (float) (1/3.0); cubicB = cubicA;
		}
		if (interpolationMode != InterpolationType.CUBICBSPLINE) {
			cubicA = 0; cubicB = 1;
		}
			
		boolean useCubicInterpolation = (interpolationMode == InterpolationType.BICUBIC) ||
										(interpolationMode == InterpolationType.CATMULLROM) ||
										(interpolationMode == InterpolationType.MITCHELLNETRAVALI) ||
										(interpolationMode == InterpolationType.CUBICBSPLINE);
						
		int widthTransform = img.getWidth();
		int heightTransform = img.getHeight();
				
		int type = img.getType();
		
		if (interpolationMode != InterpolationType.NEARESTNEIGHBOR && 
				!(type == BufferedImage.TYPE_USHORT_GRAY || type == BufferedImage.TYPE_BYTE_GRAY ||
						type == BufferedImage.TYPE_CUSTOM )) {		
			Dialogs.showErrorNotification("AffineTransformInterpolationImageServer", "Current image type is only supported with NearestNeighbor interpolation!");
			return null;
		}
				
		boolean isNotFloatType = false;
		if (interpolationMode != InterpolationType.NEARESTNEIGHBOR) {
			if (type == BufferedImage.TYPE_USHORT_GRAY || type == BufferedImage.TYPE_BYTE_GRAY) {
				isNotFloatType = true;
				pixelsInt = new int[nBands][];
				pixelsFloat = new float[nBands][];
				for (int b=0; b<nBands; b++) {
					pixelsInt[b] = (int[])rasterTransform.getSamples(0, 0, widthTransform, heightTransform, b, (int[])null);				
					pixelsFloat[b] = InterpolationHelper.convertToFloatArray(pixelsInt[b],  (float[])null);
				}
			}
			else {
				pixelsFloat = new float[nBands][];
				for (int b=0; b<nBands; b++) {
					pixelsFloat[b] = (float[])rasterTransform.getSamples(0, 0, widthTransform, heightTransform, b, (float[])null);
				}
			}					
		}
	
		int xB1 = 0, xB2 = 1;
		int yB1 = 0, yB2 = 1;
		
		if ( useCubicInterpolation ) { //interpolationMode == InterpolationType.BICUBIC) {
			xB1 = 1; xB2 = 2;
			yB1 = 1; yB2 = 2;
		}	
		/*
		int[][] samplesInt = null;
		float[][] samplesFloat = null;
		
		if ( isNotFloatType )
			samplesInt = new int[nBands][w*h];
		else
			samplesFloat = new float[nBands][w*h];
		*/
		for (int y = 0; y < h; y++) { // Target
			
			int offset = y * w;
			
			for (int x = 0; x < w; x++) { // Target
				
				dbl[0] = x*downsample + request.getX();
				dbl[1] = y*downsample + request.getY();
				
				transform2.apply(dbl, dbl2);// Target -> Source
				
				float dblX = (float) ((dbl2[0]-requestTR.getX())/downsampleTR ); // Source
				float dblY = (float) ((dbl2[1]-requestTR.getY())/downsampleTR ); // Source							
				
				if (dblX >= xB1 && dblY >= yB1 && dblX < (widthTransform-xB2) && dblY < (heightTransform-yB2)) {				
					
					if (interpolationMode == InterpolationType.BILINEAR) {											
						// Portion from Burger&Burge, Digital Image Processing, 2010
						xx = (int)Math.floor(dblX);
						yy = (int)Math.floor(dblY);		
						
						index = yy * widthTransform + xx;

						for (int b=0; b<nBands; b++) {
							p1 = pixelsFloat[b][index];
							p2 = pixelsFloat[b][index + 1];
							p3 = pixelsFloat[b][index + widthTransform];
							p4 = pixelsFloat[b][index + widthTransform + 1];
							
							ka = dblX - xx;
							kb = dblY - yy;
	
							pA = p1 + ka*(p2 - p1);
							pB = p3 + ka*(p4 - p3);
							pOut = pA + kb*(pB - pA);
							
							if ( isNotFloatType ) {
								raster.setSample(x, y, b, (int)Math.round(pOut));
								//samplesInt[b][offset+x] = (int)Math.round(pOut);
							}
							else {
								raster.setSample(x, y, b, pOut);
								//samplesFloat[b][offset+x] = pOut;
							}
						}
					}
					else if ( useCubicInterpolation ) { //interpolationMode == InterpolationType.BICUBIC) { 
						// Portion from Burger&Burge, Digital Image Processing, 2010
						// https://en.wikipedia.org/wiki/Bicubic_interpolation
						xx = (int)Math.floor(dblX);
						yy = (int)Math.floor(dblY);		
						
						for (int b=0; b<nBands; b++) {
							pA = 0;
							for (int ty=0; ty<=3; ty++) {
								int v = yy - 1 + ty ; 
								pB = 0;
								index = v * widthTransform;
								for (int tx=0; tx<=3; tx++) {
									int u = xx - 1 + tx;
									pB += pixelsFloat[b][index + u] * InterpolationHelper.cubic(dblX - u, cubicA, cubicB);
								}
								pA += pB * InterpolationHelper.cubic(dblY - v, cubicA, cubicB);
							}
							
							if ( isNotFloatType ) {
								raster.setSample(x, y, b, (int)Math.round(pA));
								//samplesInt[b][offset+x] = (int)Math.round(pA);
							}
							else {
								raster.setSample(x, y, b, pA);
								//samplesFloat[b][offset+x] = pA;
							}
						}
					}
					else {  // Nearest neighbor Interpolation
						xx = (int)Math.round(dblX);
						yy = (int)Math.round(dblY);		
						
						elements = rasterTransform.getDataElements(xx, yy, elements);// Source
						raster.setDataElements(x, y, elements); // Target
					}				
				}				
			}
		}
		/*
		if (interpolationMode == InterpolationType.BILINEAR || useCubicInterpolation) {
			for (int b=0; b<nBands; b++) {
				if ( isNotFloatType )
					raster.setSamples(0, 0, w, h, b, samplesInt[b]);
				else
					raster.setSamples(0, 0, w, h, b, samplesFloat[b]);
			}
		}
		*/
		return new BufferedImage(img.getColorModel(), raster, img.isAlphaPremultiplied(), null);
	}

	/**
	 * Get the affine transform for this server.
	 * @return
	 */
	public RealTransformInterpolation getTransform() {
		return new RealTransformInterpolation(rtis);
	}
	 	
	@Override
	public ImageServerMetadata getOriginalMetadata() {
		return metadata;
	}
	
	@Override
	public String getServerType() {
		return "Real transform server";
	}
	
	@Override
	protected ServerBuilder<BufferedImage> createServerBuilder() {
		return new RealTransformImageServerBuilder(
				getMetadata(),
				getWrappedServer().getBuilder(),
				getTransform()
				);
	}

}

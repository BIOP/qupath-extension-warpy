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
 * 
 * This file is based on AffineTransformImageServer.java (by @author Pete Bankhead)
 *
 * Peter Haub (@phaub), Oct 2021
 * 
 *********************************/

package qupath.ext.imagecombinerwarpy.gui;


import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import qupath.ext.imagecombinerwarpy.gui.InterpolationModes.InterpolationType;
import qupath.lib.awt.common.AwtTools;
import qupath.lib.gui.dialogs.Dialogs;
import qupath.lib.images.servers.AbstractImageServer;
import qupath.lib.images.servers.ChannelTransformFeatureServer;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.images.servers.RotatedImageServer;
import qupath.lib.images.servers.RotatedImageServer.Rotation;
import qupath.lib.images.servers.TransformingImageServer;
import qupath.lib.io.GsonTools;
import qupath.lib.regions.ImageRegion;
import qupath.lib.regions.RegionRequest;

/**
 * ImageServer that dynamically applies an AffineTransform to an existing ImageServer.
 * <p>
 * Warning! This is incomplete and will be changed/removed in the future.
 * 
 * @author Pete Bankhead
 *
 */
public class AffineTransformInterpolationImageServer extends TransformingImageServer<BufferedImage> {
	
	private static Logger logger = LoggerFactory.getLogger(AffineTransformInterpolationImageServer.class);
	
	private ImageServerMetadata metadata;
	
	private transient ImageRegion region;
	
	private AffineTransformInterpolation transforminterpolate;
	private AffineTransform transform;
	private transient AffineTransform transformInverse;
	
	private InterpolationType interpolationMode = InterpolationType.NEARESTNEIGHBOR;
	
	private double globalScale = 1.0;	
	private double[] dsLevels;
	

	protected AffineTransformInterpolationImageServer(final ImageServer<BufferedImage> server, AffineTransformInterpolation transforminterpolate) throws NoninvertibleTransformException {
		super(server);
		
		logger.trace("Creating server for {} and AffineTransformInterpolate {}", server, transforminterpolate);
		
		this.transforminterpolate = transforminterpolate;
		
		this.transform = new AffineTransform(transforminterpolate.getTransform());
		this.transformInverse = transform.createInverse();
		
		this.interpolationMode = InterpolationModes.getInterpolationType(transforminterpolate.getInterpolation());
		
		double[] edges = new double[4];
		double[] edgesTR = new double[4];
				
		edges[2] = server.getWidth();
		edges[3] = server.getHeight();
		
		transform.transform(edges, 0, edgesTR, 0, 2);
		
		double lOrig = Math.sqrt(server.getWidth()*server.getWidth() + server.getHeight()*server.getHeight());
		double lRT = Math.sqrt((edgesTR[2]-edgesTR[0])*(edgesTR[2]-edgesTR[0]) 
								+ (edgesTR[3]-edgesTR[1])*(edgesTR[3]-edgesTR[1]));
		double scale = lRT / lOrig;
				
		var boundsTransformed = transform.createTransformedShape(
				new Rectangle2D.Double(0, 0, server.getWidth(), server.getHeight())).getBounds2D();
		
//		int minX = Math.max(0, (int)boundsTransformed.getMinX());
//		int maxX = Math.min(server.getWidth(), (int)Math.ceil(boundsTransformed.getMaxX()));
//		int minY = Math.max(0, (int)boundsTransformed.getMinY());
//		int maxY = Math.min(server.getHeight(), (int)Math.ceil(boundsTransformed.getMaxY()));
//		this.region = ImageRegion.createInstance(
//				minX, minY, maxX-minX, maxY-minY, 0, 0);
		
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
		
		// TODO: Apply AffineTransform to pixel sizes! Perhaps create a Shape or point and transform that?
		var builder = new ImageServerMetadata.Builder(server.getMetadata())
//				.path(server.getPath() + ": Affine " + transform.toString())
				.width(region.getWidth())
				.height(region.getHeight())
				.name(String.format("%s (%s)", server.getMetadata().getName(), transform.toString()))
				.levels(levelBuilder.build());
		
		// TODO: Handle pixel sizes in units other than microns
		var calUpdated = updatePixelCalibration(server.getPixelCalibration(), transform);
		if (!calUpdated.equals(server.getPixelCalibration())) {
			if (!calUpdated.equals(PixelCalibration.getDefaultInstance()))
				logger.debug("Pixel calibration updated to {}", calUpdated);
			builder.pixelSizeMicrons(calUpdated.getPixelWidthMicrons(), calUpdated.getPixelHeightMicrons());
		}
				
		metadata = builder.build();

		globalScale = scale; //Math.min(transform.getScaleX(), transform.getScaleY());		
		dsLevels = server.getPreferredDownsamples();
	}
	
	
	/**
	 * Try to update a {@link PixelCalibration} for an image that has been transformed.
	 * <p>
	 * If the transform is the identity transform, the input calibration will be returned unchanged.
	 * Otherwise, it will be updated if possible, or a default calibration (i.e. lacking pixel sizes) returned if not.
	 * <p>
	 * A default pixel calibration is expected in the following situations:
	 * <ul>
	 *   <li>The input calibration lacks pixel sizes</li>
	 *   <li>The transform is not invertible</li>
	 *   <li>The transform applies an arbitrary rotation (not a quadrant rotation) with non-square pixels</li>
	 *   <li>The transform performs an arbitrary conversion of the input coordinates (and cannot be simplified into translation, rotation or scaling)</li>
	 * </ul>
	 * <p>
	 * Warning! Be cautious when applying applying the same affine transform to an image in QuPath, 
	 * particularly whether the original transform or its inverse is needed.
	 * <p>
	 * The behavior of this method may change in future versions.
	 * 
	 * @param cal the original calibration for the (untransformed) image
	 * @param transform the affine transform to apply
	 * @return an appropriate pixel calibration; this may be the same as the input calibration
	 */
	static PixelCalibration updatePixelCalibration(PixelCalibration cal, AffineTransform transform) {
		// If units are in pixels, or we have an identity transform, keep the same calibration
		if (transform.isIdentity() || 
				(cal.unitsMatch2D() && PixelCalibration.PIXEL.equals(cal.getPixelWidthUnit()) && cal.getPixelWidth().doubleValue() == 1 && cal.getPixelHeight().doubleValue() == 1))
			return cal;
		
		try {
			transform = transform.createInverse();
		} catch (NoninvertibleTransformException e) {
			logger.warn("Transform is not invertible! I will use the default pixel calibration.");
			return PixelCalibration.getDefaultInstance();
		}
		
		int type = transform.getType();
		
		if ((type & AffineTransform.TYPE_GENERAL_TRANSFORM) != 0) {
			logger.warn("Arbitrary transform cannot be decomposed! I will use the default pixel calibration.");
			return PixelCalibration.getDefaultInstance();										
		}
		
		// Compute x and y scaling
		double[] temp = new double[] {1, 0, 0, 1};
		transform.deltaTransform(temp, 0, temp, 0, 2);

		double xScale = Math.sqrt(temp[0]*temp[0] + temp[1]*temp[1]);
		double yScale = Math.sqrt(temp[2]*temp[2] + temp[3]*temp[3]);
		
		// See what we can do with non-square pixels
		double pixelWidth = cal.getPixelWidth().doubleValue();
		double pixelHeight = cal.getPixelHeight().doubleValue();
		// For a 90 degree rotation with non-square pixels, swap pixel width & height
		if ((type & AffineTransform.TYPE_QUADRANT_ROTATION) != 0 && temp[0] == 0 && temp[3] == 0) {
			double pixelWidthOutput = pixelHeight * yScale;
			double pixelHeightOutput = pixelWidth * xScale;
			xScale = pixelWidthOutput / pixelWidth;
			yScale = pixelHeightOutput / pixelHeight;
		} else if ((type & AffineTransform.TYPE_GENERAL_ROTATION) != 0 && pixelWidth != pixelHeight) {
			logger.warn("General rotation with non-square pixels is not supported ({} vs {})! I will use the default pixel calibration.", pixelWidth, pixelHeight);
			return PixelCalibration.getDefaultInstance();							
		}
		
		return cal.createScaledInstance(xScale, yScale);
	}
	
	
	@Override
	protected String createID() {
		return getClass().getName() + ": + " + getWrappedServer().getPath() + " " + GsonTools.getInstance().toJson(transform);
	}
	
	/*
	private static Rotation getRotation(ImageServer<BufferedImage> server, Rotation rotation) {
		if (server instanceof RotatedImageServer) {
			rotation = ((RotatedImageServer)server).getRotation();
			return rotation;
		}
		else if (server instanceof TransformingImageServer){
			ImageServer<BufferedImage> serverTmp = null;
			serverTmp = ((TransformingImageServer<BufferedImage>) server).getWrappedServer();
			return getRotation(serverTmp, rotation);
		}
		else if (server == null)
			return null;
		
		return rotation;
	}
	*/
	
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
	public BufferedImage readBufferedImage(final RegionRequest request) throws IOException {

		double downsample = request.getDownsample();
		
		var bounds = AwtTools.getBounds(request);
		var boundsTransformed = transformInverse.createTransformedShape(bounds).getBounds();

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
		int pad = (int) Math.ceil(downsampleTR* padFactor);
		int minX = Math.max(0, (int)(boundsTransformed.getMinX()-pad));
		int maxX = Math.min(wrappedServer.getWidth(), (int)Math.ceil(boundsTransformed.getMaxX()+pad));
		int minY = Math.max(0, (int)(boundsTransformed.getMinY()-pad));
		int maxY = Math.min(wrappedServer.getHeight(), (int)Math.ceil(boundsTransformed.getMaxY()+pad));
				
		var requestTR = RegionRequest.createInstance(
				wrappedServer.getPath(),
				downsampleTR,
				minX, minY, maxX - minX, maxY - minY,
				request.getZ(),
				request.getT()
				);
			
		BufferedImage img = getWrappedServer().readBufferedImage(requestTR);
		if (img == null)
			return img;
		
		int w = (int)(request.getWidth() / downsample);
		int h = (int)(request.getHeight() / downsample);
		
		AffineTransform transform2 = new AffineTransform();
		transform2.scale(1.0/downsample, 1.0/downsample);
		transform2.translate(-request.getX(), -request.getY());
		transform2.concatenate(transform);
		
		try {
			transform2 = transform2.createInverse();
		} catch (NoninvertibleTransformException e) {
			throw new IOException(e);
		}

		var rasterTransform = img.getRaster();
		var raster = rasterTransform.createCompatibleWritableRaster(w, h);
		
		int nBands = rasterTransform.getNumBands();
		
		double[] row = new double[w*2];
		double[] row2 = new double[w*2];
		
		Object elements = null;

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
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				row[x*2] = x;
				row[x*2+1] = y;
			}
			
			transform2.transform(row, 0, row2, 0, w);
			
			int offset = y * w;
						
			for (int x = 0; x < w; x++) {
				
				float dblX = (float) ((row2[x*2]-requestTR.getX())/downsampleTR );
				float dblY = (float) ((row2[x*2+1]-requestTR.getY())/downsampleTR );							
				
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
						
						elements = rasterTransform.getDataElements(xx, yy, elements);
						raster.setDataElements(x, y, elements);
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
	public AffineTransformInterpolation getTransform() {
		return new AffineTransformInterpolation(transforminterpolate);
	}
	
	@Override
	public ImageServerMetadata getOriginalMetadata() {
		return metadata;
	}
	
	@Override
	public String getServerType() {
		return "Affine transform interpolation server";
	}
	
	@Override
	protected ServerBuilder<BufferedImage> createServerBuilder() {
		return new AffineTransformInterpolationImageServerBuilder(
				getMetadata(),
				getWrappedServer().getBuilder(),
				getTransform()
				);
	}

}
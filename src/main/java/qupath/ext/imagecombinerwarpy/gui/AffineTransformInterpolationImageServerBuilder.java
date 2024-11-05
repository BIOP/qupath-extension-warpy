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

import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;

public class AffineTransformInterpolationImageServerBuilder implements ServerBuilder<BufferedImage> {// extends AbstractServerBuilder<BufferedImage> {
	
	//::dip
	private ImageServerMetadata metadata;
	
	private ServerBuilder<BufferedImage> builder;
	private AffineTransformInterpolation transforminterpolation;
	
	AffineTransformInterpolationImageServerBuilder(ImageServerMetadata metadata, ServerBuilder<BufferedImage> builder, AffineTransformInterpolation transforminterpolation) {
		//super(metadata);
		this.metadata = metadata;
		this.builder = builder;
		this.transforminterpolation = transforminterpolation;
	}
	
	//@Override
	protected ImageServer<BufferedImage> buildOriginal() throws Exception {
		return new AffineTransformInterpolationImageServer(builder.build(), transforminterpolation);
	}

	@Override
	public ImageServer<BufferedImage> build() throws Exception {
		var server = buildOriginal();
		if (server == null)
			return null;
		if (metadata != null)
			server.setMetadata(metadata);
		return server;
	}		

	@Override
	public Collection<URI> getURIs() {
		return builder.getURIs();
	}

	@Override
	public ServerBuilder<BufferedImage> updateURIs(Map<URI, URI> updateMap) {
		ServerBuilder<BufferedImage> newBuilder = builder.updateURIs(updateMap);
		if (newBuilder == builder)
			return this;
		return new AffineTransformInterpolationImageServerBuilder(getMetadata().get(), newBuilder, transforminterpolation);
	}

	public Optional<ImageServerMetadata> getMetadata() {
		return Optional.of(metadata);
	}

}

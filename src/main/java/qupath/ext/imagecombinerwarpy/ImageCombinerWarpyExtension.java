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
 * The ImageCombinerWarpy is thought as an experimental² tool.
 * 
 * It is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * 
 * Note: This file is derived from QuPath Image Align Extension and was added to this project and modified by @phaub (Oct 2021).
 *
 * Peter Haub (@phaub), Oct 2021
 * 
 *********************************/
package qupath.ext.imagecombinerwarpy;

import net.imglib2.realtransform.RealTransformSerializer;
import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.GsonBuilder;

import qupath.ext.biop.warpy.Warpy;
import qupath.ext.imagecombinerwarpy.gui.*;
import qupath.lib.common.Version;
import qupath.lib.gui.ActionTools;
import qupath.lib.gui.ActionTools.ActionDescription;
import qupath.lib.gui.ActionTools.ActionMenu;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServers;
import qupath.lib.io.GsonTools;
import qupath.lib.io.GsonTools.SubTypeAdapterFactory;

/**
 * Extension to make more experimental commands present in the GUI.
 */
public class ImageCombinerWarpyExtension implements QuPathExtension, GitHubProject {
	
	private static Logger logger = LoggerFactory.getLogger(ImageCombinerWarpyExtension.class);
	
	private static boolean alreadyInstalled = false;
	
	private static Version minimumVersion = Version.parse("0.3.0-SNAPSHOT");

	@SuppressWarnings("javadoc")
	public class ExperimentalCommands {
		
		@ActionMenu("Analyze>Interactive image combiner warpy")
		@ActionDescription("Experimental command to interactively align and combine images using an Affine or Warpy transform. "
				+ "This is currently not terribly useful in itself, but may be helpful as part of more complex scripting workflows.")
		public final Action actionInteractiveImageCombinerWarpy;

		private ExperimentalCommands(QuPathGUI qupath) {
			var interactiveImageCombinerWarpy = new InteractiveImageCombinerWarpyCommand(qupath);
			actionInteractiveImageCombinerWarpy = qupath.createProjectAction(project -> interactiveImageCombinerWarpy.run());
		}
		
	}
	
	
    @Override
    public void installExtension(QuPathGUI qupath) {
    	if (alreadyInstalled || !checkCompatibility())
			return;
		
		try {			
			
			//RuntimeTypeAdapterFactory<ServerBuilder> typeAdapterFactory = (RuntimeTypeAdapterFactory<ServerBuilder>) ImageServers.getServerBuilderFactory();	        
	        SubTypeAdapterFactory<ServerBuilder> typeAdapterFactory = (SubTypeAdapterFactory<ServerBuilder>) ImageServers.getServerBuilderFactory();
	        		
			typeAdapterFactory.registerSubtype(RealTransformImageServerBuilder.class, "realtransform");
			typeAdapterFactory.registerSubtype(AffineTransformInterpolationImageServerBuilder.class, "transforminterpolate");
			
			GsonBuilder builder = GsonTools.getDefaultBuilder();

			builder.registerTypeAdapter(AffineTransformInterpolationTypeAdapter.class, new AffineTransformInterpolationTypeAdapter());
			RealTransformSerializer.addRealTransformAdapters(builder);
			builder.registerTypeAdapter(RealTransformInterpolation.class, new RealTransformSerializer.RealTransformInterpolationAdapter());

			// Add ImageCombinerWarpy
	    	qupath.installActions(ActionTools.getAnnotatedActions(new ExperimentalCommands(qupath)));
	    	
	    	alreadyInstalled = true;
			
		} catch (Throwable t) {
			logger.debug("Unable to add ImageCombinerWarpy to menu", t);
		}		
    }

    /**
	 * Check compatibility with the QuPath version.
	 * @return
	 */
	private static boolean checkCompatibility() {
		try {
			var version = QuPathGUI.getVersion();
			// If >= the minimum version, we are compatible as far as we know
			if (minimumVersion.compareTo(version) <= 0)
				return true;
		} catch (Exception e) {
			logger.debug("Version check exception: " + e.getLocalizedMessage(), e);
		}
		logger.warn("ImageCombinerWarpy extension is not compatible with the current QuPath version ({}.{}.{} required)",
				minimumVersion.getMajor(), minimumVersion.getMinor(), minimumVersion.getPatch());
		return false;
	}
    
    @Override
    public String getName() {
        return "Image Combiner Warpy extension";
    }

    @Override
    public String getDescription() {
		String msg = "Add ImageCombinerWarpy to QuPath 0.3 menu Analyze\n";
		msg += "Register and combine multiple images and add a new project entry.\n";
		msg += "Author: Peter Haub, Oct 2021\n";
		msg += "(Main Source: ImageAlignmentPane.java by Pete Bankhead)\n";
		msg += "(             'Warpy' project by BIOP EPFL)";
		return msg;
    }

	@Override
	public GitHubRepo getRepository() {
		return GitHubRepo.create(getName(), "qupath", "qupath-extension-imagecombinerwarpy");
	}

	@Override
	public Version getQuPathVersion() {
		return Version.parse("0.3.1");
	}

	@Override
	public Version getVersion() {
		return Version.parse(Warpy.version);
	}
}

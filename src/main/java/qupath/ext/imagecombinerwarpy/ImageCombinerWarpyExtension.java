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

import qupath.ext.imagecombinerwarpy.gui.*;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;
import qupath.lib.images.servers.ImageServerBuilder.ServerBuilder;
import qupath.lib.images.servers.ImageServers;
import qupath.lib.io.GsonTools;
import qupath.lib.io.GsonTools.SubTypeAdapterFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;

/**
 * Extension to make more experimental commands present in the GUI.
 */
public class ImageCombinerWarpyExtension implements QuPathExtension, GitHubProject {
	
	private static final Logger logger = LoggerFactory.getLogger(ImageCombinerWarpyExtension.class);
	
	private static boolean alreadyInstalled = false;
	
	private static final Version minimumVersion = Version.parse("0.3.0-SNAPSHOT");

	private static final LinkedHashMap<String, String> SCRIPTS = new LinkedHashMap<>() {{
		put("Warpy transfer annotations and detections to current entry", "scripts/Warpy_transfer_annotations_and_detections_to_current_entry.groovy");
		put("Warpy  transfer TMA to current entry", "scripts/Warpy_transfer_TMA_to_current_entry.groovy");
	}};
    @Override
    public void installExtension(QuPathGUI qupath) {
    	if (alreadyInstalled || !checkCompatibility())
			return;
		
		try {			
			
	        SubTypeAdapterFactory<ServerBuilder> typeAdapterFactory = (SubTypeAdapterFactory<ServerBuilder>) ImageServers.getServerBuilderFactory();
	        		
			typeAdapterFactory.registerSubtype(RealTransformImageServerBuilder.class, "realtransform");
			typeAdapterFactory.registerSubtype(AffineTransformInterpolationImageServerBuilder.class, "transforminterpolate");
			
			GsonBuilder builder = GsonTools.getDefaultBuilder();

			builder.registerTypeAdapter(AffineTransformInterpolationTypeAdapter.class, new AffineTransformInterpolationTypeAdapter());
			RealTransformSerializer.addRealTransformAdapters(builder);
			builder.registerTypeAdapter(RealTransformInterpolation.class, new RealTransformSerializer.RealTransformInterpolationAdapter());

			// Add ImageCombinerWarpy
			var imageCombinerWarpy = ActionTools.createAction(new InteractiveImageCombinerWarpyCommand(qupath), "Interactive image combiner Warpy");
			MenuTools.addMenuItems(qupath.getMenu("Analyze", false),
			imageCombinerWarpy);

			SCRIPTS.entrySet().forEach(entry -> {
				String name = entry.getValue();
				String command = entry.getKey();
				try (InputStream stream = ImageCombinerWarpyExtension.class.getClassLoader().getResourceAsStream(name)) {
					String script = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
					if (script != null) {
						MenuTools.addMenuItems(
								qupath.getMenu("Extensions>Cellpose", true),
								new Action(command, e -> openScript(qupath, script)));
					}
				} catch (Exception e) {
					logger.error(e.getLocalizedMessage(), e);
				}
			});
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
		return GitHubRepo.create(getName(), "biop", "qupath-extension-warpy");
	}

	@Override
	public Version getQuPathVersion() {
		return QuPathExtension.super.getQuPathVersion();
	}

	private static void openScript(QuPathGUI qupath, String script) {
		var editor = qupath.getScriptEditor();
		if (editor == null) {
			logger.error("No script editor is available!");
			return;
		}
		qupath.getScriptEditor().showScript("Cellpose detection", script);
	}

}

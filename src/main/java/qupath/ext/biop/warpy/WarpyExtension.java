package qupath.ext.biop.warpy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;

/**
 * Install Warpy as an extension.
 * <p>
 * Installs Warpy into QuPath, adding some metadata and adds the necessary global variables to QuPath's Preferences
 *
 * @author Nicolas Chiaruttini
 */
public class WarpyExtension implements QuPathExtension, GitHubProject {
    private final static Logger logger = LoggerFactory.getLogger(qupath.ext.biop.warpy.WarpyExtension.class);


    @Override
    public GitHubRepo getRepository() {
        return GitHubRepo.create("QuPath Warpy Extension", "biop", "qupath-extension-warpy");
    }

    @Override
    public void installExtension(QuPathGUI qupath) {

    }

    @Override
    public String getName() {
        return "Warpy extension";
    }

    @Override
    public String getDescription() {
        return "An extension for QuPath that allows non linear deformation of ROIs";
    }

    @Override
    public Version getQuPathVersion() {
        return QuPathExtension.super.getQuPathVersion();
    }

    @Override
    public Version getVersion() {
        return Version.parse("0.1.1-SNAPSHOT");
    }
}
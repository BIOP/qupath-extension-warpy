package qupath.ext.warpy;

import org.controlsfx.control.action.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.GeneralTools;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.extensions.GitHubProject;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;

import java.io.InputStream;
import java.util.Map;

/**
 * Install Warpy as an extension.
 * <p>
 * Installs Warpy into QuPath, adding some metadata and adds the necessary global variables to QuPath's Preferences
 *
 * @author Nicolas Chiaruttini, EPFL, 2021
 */
public class WarpyExtension implements QuPathExtension, GitHubProject {
    private final static Logger logger = LoggerFactory.getLogger(WarpyExtension.class);

    private boolean isInstalled = false;

    private static final Map<String, String> SCRIPTS = Map.of(
            "Warpy transfer annotations and detections to current entry", "scripts/Transfer_annotations_and_detections_to_current_entry.groovy",
            "Warpy transfer TMAs to current entry", "scripts/Transfer_TMA_to_current_entry.groovy"
    );

    @Override
    public GitHubRepo getRepository() {
        return GitHubRepo.create("Warpy Extension", "biop", "qupath-extension-warpy");
    }

    @Override
    public void installExtension(QuPathGUI qupath) {
        if(isInstalled)
            return;

        SCRIPTS.entrySet().forEach(entry -> {
            String name = entry.getValue();
            String command = entry.getKey();
            try (InputStream stream = WarpyExtension.class.getClassLoader().getResourceAsStream(name)) {
                String script = new String(stream.readAllBytes(), "UTF-8");
                if (script != null) {
                    MenuTools.addMenuItems(
                            qupath.getMenu("Extensions>Warpy", true),
                            new Action(command, e -> openScript(qupath, script)));
                }

                isInstalled = true;

            } catch (Exception e) {
                logger.error(e.getLocalizedMessage(), e);
            }
        });

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

    public static String getWarpyVersion() {
        String packageVersion = GeneralTools.getPackageVersion(WarpyExtension.class);
        return Version.parse(packageVersion).toString();
    }


    private static void openScript(QuPathGUI qupath, String script) {
        var editor = qupath.getScriptEditor();
        if (editor == null) {
            logger.error("No script editor is available!");
            return;
        }
        qupath.getScriptEditor().showScript("Warpy", script);
    }
}
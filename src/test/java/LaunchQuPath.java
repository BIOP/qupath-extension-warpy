import qupath.lib.gui.QuPathApp;

public class LaunchQuPath {

    /**
     * Main class for debugging
     *
     * @param args
     */
    public static void main(String... args) {
        //String projectPath = "\\\\svfas6.epfl.ch\\biop\\etx";
        QuPathApp.launch(QuPathApp.class);//, projectPath);
    }
}

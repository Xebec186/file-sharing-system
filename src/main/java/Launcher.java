/**
 * The Launcher class serves as a workaround entry point for launching the JavaFX application.
 * By starting from a class that does not extend javafx.application.Application, we prevent
 * the Java runtime from checking for JavaFX modules on the module-path, enabling the app
 * to run seamlessly on the classpath (unnamed module).
 */
public class Launcher {
    public static void main(String[] args) {
        // Delegate to the main JavaFX Application class
        Main.main(args);
    }
}

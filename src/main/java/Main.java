import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Main is the primary entry point for the JavaFX GUI client application.
 * It manages the creation of multiple independent client windows.
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        // Launch the initial client dashboard window
        createNewClientWindow();
    }

    /**
     * Creates and displays a new independent client window with its own controller,
     * UI state, and network socket connection.
     */
    public static void createNewClientWindow() {
        try {
            // Load the UI layout definition from FXML
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("File Sharing Client - Offline");
            
            // Link the Controller with the stage instance
            MainController controller = loader.getController();
            controller.setStage(stage);
            
            // Set up scene with the external stylesheet styling
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Main.class.getResource("style.css").toExternalForm());
            
            stage.setScene(scene);
            
            // Ensure connection resources are closed when window is closed
            stage.setOnCloseRequest(event -> controller.shutdown());
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to launch new client window: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // Launch the JavaFX runtime
        launch(args);
    }
}

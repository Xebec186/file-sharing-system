import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        createNewClientWindow();
    }

    /**
     * Creates and displays a new independent client window with its own state.
     */
    public static void createNewClientWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource("main-view.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("File Sharing Client - Offline");
            
            MainController controller = loader.getController();
            controller.setStage(stage);
            
            Scene scene = new Scene(root);
            scene.getStylesheets().add(Main.class.getResource("style.css").toExternalForm());
            
            stage.setScene(scene);
            
            // Clean up socket resources on window close
            stage.setOnCloseRequest(event -> controller.shutdown());
            
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Failed to launch new client window: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

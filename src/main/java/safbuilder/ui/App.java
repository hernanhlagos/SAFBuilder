package safbuilder.ui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import safbuilder.utilities.Paths;

import java.nio.file.Path;

/**
 * Main application class for the SAFBuilder UI.
 * <p>
 * This class extends {@link Application} and launches the JavaFX application.
 * It loads the FXML layout from the provided path and sets up the primary stage.
 * </p>
 *
 * @author Hernán Lagos
 */
public class App extends Application {

    /**
     * Main entry point for the application.
     * This method launches the JavaFX application.
     *
     * @param args the command-line arguments (not used)
     */
    public static void main(String[] args) {
        launch(); // Launches the JavaFX application
    }

    /**
     * Starts the JavaFX application.
     * <p>
     * This method loads the FXML file from the {@code Paths.App} location and sets
     * up the stage with a non-resizable window and a title.
     * </p>
     *
     * @param stage the primary stage for this application
     * @throws Exception if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws Exception {
        // Load the FXML layout from the specified path
        AnchorPane root = FXMLLoader.load(getClass().getResource(Paths.App));

        // Create and set the scene for the primary stage
        Scene scene = new Scene(root);
        stage.setScene(scene);

        // Configure the stage properties
        stage.setResizable(false); // Disable window resizing
        stage.setTitle("Simple Archive Format Package (SAFBuilder)"); // Set window title

        // Show the stage
        stage.show();
    }
}

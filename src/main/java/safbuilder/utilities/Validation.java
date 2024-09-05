package safbuilder.utilities;

import javafx.scene.control.Alert;

import java.io.File;

/**
 * Utility class for validation-related methods.
 * <p>
 * The {@code Validation} class provides helper methods for showing alerts,
 * checking the existence of files, and validating CSV file extensions.
 * </p>
 *
 * @author Hernán Lagos
 */
public class Validation {

    /**
     * Displays an alert dialog with the specified parameters.
     *
     * @param type    the type of alert (e.g., ERROR, INFORMATION)
     * @param title   the title of the alert dialog
     * @param message the message to be displayed in the alert dialog
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Validates if a file exists at the given path and is a regular file.
     *
     * @param path the file path to check
     * @return {@code true} if the file exists and is a regular file, {@code false} otherwise
     */
    public static boolean isFileExists(String path) {
        File file = new File(path);
        return file.exists() && file.isFile();
    }

    /**
     * Checks if the given file path points to a CSV file.
     * <p>
     * The method checks the file extension, ignoring case sensitivity.
     * </p>
     *
     * @param path the file path to validate
     * @return {@code true} if the file has a ".csv" extension, {@code false} otherwise
     */
    public static boolean isCsvFile(String path) {
        return path.toLowerCase().endsWith(".csv");
    }
}

package safbuilder.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.cli.ParseException;
import safbuilder.BatchProcess;
import safbuilder.utilities.LogListener;
import safbuilder.utilities.Logger;
import safbuilder.utilities.Validation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller class for handling user interactions in the SAFBuilder UI.
 * <p>
 * This class contains methods to handle events from buttons and other UI elements,
 * as well as to validate user input and execute batch processes.
 * It also implements the {@link LogListener} interface to display log messages in a {@code TextArea}.
 * </p>
 *
 * @author Hernán Lagos
 */
public class AppController implements LogListener {

    // UI elements defined in the FXML file
    @FXML
    private Button btnCSV;

    @FXML
    private Button btnCreateBatch;

    @FXML
    private CheckBox checkBoxZipFile;

    @FXML
    private TextField txtFieldBatchName;

    @FXML
    private TextField txtFieldCSV;

    @FXML
    private TextArea textAreaLog;

    @FXML
    private Button btnClearLog;

    /**
     * Initializes the controller and registers this instance as a log listener.
     */
    @FXML
    public void initialize() {
        // Register this controller as a log listener
        Logger.getInstance().addLogListener(this);
    }

    /**
     * Handles the action of loading a CSV file.
     * <p>
     * Opens a {@link FileChooser} to select a CSV file and displays the selected file path
     * in the corresponding text field.
     * </p>
     *
     * @param event the event triggered by the button click
     */
    @FXML
    void loadCSV(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        // Show the file chooser dialog
        File selectedFile = fileChooser.showOpenDialog(new Stage());

        // Check if a file was selected and update the TextField
        if (selectedFile != null) {
            txtFieldCSV.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Creates a batch process based on user input.
     * <p>
     * Validates the user input, prepares the arguments, and runs the batch process.
     * Opens the output directory after the process completes.
     * </p>
     *
     * @param event the event triggered by the button click
     * @throws IOException if an error occurs during batch processing
     * @throws ParseException if there is an error parsing the arguments
     */
    @FXML
    void createBatch(ActionEvent event) throws IOException, ParseException {
        boolean zip = checkBoxZipFile.isSelected();
        String csv = txtFieldCSV.getText();
        String batchName = txtFieldBatchName.getText();

        // Validate CSV file
        if (csv == null || csv.isEmpty() || !Validation.isCsvFile(csv) || !Validation.isFileExists(csv)) {
            Validation.showAlert(Alert.AlertType.ERROR, "Validation Error", "The file path is not valid. Please select a valid metadata CSV file.");
            return;
        }

        // Prepare arguments for batch process
        List<String> argsList = new ArrayList<>();
        argsList.add("-c");
        argsList.add(csv);

        if (batchName != null && !batchName.isEmpty()) {
            argsList.add("-o");
            argsList.add(batchName);
        }

        if (zip) {
            argsList.add("-z");
        }

        // Run batch process
        String[] args = argsList.toArray(new String[0]);
        BatchProcess.BatchProcessCLI(args);

        // Open the output directory after the process
        String openFolderOutput = new File(csv).getParent();
        openOutputFolder(openFolderOutput);
    }

    /**
     * Clears the content of the log TextArea.
     *
     * This method is triggered by a UI event (such as a button click)
     * to clear the displayed logs in the TextArea, providing the user
     * with a fresh space for new log messages.
     *
     * @param event the event that triggers the clearing action, typically an {@link ActionEvent}
     */
    @FXML
    void clearLog(ActionEvent event) {
        textAreaLog.clear();
    }

    /**
     * Opens the specified folder in the file explorer depending on the OS.
     *
     * @param folderPath the path to the folder to open
     * @throws IOException if an error occurs while opening the folder
     */
    private void openOutputFolder(String folderPath) throws IOException {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            new ProcessBuilder("explorer", folderPath).start();
        } else if (os.contains("mac")) {
            new ProcessBuilder("open", folderPath).start();
        } else if (os.contains("nix") || os.contains("nux")) {
            new ProcessBuilder("xdg-open", folderPath).start();
        } else {
            System.out.println("Unsupported operating system.");
        }
    }

    /**
     * Displays log messages in the {@code TextArea}.
     * This method is called by the logger whenever a new message is logged.
     *
     * @param message the log message to display
     */
    @Override
    public void onLog(String message) {
        javafx.application.Platform.runLater(() -> {
            textAreaLog.appendText(message + System.lineSeparator());
            textAreaLog.setScrollTop(Double.MAX_VALUE); // Scroll down to the latest message
        });
    }
}

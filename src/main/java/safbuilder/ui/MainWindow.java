package safbuilder.ui;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import safbuilder.core.BuildConfig;
import safbuilder.core.BuildResult;
import safbuilder.core.ProgressListener;
import safbuilder.core.SAFPackage;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.prefs.Preferences;

public class MainWindow extends JFrame {
    private static final Logger log = LoggerFactory.getLogger(MainWindow.class);
    private final Preferences prefs = Preferences.userNodeForPackage(MainWindow.class);

    private JTextField csvPathField;
    private JTextField inputDirPathField;
    private JTextField outputDirPathField;
    private JTextField outputNameField;
    private JCheckBox zipCheckBox;
    private JProgressBar progressBar;
    private JTextArea logArea;
    private JButton runButton;
    private JLabel validationLabel; // Indicador visual de salud del CSV
    private JTable csvTable; // Tabla de vista previa
    private javax.swing.table.DefaultTableModel tableModel;

    public MainWindow() {
        setTitle("SAFBuilder Modernized v2.0");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        // --- Panel Superior: Configuración ---
        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        // Fila 1: CSV
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 1; gbc.weightx = 0;
        formPanel.add(new JLabel("Archivo CSV:"), gbc);

        csvPathField = new JTextField(prefs.get("lastCsv", ""));
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(csvPathField, gbc);

        JButton browseCsvBtn = new JButton("...");
        browseCsvBtn.addActionListener(e -> browseCsv());
        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(browseCsvBtn, gbc);

        // Fila 2: Carpeta de Archivos (Origen)
        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Carpeta Archivos:"), gbc);

        inputDirPathField = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(inputDirPathField, gbc);

        JButton browseDirBtn = new JButton("...");
        browseDirBtn.addActionListener(e -> browseInputDir());
        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(browseDirBtn, gbc);

        // Fila 3: Carpeta de Destino (Salida)
        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Carpeta Destino:"), gbc);

        outputDirPathField = new JTextField();
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(outputDirPathField, gbc);

        JButton browseOutBtn = new JButton("...");
        browseOutBtn.addActionListener(e -> browseOutputDir());
        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(browseOutBtn, gbc);

        // Fila 4: Nombre de salida y Validación
        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Nombre Salida:"), gbc);

        outputNameField = new JTextField("SimpleArchiveFormat");
        gbc.gridx = 1; gbc.weightx = 1.0;
        formPanel.add(outputNameField, gbc);

        JButton validateBtn = new JButton("Validar CSV");
        validateBtn.addActionListener(e -> performValidation());
        gbc.gridx = 2; gbc.weightx = 0;
        formPanel.add(validateBtn, gbc);

        // Fila 5: Opciones y Estado
        gbc.gridx = 1; gbc.gridy = 4; gbc.gridwidth = 1;
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        zipCheckBox = new JCheckBox("Crear ZIP", true);
        optionsPanel.add(zipCheckBox);
        formPanel.add(optionsPanel, gbc);

        validationLabel = new JLabel("Seleccione un archivo CSV para validar");
        validationLabel.setFont(validationLabel.getFont().deriveFont(Font.ITALIC));
        gbc.gridx = 1; gbc.gridy = 5; gbc.gridwidth = 2;
        formPanel.add(validationLabel, gbc);

        mainPanel.add(formPanel, BorderLayout.NORTH);

        // --- Panel Central: Logs y Vista Previa (Dividido) ---
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de ejecución"));

        tableModel = new javax.swing.table.DefaultTableModel() {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Solo lectura
            }
        };
        csvTable = new JTable(tableModel);
        csvTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        csvTable.getTableHeader().setReorderingAllowed(false);
        JScrollPane tableScroll = new JScrollPane(csvTable);

        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.setBorder(BorderFactory.createTitledBorder("Vista previa del CSV"));
        tablePanel.add(tableScroll, BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePanel, logScroll);
        splitPane.setResizeWeight(0.5); // 50/50 al redimensionar
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(200);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        // --- Panel Inferior: Progreso y Acción ---
        JPanel bottomPanel = new JPanel(new BorderLayout(10, 10));
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        bottomPanel.add(progressBar, BorderLayout.NORTH);

        runButton = new JButton("GENERAR PAQUETE SAF");
        runButton.setFont(runButton.getFont().deriveFont(Font.BOLD, 14f));
        runButton.addActionListener(e -> runProcess());
        bottomPanel.add(runButton, BorderLayout.SOUTH);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
    }

    private void browseCsv() {
        JFileChooser chooser = new JFileChooser(prefs.get("lastDir", "."));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos CSV", "csv"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            csvPathField.setText(file.getAbsolutePath());
            prefs.put("lastCsv", file.getAbsolutePath());
            prefs.put("lastDir", file.getParent());
            
            // Sugerir carpetas si están vacías
            if (inputDirPathField.getText().trim().isEmpty()) {
                inputDirPathField.setText(file.getParent());
            }
            if (outputDirPathField.getText().trim().isEmpty()) {
                outputDirPathField.setText(file.getParent());
            }
            
            performValidation();
        }
    }

    private void browseInputDir() {
        JFileChooser chooser = new JFileChooser(inputDirPathField.getText().trim().isEmpty() 
            ? prefs.get("lastDir", ".") : inputDirPathField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            inputDirPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void browseOutputDir() {
        JFileChooser chooser = new JFileChooser(outputDirPathField.getText().trim().isEmpty() 
            ? prefs.get("lastDir", ".") : outputDirPathField.getText().trim());
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            outputDirPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void performValidation() {
        File csv = new File(csvPathField.getText().trim());
        if (!csv.exists()) return;

        safbuilder.core.CsvValidator validator = new safbuilder.core.CsvValidator();
        safbuilder.core.ValidationResult result = validator.validate(csv);

        if (result.isValid() && result.getErrors().isEmpty()) {
            validationLabel.setText("✓ CSV Estructura Válida [" + result.getDetectedCharset() + "] (" + result.getHeaders().size() + " columnas)");
            validationLabel.setForeground(new Color(40, 167, 69)); // Verde
            
            // Cargar datos en la tabla
            loadCsvToTable(csv);

            if (!result.getWarnings().isEmpty()) {
                logArea.append("Advertencias de validación:\n");
                result.getWarnings().forEach(w -> logArea.append(" - " + w + "\n"));
            }
        } else {
            validationLabel.setText("✗ Error en estructura: " + (result.getErrors().isEmpty() ? "No válido" : result.getErrors().get(0)));
            validationLabel.setForeground(Color.RED);
            tableModel.setRowCount(0);
            tableModel.setColumnCount(0);
        }
    }

    private void loadCsvToTable(File csvFile) {
        try {
            java.nio.charset.Charset charset = safbuilder.core.CharsetDetector.detect(csvFile.getAbsolutePath());
            try (org.apache.commons.csv.CSVParser parser = org.apache.commons.csv.CSVParser.parse(csvFile, charset, 
                    org.apache.commons.csv.CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
                
                List<String> headers = new java.util.ArrayList<>(parser.getHeaderNames());
                // Añadir columna de índice al principio
                headers.add(0, "#");
                
                tableModel.setColumnIdentifiers(headers.toArray());
                tableModel.setRowCount(0);

                int count = 1;
                for (org.apache.commons.csv.CSVRecord record : parser) {
                    Object[] row = new Object[headers.size()];
                    row[0] = count++; // Número de fila
                    for (int i = 1; i < headers.size(); i++) {
                        row[i] = (i-1) < record.size() ? record.get(i-1) : "";
                    }
                    tableModel.addRow(row);
                }
            }
        } catch (Exception e) {
            log.error("Error al cargar tabla", e);
            logArea.append("Error al previsualizar CSV: " + e.getMessage() + "\n");
        }
    }

    private void runProcess() {
        File csv = new File(csvPathField.getText().trim());
        if (!csv.exists() || !csv.isFile()) {
            JOptionPane.showMessageDialog(this, "Por favor seleccione un archivo CSV válido.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        runButton.setEnabled(false);
        logArea.setText("");
        progressBar.setValue(0);

        // Ejecución en segundo plano para no congelar la UI
        SwingWorker<BuildResult, String> worker = new SwingWorker<>() {
            @Override
            protected BuildResult doInBackground() throws Exception {
                BuildConfig.Builder builder = BuildConfig.builder(csv)
                        .outputName(outputNameField.getText().trim())
                        .exportToZip(zipCheckBox.isSelected());
                
                String customInDir = inputDirPathField.getText().trim();
                if (!customInDir.isEmpty()) {
                    builder.inputDirectory(new File(customInDir));
                }
                
                String customOutDir = outputDirPathField.getText().trim();
                if (!customOutDir.isEmpty()) {
                    builder.outputDirectory(new File(customOutDir));
                }
                
                BuildConfig config = builder.build();

                SAFPackage engine = new SAFPackage();
                return engine.processMetaPack(config, new ProgressListener() {
                    @Override
                    public void onProgress(String message, int percentComplete) {
                        publish(percentComplete + "|" + message);
                    }

                    @Override
                    public void onWarning(String message, Exception cause) {
                        publish("WARN|" + message);
                    }

                    @Override
                    public void onError(String message, Exception cause) {
                        publish("ERROR|" + message);
                    }

                    @Override
                    public void onComplete(BuildResult result) {
                        // handled in done()
                    }
                });
            }

            @Override
            protected void process(java.util.List<String> chunks) {
                for (String chunk : chunks) {
                    if (chunk.contains("|")) {
                        String[] parts = chunk.split("\\|", 2);
                        if (parts[0].equals("WARN")) {
                            logArea.append(" [!] " + parts[1] + "\n");
                        } else if (parts[0].equals("ERROR")) {
                            logArea.append(" [X] " + parts[1] + "\n");
                        } else {
                            progressBar.setValue(Integer.parseInt(parts[0]));
                            logArea.append(" - " + parts[1] + "\n");
                        }
                    }
                }
            }

            @Override
            protected void done() {
                runButton.setEnabled(true);
                try {
                    BuildResult res = get();
                    if (res.isSuccess()) {
                        JOptionPane.showMessageDialog(MainWindow.this, "¡Proceso completado exitosamente!\n" + res.getItemsProcessed() + " ítems generados.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(MainWindow.this, "Hubo errores durante el proceso.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    log.error("Error en el worker", e);
                    JOptionPane.showMessageDialog(MainWindow.this, "Error crítico: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        worker.execute();
    }

    public static void main(String[] args) {
        // Soporte para escalado en pantallas 4K/Retina
        System.setProperty("flatlaf.uiScale", "2");
        FlatDarkLaf.setup();
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }
}

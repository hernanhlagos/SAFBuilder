package safbuilder.core;

import java.io.File;

/**
 * Immutable configuration object for a SAF build run.
 *
 * <p>Replaces the scattered method parameters of the original
 * {@code processMetaPack} calls with a single, validated config object.</p>
 *
 * <p>Use the nested {@link Builder} to construct instances.</p>
 */
public final class BuildConfig {

    private final File csvFile;
    private final File inputDirectory;
    private final File outputDirectory;
    private final String outputName;
    private final boolean exportToZip;

    private BuildConfig(Builder b) {
        this.csvFile        = b.csvFile;
        this.inputDirectory = b.inputDirectory != null ? b.inputDirectory : b.csvFile.getParentFile();
        this.outputDirectory = b.outputDirectory != null ? b.outputDirectory : b.csvFile.getParentFile();
        this.outputName     = b.outputName;
        this.exportToZip    = b.exportToZip;
    }

    public File getCsvFile() { return csvFile; }

    /** The directory where the actual bitstreams (PDFs, etc.) are located. */
    public File getInputDirectory() { return inputDirectory; }

    /** The directory where SAF will be created. */
    public File getOutputDirectory() { return outputDirectory; }

    /** Name of the output SAF directory (default: {@code SimpleArchiveFormat}). */
    public String getOutputName() { return outputName; }

    /** Whether to ZIP the output directory after building. */
    public boolean isExportToZip() { return exportToZip; }

    // -------------------------------------------------------------------------

    public static Builder builder(File csvFile) {
        return new Builder(csvFile);
    }

    public static final class Builder {
        private final File csvFile;
        private File    inputDirectory = null;
        private File    outputDirectory = null;
        private String  outputName     = "SimpleArchiveFormat";
        private boolean exportToZip    = false;

        private Builder(File csvFile) {
            if (csvFile == null)       throw new IllegalArgumentException("csvFile must not be null");
            if (!csvFile.exists())     throw new IllegalArgumentException("csvFile does not exist: " + csvFile);
            if (!csvFile.isFile())     throw new IllegalArgumentException("csvFile is not a regular file: " + csvFile);
            this.csvFile = csvFile;
        }

        public Builder outputName(String outputName) { this.outputName = outputName; return this; }
        public Builder inputDirectory(File inputDirectory) { this.inputDirectory = inputDirectory; return this; }
        public Builder outputDirectory(File outputDirectory) { this.outputDirectory = outputDirectory; return this; }
        public Builder exportToZip(boolean exportToZip) { this.exportToZip = exportToZip; return this; }

        public BuildConfig build() {
            return new BuildConfig(this);
        }
    }
}

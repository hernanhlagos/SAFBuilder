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
    private final String outputName;
    private final boolean exportToZip;
    private final boolean useSymbolicLinks;

    private BuildConfig(Builder b) {
        this.csvFile        = b.csvFile;
        this.outputName     = b.outputName;
        this.exportToZip    = b.exportToZip;
        this.useSymbolicLinks = b.useSymbolicLinks;
    }

    /** The CSV metadata file. Its parent directory is the input directory. */
    public File getCsvFile() { return csvFile; }

    /** Parent directory of the CSV — all content files must live here. */
    public File getInputDirectory() { return csvFile.getParentFile(); }

    /** Name of the output SAF directory (default: {@code SimpleArchiveFormat}). */
    public String getOutputName() { return outputName; }

    /** Whether to ZIP the output directory after building. */
    public boolean isExportToZip() { return exportToZip; }

    /** Whether to create symbolic links instead of copying files. */
    public boolean isUseSymbolicLinks() { return useSymbolicLinks; }

    // -------------------------------------------------------------------------

    public static Builder builder(File csvFile) {
        return new Builder(csvFile);
    }

    public static final class Builder {
        private final File csvFile;
        private String  outputName     = "SimpleArchiveFormat";
        private boolean exportToZip    = false;
        private boolean useSymbolicLinks = false;

        private Builder(File csvFile) {
            if (csvFile == null)       throw new IllegalArgumentException("csvFile must not be null");
            if (!csvFile.exists())     throw new IllegalArgumentException("csvFile does not exist: " + csvFile);
            if (!csvFile.isFile())     throw new IllegalArgumentException("csvFile is not a regular file: " + csvFile);
            this.csvFile = csvFile;
        }

        public Builder outputName(String outputName) {
            if (outputName != null && !outputName.isBlank()) {
                this.outputName = outputName.strip();
            }
            return this;
        }

        public Builder exportToZip(boolean exportToZip) {
            this.exportToZip = exportToZip;
            return this;
        }

        public Builder useSymbolicLinks(boolean useSymbolicLinks) {
            this.useSymbolicLinks = useSymbolicLinks;
            return this;
        }

        public BuildConfig build() {
            return new BuildConfig(this);
        }
    }
}

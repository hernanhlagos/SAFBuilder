package safbuilder.core;

import java.io.File;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

/**
 * Immutable result object returned after a SAF build completes.
 */
public final class BuildResult {

    private final boolean success;
    private final int     itemsProcessed;
    private final File    outputDirectory;
    private final File    zipFile;           // null if no ZIP was created
    private final List<String> warnings;
    private final Instant  startedAt;
    private final Instant  finishedAt;

    private BuildResult(Builder b) {
        this.success         = b.success;
        this.itemsProcessed  = b.itemsProcessed;
        this.outputDirectory = b.outputDirectory;
        this.zipFile         = b.zipFile;
        this.warnings        = Collections.unmodifiableList(b.warnings);
        this.startedAt       = b.startedAt;
        this.finishedAt      = b.finishedAt;
    }

    public boolean isSuccess()              { return success; }
    public int     getItemsProcessed()      { return itemsProcessed; }
    public File    getOutputDirectory()     { return outputDirectory; }
    /** @return the ZIP file, or {@code null} if no ZIP was requested/created. */
    public File    getZipFile()             { return zipFile; }
    public List<String> getWarnings()       { return warnings; }
    public Instant getStartedAt()           { return startedAt; }
    public Instant getFinishedAt()          { return finishedAt; }
    public Duration getDuration()           { return Duration.between(startedAt, finishedAt); }

    @Override
    public String toString() {
        return String.format("BuildResult{success=%b, items=%d, duration=%dms, warnings=%d}",
                success, itemsProcessed, getDuration().toMillis(), warnings.size());
    }

    // -------------------------------------------------------------------------

    public static Builder builder(Instant startedAt) {
        return new Builder(startedAt);
    }

    public static final class Builder {
        private boolean  success        = false;
        private int      itemsProcessed = 0;
        private File     outputDirectory;
        private File     zipFile;
        private List<String> warnings   = new java.util.ArrayList<>();
        private final Instant startedAt;
        private Instant  finishedAt     = Instant.now();

        private Builder(Instant startedAt) {
            this.startedAt = startedAt;
        }

        public Builder success(boolean v)               { this.success = v; return this; }
        public Builder itemsProcessed(int v)            { this.itemsProcessed = v; return this; }
        public Builder outputDirectory(File v)          { this.outputDirectory = v; return this; }
        public Builder zipFile(File v)                  { this.zipFile = v; return this; }
        public Builder warnings(List<String> v)         { this.warnings = v; return this; }
        public Builder finishedAt(Instant v)            { this.finishedAt = v; return this; }

        public BuildResult build() {
            this.finishedAt = Instant.now();
            return new BuildResult(this);
        }
    }
}

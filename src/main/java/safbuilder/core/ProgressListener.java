package safbuilder.core;

/**
 * Receives progress notifications from {@link SAFPackage} during a build.
 *
 * <p>Implement this interface to connect the core engine to any output
 * channel: console (CLI), Swing components (UI), log files, etc.</p>
 *
 * <p>Implementations that update Swing components <strong>must</strong>
 * dispatch calls via {@code SwingUtilities.invokeLater}.</p>
 */
public interface ProgressListener {

    /**
     * Called for each informational step completed during the build.
     *
     * @param message         Human-readable description of the step.
     * @param percentComplete Value between 0 and 100.
     */
    void onProgress(String message, int percentComplete);

    /**
     * Called when a non-fatal error occurs (processing continues).
     *
     * @param message Short description of the problem.
     * @param cause   The underlying exception, may be {@code null}.
     */
    void onWarning(String message, Exception cause);

    /**
     * Called when a fatal error stops processing.
     *
     * @param message Short description of the error.
     * @param cause   The underlying exception, may be {@code null}.
     */
    void onError(String message, Exception cause);

    /**
     * Called once when the build finishes successfully.
     *
     * @param result Summary of what was produced.
     */
    void onComplete(BuildResult result);

    /** No-op listener. Useful in tests and as a safe default. */
    ProgressListener SILENT = new ProgressListener() {
        public void onProgress(String msg, int pct) {}
        public void onWarning(String msg, Exception e) {}
        public void onError(String msg, Exception e) {}
        public void onComplete(BuildResult r) {}
    };
}

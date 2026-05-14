package safbuilder.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import safbuilder.core.BuildResult;
import safbuilder.core.ProgressListener;

/**
 * {@link ProgressListener} implementation that prints to {@code stdout} and
 * {@code stderr}. Used by the CLI entry point.
 *
 * <p>Progress lines are printed with a percentage prefix:
 * {@code [42%] Procesando ítem 5 de 12}</p>
 */
public class ConsoleProgressListener implements ProgressListener {

    private static final Logger log = LoggerFactory.getLogger(ConsoleProgressListener.class);

    @Override
    public void onProgress(String message, int percentComplete) {
        String line = String.format("[%3d%%] %s", percentComplete, message);
        System.out.println(line);
        log.debug(line);
    }

    @Override
    public void onWarning(String message, Exception cause) {
        String line = "[WARN] " + message;
        System.out.println(line);
        if (cause != null) {
            log.warn(message, cause);
        } else {
            log.warn(message);
        }
    }

    @Override
    public void onError(String message, Exception cause) {
        String line = "[ERROR] " + message;
        System.err.println(line);
        if (cause != null) {
            log.error(message, cause);
        } else {
            log.error(message);
        }
    }

    @Override
    public void onComplete(BuildResult result) {
        System.out.println("[100%] Proceso completado.");
        if (result.getOutputDirectory() != null) {
            System.out.println("       Directorio SAF: " + result.getOutputDirectory().getAbsolutePath());
        }
        if (result.getZipFile() != null) {
            System.out.println("       ZIP: " + result.getZipFile().getAbsolutePath());
        }
        log.info("Build complete: {}", result);
    }
}

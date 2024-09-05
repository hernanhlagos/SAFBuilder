package safbuilder.utilities;

/**
 * Interface for listening to log messages from the {@code Logger}.
 * <p>
 * Any class that implements {@code LogListener} must provide an implementation for the {@code onLog} method,
 * which will be invoked whenever a new log message is emitted by the {@code Logger}.
 * </p>
 *
 * @author Hernán Lagos
 */
public interface LogListener {

    /**
     * Called when a log message is generated.
     *
     * @param message the log message
     */
    void onLog(String message);
}

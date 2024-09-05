package safbuilder.utilities;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class responsible for logging messages and notifying listeners.
 * <p>
 * The {@code Logger} class implements the Singleton pattern to ensure only one instance of the logger exists.
 * It allows multiple listeners to subscribe and receive log messages.
 * </p>
 *
 * @author Hernán Lagos
 */
public class Logger {

    // Singleton instance of the Logger
    private static Logger instance;

    // List of registered listeners that will receive log messages
    private final List<LogListener> listeners = new ArrayList<>();

    // Private constructor to prevent instantiation
    private Logger() {}

    /**
     * Returns the single instance of the {@code Logger}.
     * If the instance does not exist yet, it will be created.
     *
     * @return the singleton instance of the {@code Logger}
     */
    public static synchronized Logger getInstance() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    /**
     * Registers a new log listener.
     * <p>
     * The listener will receive any subsequent log messages.
     * </p>
     *
     * @param listener the listener to be added
     */
    public void addLogListener(LogListener listener) {
        listeners.add(listener);
    }

    /**
     * Logs a message and notifies all registered listeners.
     * <p>
     * Each listener's {@code onLog} method is called with the provided message.
     * </p>
     *
     * @param message the message to be logged
     */
    public void log(String message) {
        for (LogListener listener : listeners) {
            listener.onLog(message);
        }
    }
}

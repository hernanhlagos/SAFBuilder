package safbuilder.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of a CSV structure and content validation.
 */
public class ValidationResult {
    private final boolean valid;
    private final List<String> errors = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> headers = new ArrayList<>();

    public ValidationResult(boolean valid) {
        this.valid = valid;
    }

    public boolean isValid() { return valid; }
    public List<String> getErrors() { return errors; }
    public List<String> getWarnings() { return warnings; }
    public List<String> getHeaders() { return headers; }

    public void addError(String msg) { errors.add(msg); }
    public void addWarning(String msg) { warnings.add(msg); }
}

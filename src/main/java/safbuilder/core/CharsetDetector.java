package safbuilder.core;

import org.mozilla.universalchardet.UniversalDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Detects the character encoding of a file using the juniversalchardet library.
 *
 * <p>Falls back to UTF-8 if detection is inconclusive.</p>
 */
public final class CharsetDetector {

    private static final Logger log = LoggerFactory.getLogger(CharsetDetector.class);

    private CharsetDetector() {}

    /**
     * Detects the charset of the file at the given path.
     *
     * @param filePath Absolute or relative path to the file.
     * @return The detected {@link Charset}, or UTF-8 if detection fails.
     * @throws IOException if the file cannot be read.
     */
    public static Charset detect(String filePath) throws IOException {
        UniversalDetector detector = new UniversalDetector(null);
        byte[] buf = new byte[4096];

        try (FileInputStream fis = new FileInputStream(filePath)) {
            int nread;
            while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
                detector.handleData(buf, 0, nread);
            }
        }
        detector.dataEnd();

        String detected = detector.getDetectedCharset();
        if (detected == null) {
            log.warn("Could not detect charset for '{}', defaulting to UTF-8", filePath);
            return StandardCharsets.UTF_8;
        }

        Charset charset = Charset.forName(detected);
        log.info("Detected charset for '{}': {}", filePath, charset.displayName());
        return charset;
    }
}

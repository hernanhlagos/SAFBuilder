package safbuilder.util;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

/**
 * Utility to create a ZIP archive from a directory tree.
 *
 * <p>Uses Apache Commons Compress for reliable cross-platform ZIP creation.</p>
 */
public final class ZipUtil {

    private static final Logger log = LoggerFactory.getLogger(ZipUtil.class);

    private ZipUtil() {}

    /**
     * Creates a ZIP file at {@code zipPath} containing the full contents of
     * the directory at {@code directoryPath}.
     *
     * @param directoryPath Absolute path to the directory to zip.
     * @param zipPath       Absolute path of the ZIP file to create.
     * @throws IOException if any file operation fails.
     */
    public static void createZip(String directoryPath, String zipPath) throws IOException {
        log.info("Creating ZIP: {} → {}", directoryPath, zipPath);
        try (FileOutputStream fOut     = new FileOutputStream(zipPath);
             BufferedOutputStream bOut = new BufferedOutputStream(fOut);
             ZipArchiveOutputStream zip = new ZipArchiveOutputStream(bOut)) {
            addFileToZip(zip, directoryPath, "");
        }
        log.info("ZIP created successfully: {}", zipPath);
    }

    private static void addFileToZip(ZipArchiveOutputStream zip,
                                     String path, String base) throws IOException {
        File f = new File(path);
        String entryName = base + f.getName();
        ZipArchiveEntry entry = new ZipArchiveEntry(f, entryName);
        zip.putArchiveEntry(entry);

        if (f.isFile()) {
            try (FileInputStream fin = new FileInputStream(f)) {
                fin.transferTo(zip);
            }
            zip.closeArchiveEntry();
        } else {
            zip.closeArchiveEntry();
            File[] children = f.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToZip(zip, child.getAbsolutePath(), entryName + "/");
                }
            }
        }
    }
}

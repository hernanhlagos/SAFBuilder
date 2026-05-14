package safbuilder.core;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.vfs2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import safbuilder.util.ZipUtil;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Core engine that builds a DSpace Simple Archive Format (SAF) package from a
 * CSV metadata file and a directory of content files.
 *
 * <p>This class is UI-agnostic and CLI-agnostic. All progress and error
 * notifications are routed through a {@link ProgressListener}. Logging uses
 * SLF4J so output can be directed to any backend (console, file, UI widget).</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * BuildConfig config = BuildConfig.builder(new File("/path/to/metadata.csv"))
 *         .outputName("MyPackage")
 *         .exportToZip(true)
 *         .build();
 *
 * SAFPackage engine = new SAFPackage();
 * BuildResult result = engine.processMetaPack(config, ProgressListener.SILENT);
 * }</pre>
 */
public class SAFPackage {

    private static final Logger log = LoggerFactory.getLogger(SAFPackage.class);

    /** Separator used for multiple values within a single CSV cell. */
    private static final String SEPARATOR_REGEX = "\\|\\|";

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds a SAF package according to the supplied configuration.
     *
     * @param config   Build parameters (CSV path, output name, zip).
     * @param listener Receives progress/error/completion notifications.
     * @return A {@link BuildResult} describing what was produced.
     */
    public BuildResult processMetaPack(BuildConfig config, ProgressListener listener) {
        Instant startedAt = Instant.now();
        List<String> warnings = new ArrayList<>();
        int itemCount = 0;

        File csvFile      = config.getCsvFile();
        File inputDir     = config.getInputDirectory();
        File outputDir    = new File(config.getOutputDirectory(), config.getOutputName());

        log.info("Starting SAF build — CSV: {}", csvFile.getAbsolutePath());
        listener.onProgress("Iniciando procesamiento de " + csvFile.getName(), 0);

        try {
            // 1. Detect charset and open CSV
            listener.onProgress("Detectando charset del CSV…", 5);
            Charset charset = CharsetDetector.detect(csvFile.getAbsolutePath());
            log.info("Detected CSV charset: {}", charset.displayName());

            if (!"UTF-8".equalsIgnoreCase(charset.name())) {
                throw new IOException("El archivo CSV no es UTF-8 (se detectó " + charset.name() + "). SAFBuilder requiere UTF-8 para garantizar la compatibilidad con DSpace.");
            }

            // 2. Scan all files in the input directory for usage tracking
            listener.onProgress("Escaneando archivos en el directorio de entrada…", 10);
            Map<String, Integer> fileUsage = scanDirectory(inputDir);

            // 3. Prepare clean output directory
            listener.onProgress("Preparando directorio de salida: " + outputDir.getName(), 15);
            prepareOutputDir(outputDir);

            // 4. Parse CSV and build SAF items
            listener.onProgress("Procesando filas del CSV…", 20);
            try (org.apache.commons.csv.CSVParser parser = openCsvParser(csvFile, charset)) {
                List<String> headers = parser.getHeaderNames();
                List<org.apache.commons.csv.CSVRecord> records = parser.getRecords();
                int total = records.size();
                log.info("CSV has {} data rows", total);

                for (int i = 0; i < total; i++) {
                    org.apache.commons.csv.CSVRecord record = records.get(i);
                    int rowNum = i + 1; // 1-based for display
                    int pct = 20 + (int) ((double) i / total * 70);
                    listener.onProgress("Procesando ítem " + rowNum + " de " + total, pct);

                    try {
                        processRow(rowNum, record, headers, inputDir, outputDir, config, fileUsage, warnings);
                        itemCount++;
                    } catch (Exception e) {
                        String msg = "Error procesando fila " + rowNum + ": " + e.getMessage();
                        log.error(msg, e);
                        listener.onWarning(msg, e);
                        warnings.add(msg);
                    }
                }
            }

            // 5. Optional ZIP
            File zipFile = null;
            if (config.isExportToZip()) {
                listener.onProgress("Generando ZIP…", 92);
                File zipDest = new File(config.getOutputDirectory(), config.getOutputName() + ".zip");
                ZipUtil.createZip(outputDir.getAbsolutePath(), zipDest.getAbsolutePath());
                zipFile = zipDest;
                log.info("ZIP created at: {}", zipDest.getAbsolutePath());
            }

            // 6. Report unused files
            fileUsage.forEach((filename, count) -> {
                if (count == 0 && !filename.endsWith(".csv") && !filename.startsWith(".")) {
                    String w = "Archivo no referenciado en el CSV: " + filename;
                    log.warn(w);
                    warnings.add(w);
                }
            });

            listener.onProgress("Completado — " + itemCount + " ítems generados", 100);
            log.info("SAF build completed. Items: {}, warnings: {}", itemCount, warnings.size());

            BuildResult result = BuildResult.builder(startedAt)
                    .success(true)
                    .itemsProcessed(itemCount)
                    .outputDirectory(outputDir)
                    .zipFile(zipFile)
                    .warnings(warnings)
                    .build();

            listener.onComplete(result);
            return result;

        } catch (Exception e) {
            log.error("SAF build failed", e);
            listener.onError("Error fatal: " + e.getMessage(), e);
            return BuildResult.builder(startedAt)
                    .success(false)
                    .itemsProcessed(itemCount)
                    .outputDirectory(outputDir)
                    .warnings(warnings)
                    .build();
        }
    }

    /**
     * Generates a blank CSV manifest listing all files in the directory of
     * the given CSV path.
     *
     * @param csvFile The (possibly non-existent) CSV file to write.
     */
    public void generateManifest(File csvFile) {
        File directory = csvFile.getParentFile();
        log.info("Creating manifest in: {}, output: {}", directory, csvFile);

        String[] standardHeaders = {
            "filename", "dc.title", "dc.contributor.author",
            "dc.date.issued", "dc.description.abstract", "dc.subject"
        };

        try (org.apache.commons.csv.CSVPrinter printer = new org.apache.commons.csv.CSVPrinter(
                new FileWriter(csvFile), org.apache.commons.csv.CSVFormat.DEFAULT)) {
            printer.printRecord((Object[]) standardHeaders);

            File[] files = directory.listFiles();
            if (files == null) {
                log.warn("Directory is empty or inaccessible: {}", directory);
                return;
            }
            Arrays.sort(files);
            int count = 0;
            for (File f : files) {
                if (f.isFile() && !f.getName().startsWith(".")
                        && !f.getName().equals(csvFile.getName())) {
                    printer.print(f.getName());
                    printer.println();
                    count++;
                }
            }
            log.info("Manifest written — {} files listed", count);
        } catch (IOException e) {
            log.error("Error writing manifest", e);
        }
    }

    // -------------------------------------------------------------------------
    // Private — row processing
    // -------------------------------------------------------------------------

    private void processRow(int rowNum, org.apache.commons.csv.CSVRecord record,
                            List<String> headers, File inputDir, File outputDir,
                            BuildConfig config, Map<String, Integer> fileUsage,
                            List<String> warnings) throws IOException {

        File itemDir = new File(outputDir, "item_" + String.format("%03d", rowNum));
        itemDir.mkdirs();

        File contentsFile   = new File(itemDir, "contents");
        File collectionFile = new File(itemDir, "collections");

        // Open XML writers before the try block so the finally can always close them.
        // dc → dublin_core.xml; non-DC schemas → metadata_<schema>.xml
        XmlWriter dcWriter = new XmlWriter(new File(itemDir, "dublin_core.xml"));
        dcWriter.start();  // ← must be called before any writeField() or end()
        Map<String, XmlWriter> nonDcWriters = new LinkedHashMap<>();

        try (BufferedWriter contentsWriter = new BufferedWriter(new FileWriter(contentsFile))) {

            for (int j = 0; j < headers.size(); j++) {
                if (j >= record.size()) break;

                String header = headers.get(j);
                String value  = record.get(j);

                if (StringUtils.isBlank(value)) continue;

                if (isFilenameColumn(header)) {
                    processFiles(contentsWriter, itemDir, inputDir, value, "", config, fileUsage);
                } else if (isFilenameWithParams(header)) {
                    String paramPart = header.split("__", 2)[1];
                    processFiles(contentsWriter, itemDir, inputDir, value, paramPart, config, fileUsage);
                } else if (header.contains("filegroup")) {
                    String paramPart = header.contains("__") ? header.split("__", 2)[1] : "";
                    processFilegroup(contentsWriter, itemDir, inputDir, value, paramPart, fileUsage);
                } else if (header.toLowerCase().contains("collection")) {
                    writeCollections(collectionFile, value);
                } else {
                    // Metadata field — split on '.' to get schema.element.qualifier
                    String[] parts = header.split("\\.");
                    if (parts.length < 2) continue;

                    String schema = parts[0];
                    if ("dc".equals(schema)) {
                        dcWriter.writeField(header, value);
                    } else {
                        nonDcWriters.computeIfAbsent(schema, s -> {
                            try {
                                XmlWriter w = new XmlWriter(new File(itemDir, "metadata_" + s + ".xml"), s);
                                w.start();
                                return w;
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }).writeField(header, value);
                    }
                }
            }

        } finally {
            // Always close writers; end() flushes and closes the underlying stream.
            dcWriter.end();
            for (XmlWriter w : nonDcWriters.values()) w.end();
        }
    }

    // -------------------------------------------------------------------------
    // Private — file processing
    // -------------------------------------------------------------------------

    private static final List<String> FILENAME_COLS = List.of("filename", "bitstream", "bitstreams");
    private static final List<String> FILENAME_PREFIX_COLS = List.of("filename__", "bitstream__", "bitstreams__");

    private boolean isFilenameColumn(String header) {
        return FILENAME_COLS.contains(header.toLowerCase());
    }

    private boolean isFilenameWithParams(String header) {
        String lower = header.toLowerCase();
        return FILENAME_PREFIX_COLS.stream().anyMatch(lower::startsWith);
    }

    private void processFiles(BufferedWriter contentsWriter, File itemDir, File inputDir,
                              String filenames, String globalParams, BuildConfig config,
                              Map<String, Integer> fileUsage) throws IOException {
        for (String entry : filenames.split(SEPARATOR_REGEX)) {
            String[] parts = (entry.trim() + "__").split("__", 2);
            String filename    = parts[0].trim();
            String localParams = parts[1];

            File srcFile = new File(inputDir, filename);
            if (!srcFile.exists()) {
                log.warn("File not found: {} (referenced in CSV)", srcFile.getAbsolutePath());
                return;
            }

            FileUtils.copyFileToDirectory(srcFile, itemDir);

            fileUsage.merge(filename, 1, Integer::sum);

            String row = buildContentsRow(new File(filename).getName(), localParams, globalParams);
            contentsWriter.write(row);
            contentsWriter.newLine();
        }
    }

    @SuppressWarnings("unchecked")
    private void processFilegroup(BufferedWriter contentsWriter, File itemDir, File inputDir,
                                  String filename, String params,
                                  Map<String, Integer> fileUsage) throws FileSystemException {
        FileSystemManager fsm = VFS.getManager();
        FileObject tgz = fsm.resolveFile("tgz://" + new File(inputDir, filename).getAbsolutePath());

        List<FileObject> collected = new ArrayList<>();
        for (FileObject child : tgz.getChildren()) {
            for (FileObject gc : child.getChildren()) {
                if (gc.getType() == FileType.FILE && !gc.getName().getBaseName().equals(".htaccess")) {
                    collected.add(gc);
                }
            }
        }
        collected.sort(new AlphanumFileObjectComparator());
        Collections.reverse(collected);

        FileObject destVfs = fsm.resolveFile("file://" + itemDir.getAbsolutePath());
        for (FileObject fo : collected) {
            try {
                String baseName = fo.getName().getBaseName();
                FileObject dest = fsm.resolveFile(destVfs, baseName);
                dest.createFile();
                dest.copyFrom(fo, Selectors.SELECT_ALL);
                fileUsage.merge(baseName, 1, Integer::sum);

                String row = buildContentsRow(baseName, "", params);
                contentsWriter.write(row);
                contentsWriter.newLine();
            } catch (IOException e) {
                log.error("Error copying filegroup entry: {}", fo.getName().getBaseName(), e);
            }
        }
    }

    private void writeCollections(File collectionFile, String value) throws IOException {
        for (String handle : value.split(SEPARATOR_REGEX)) {
            String h = handle.trim();
            if (!h.isEmpty()) {
                FileUtils.writeStringToFile(collectionFile, h + System.lineSeparator(), "UTF-8", true);
            }
        }
    }

    private String buildContentsRow(String filename, String localParams, String globalParams) {
        StringBuilder sb = new StringBuilder(filename);
        String combined = localParams + "__" + globalParams;
        for (String param : combined.split("__")) {
            String p = param.trim();
            if (!p.isEmpty()) sb.append("\t").append(p);
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Private — helpers
    // -------------------------------------------------------------------------

    private org.apache.commons.csv.CSVParser openCsvParser(File csvFile, Charset charset) throws IOException {
        return org.apache.commons.csv.CSVParser.parse(
                csvFile,
                charset,
                org.apache.commons.csv.CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setIgnoreSurroundingSpaces(true)
                        .build());
    }

    private Map<String, Integer> scanDirectory(File dir) {
        Map<String, Integer> usage = new LinkedHashMap<>();
        File[] files = dir.listFiles(File::isFile);
        if (files != null) {
            for (File f : files) usage.put(f.getName(), 0);
        }
        return usage;
    }

    private void prepareOutputDir(File outputDir) throws IOException {
        if (outputDir.exists()) {
            FileUtils.deleteDirectory(outputDir);
        }
        if (!outputDir.mkdirs()) {
            throw new IOException("Could not create output directory: " + outputDir.getAbsolutePath());
        }
        log.info("Output directory: {}", outputDir.getAbsolutePath());
    }

    // -------------------------------------------------------------------------
    // Inner: alphanum comparator for FileObject
    // -------------------------------------------------------------------------

    private static class AlphanumFileObjectComparator implements Comparator<FileObject> {
        private final safbuilder.util.AlphanumComparator cmp = new safbuilder.util.AlphanumComparator();
        public int compare(FileObject a, FileObject b) {
            try {
                return cmp.compare(a.getName().getBaseName(), b.getName().getBaseName());
            } catch (Exception e) {
                return 0;
            }
        }
    }
}

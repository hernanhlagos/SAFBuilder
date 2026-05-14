package safbuilder.core;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for {@link SAFPackage}.
 *
 * <p>Each test runs the full SAF build pipeline against the sample data
 * bundled in {@code src/sample_data/} and verifies the output structure.</p>
 */
@DisplayName("SAFPackage integration tests")
class SAFPackageTest {

    private static final Path SAMPLE_DATA =
            Paths.get("src", "sample_data").toAbsolutePath();
    private static final Path SAMPLE_CSV  =
            SAMPLE_DATA.resolve("AAA_batch-metadata.csv");

    @TempDir
    Path tempDir;

    private SAFPackage engine;

    @BeforeEach
    void setUp() throws IOException {
        engine = new SAFPackage();
        // Copy sample data into temp dir so we don't pollute the source tree
        FileUtils.copyDirectory(SAMPLE_DATA.toFile(), tempDir.toFile());
    }

    // -------------------------------------------------------------------------
    // Default output name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Default output dir is created and contains item subdirectories")
    void testDefaultOutputDirCreated() throws IOException {
        File csv = tempDir.resolve(SAMPLE_CSV.getFileName()).toFile();
        BuildConfig config = BuildConfig.builder(csv).build();

        BuildResult result = engine.processMetaPack(config, ProgressListener.SILENT);

        assertTrue(result.isSuccess(), "Build should succeed");
        assertTrue(result.getItemsProcessed() > 0, "Should have processed at least one item");

        File outputDir = result.getOutputDirectory();
        assertTrue(outputDir.exists(), "Output directory must exist");

        // At least one item_NNN subdirectory must exist
        File[] items = outputDir.listFiles(f -> f.isDirectory() && f.getName().startsWith("item_"));
        assertNotNull(items);
        assertTrue(items.length > 0, "Output must contain at least one item_NNN directory");
    }

    // -------------------------------------------------------------------------
    // Custom output name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Custom output name is respected")
    void testCustomOutputName() throws IOException {
        String customName = "MyTestOutput_" + System.currentTimeMillis();
        File csv = tempDir.resolve(SAMPLE_CSV.getFileName()).toFile();
        BuildConfig config = BuildConfig.builder(csv)
                .outputName(customName)
                .build();

        BuildResult result = engine.processMetaPack(config, ProgressListener.SILENT);

        assertTrue(result.isSuccess());
        assertEquals(customName, result.getOutputDirectory().getName(),
                "Output directory name must match the configured value");
        assertTrue(result.getOutputDirectory().exists());
    }

    // -------------------------------------------------------------------------
    // ZIP export
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("ZIP file is created when exportToZip is true")
    void testZipCreated() throws IOException {
        File csv = tempDir.resolve(SAMPLE_CSV.getFileName()).toFile();
        BuildConfig config = BuildConfig.builder(csv)
                .exportToZip(true)
                .build();

        BuildResult result = engine.processMetaPack(config, ProgressListener.SILENT);

        assertTrue(result.isSuccess());
        assertNotNull(result.getZipFile(), "Zip file reference must not be null");
        assertTrue(result.getZipFile().exists(), "ZIP file must exist on disk");
        assertTrue(result.getZipFile().length() > 0, "ZIP file must not be empty");
    }

    // -------------------------------------------------------------------------
    // dublin_core.xml is created for each item
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Each item directory contains a dublin_core.xml file")
    void testDublinCoreXmlPresent() throws IOException {
        File csv = tempDir.resolve(SAMPLE_CSV.getFileName()).toFile();
        BuildConfig config = BuildConfig.builder(csv).build();

        BuildResult result = engine.processMetaPack(config, ProgressListener.SILENT);
        assertTrue(result.isSuccess());

        File[] items = result.getOutputDirectory()
                .listFiles(f -> f.isDirectory() && f.getName().startsWith("item_"));
        assertNotNull(items);
        assertTrue(items.length > 0);

        for (File item : items) {
            File dcXml = new File(item, "dublin_core.xml");
            assertTrue(dcXml.exists(),
                    "dublin_core.xml must exist in " + item.getName());
            assertTrue(dcXml.length() > 0,
                    "dublin_core.xml must not be empty in " + item.getName());
        }
    }

    // -------------------------------------------------------------------------
    // contents file is created for each item
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Each item directory contains a non-empty contents file")
    void testContentsFilePresent() throws IOException {
        File csv = tempDir.resolve(SAMPLE_CSV.getFileName()).toFile();
        BuildConfig config = BuildConfig.builder(csv).build();

        BuildResult result = engine.processMetaPack(config, ProgressListener.SILENT);
        assertTrue(result.isSuccess());

        File[] items = result.getOutputDirectory()
                .listFiles(f -> f.isDirectory() && f.getName().startsWith("item_"));
        assertNotNull(items);

        for (File item : items) {
            File contents = new File(item, "contents");
            assertTrue(contents.exists(),
                    "contents file must exist in " + item.getName());
        }
    }

    // -------------------------------------------------------------------------
    // BuildConfig validation
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("BuildConfig rejects null csvFile")
    void testBuildConfigRejectsNull() {
        assertThrows(IllegalArgumentException.class,
                () -> BuildConfig.builder(null).build());
    }

    @Test
    @DisplayName("BuildConfig rejects non-existent file")
    void testBuildConfigRejectsNonExistent() {
        File ghost = tempDir.resolve("ghost_does_not_exist.csv").toFile();
        assertThrows(IllegalArgumentException.class,
                () -> BuildConfig.builder(ghost).build());
    }

    @Test
    @DisplayName("BuildConfig default output name is SimpleArchiveFormat")
    void testBuildConfigDefaultOutputName() throws IOException {
        File csv = tempDir.resolve(SAMPLE_CSV.getFileName()).toFile();
        BuildConfig config = BuildConfig.builder(csv).build();
        assertEquals("SimpleArchiveFormat", config.getOutputName());
    }
}

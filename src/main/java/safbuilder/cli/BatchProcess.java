package safbuilder.cli;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import safbuilder.core.BuildConfig;
import safbuilder.core.BuildResult;
import safbuilder.core.ProgressListener;
import safbuilder.core.SAFPackage;

import java.io.File;

/**
 * Command-line entry point for SAFBuilder.
 *
 * <p>Parses arguments and delegates all work to {@link SAFPackage} in the
 * core layer. No business logic lives here.</p>
 *
 * <p>Usage:</p>
 * <pre>
 *   safbuilder -c /path/to/metadata.csv [-z] [-o OutputName] [-s]
 *   safbuilder -c /path/to/manifest.csv -m
 * </pre>
 */
public class BatchProcess {

    private static final Logger log = LoggerFactory.getLogger(BatchProcess.class);

    public static void main(String[] args) {
        Options options = buildOptions();
        CommandLineParser parser = new DefaultParser();

        CommandLine cmd;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.err.println("Error: " + e.getMessage());
            printHelp(options);
            System.exit(1);
            return;
        }

        if (cmd.hasOption('h') || !cmd.hasOption('c')) {
            printHelp(options);
            System.exit(0);
            return;
        }

        String csvPath = cmd.getOptionValue('c');
        File   csvFile = new File(csvPath);

        SAFPackage engine = new SAFPackage();

        // --- Manifest generation mode ---
        if (cmd.hasOption('m')) {
            log.info("Generating manifest at: {}", csvFile.getAbsolutePath());
            engine.generateManifest(csvFile);
            return;
        }

        // --- Build mode ---
        BuildConfig config;
        try {
            BuildConfig.Builder builder = BuildConfig.builder(csvFile)
                    .exportToZip(cmd.hasOption('z'))
                    .useSymbolicLinks(cmd.hasOption('s'));

            if (cmd.hasOption('o')) {
                builder.outputName(cmd.getOptionValue('o'));
            }
            config = builder.build();
        } catch (IllegalArgumentException e) {
            System.err.println("Error en la configuración: " + e.getMessage());
            System.exit(1);
            return;
        }

        // Console progress listener — prints to stdout/stderr
        ProgressListener listener = new ConsoleProgressListener();
        BuildResult result = engine.processMetaPack(config, listener);

        if (!result.isSuccess()) {
            System.err.println("El proceso terminó con errores. Revise el log para más detalles.");
            System.exit(2);
        }

        if (!result.getWarnings().isEmpty()) {
            System.out.println("\nAdvertencias (" + result.getWarnings().size() + "):");
            result.getWarnings().forEach(w -> System.out.println("  ⚠ " + w));
        }

        System.out.printf("%nCompletado: %d ítems procesados en %d ms%n",
                result.getItemsProcessed(), result.getDuration().toMillis());
    }

    // -------------------------------------------------------------------------

    private static Options buildOptions() {
        Options opts = new Options();
        opts.addOption("c", "csv",          true,  "Ruta al archivo CSV de metadatos (debe estar en el mismo directorio que los archivos de contenido).");
        opts.addOption("h", "help",         false, "Muestra esta ayuda.");
        opts.addOption("m", "manifest",     false, "Genera un CSV de manifiesto listando todos los archivos del directorio. Requiere -c.");
        opts.addOption("s", "symbolic-link",false, "Crea enlaces simbólicos en lugar de copiar los archivos.");
        opts.addOption("z", "zip",          false, "(Opcional) Comprime el paquete SAF en un archivo ZIP.");
        opts.addOption("o", "output-name",  true,  "(Opcional) Nombre personalizado para el directorio de salida. Por defecto: SimpleArchiveFormat.");
        return opts;
    }

    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(100);
        formatter.printHelp("safbuilder -c <ruta/al/archivo.csv> [opciones]", "\nOpciones:\n", options, "\nEjemplos:\n"
                + "  safbuilder -c /datos/metadata.csv -z\n"
                + "  safbuilder -c /datos/metadata.csv -o MiPaquete -z\n"
                + "  safbuilder -c /datos/metadata.csv -m\n");
    }
}

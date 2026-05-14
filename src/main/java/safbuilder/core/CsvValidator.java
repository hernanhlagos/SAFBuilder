package safbuilder.core;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

/**
 * Validates the structure and consistency of the metadata CSV.
 */
public class CsvValidator {
    private static final Logger log = LoggerFactory.getLogger(CsvValidator.class);

    public ValidationResult validate(File csvFile) {
        ValidationResult result = new ValidationResult(true);
        if (!csvFile.exists()) {
            result.addError("El archivo no existe.");
            return new ValidationResult(false);
        }

        try {
            Charset charset = CharsetDetector.detect(csvFile.getAbsolutePath());
            result.setDetectedCharset(charset.name());
            
            if (!"UTF-8".equalsIgnoreCase(charset.name())) {
                result.addError("El archivo NO es UTF-8 (se detectó " + charset.name() + "). DSpace requiere UTF-8 para evitar corrupción de caracteres. Por favor, guarde su CSV como 'CSV UTF-8 (delimitado por comas)' en Excel.");
                return result;
            }

            try (CSVParser parser = CSVParser.parse(csvFile, charset, 
                    CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build())) {
                
                List<String> headers = parser.getHeaderNames();
                result.getHeaders().addAll(headers);

                // 1. Verificar columna obligatoria
                boolean hasFileCol = headers.stream()
                        .anyMatch(h -> h.toLowerCase().startsWith("filename") || 
                                       h.toLowerCase().startsWith("bitstream"));
                
                if (!hasFileCol) {
                    result.addError("Falta la columna obligatoria 'filename' o 'bitstream'.");
                }

                // 2. Verificar que existan columnas de metadatos (ej: dc.title)
                boolean hasMetadata = headers.stream().anyMatch(h -> h.contains("."));
                if (!hasMetadata) {
                    result.addWarning("No se detectaron columnas de metadatos (ej: dc.title). El paquete SAF estará vacío de metadatos.");
                }

                // 3. Verificación rápida de la primera fila (opcional)
                if (parser.iterator().hasNext()) {
                    log.debug("CSV structure seems OK for file: {}", csvFile.getName());
                } else {
                    result.addWarning("El CSV parece estar vacío (solo tiene encabezados).");
                }

            }
        } catch (IOException e) {
            log.error("Error validando CSV", e);
            result.addError("Error al leer el archivo: " + e.getMessage());
        }

        return result;
    }
}

package safbuilder.core;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Writes DSpace Dublin Core (or other schema) XML metadata files.
 *
 * <p>Replaces the original {@code OutputXML} + the abandoned {@code xmlwriter}
 * library with standard Java {@link javax.xml.stream.XMLStreamWriter} (StAX),
 * which is part of the JDK and requires no extra dependency.</p>
 *
 * <p>Output format example:</p>
 * <pre>{@code
 * <?xml version="1.0" encoding="UTF-8"?>
 * <dublin_core schema="local">
 *   <dcvalue element="title" qualifier="alternative" language="en">Value</dcvalue>
 * </dublin_core>
 * }</pre>
 */
public class XmlWriter implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(XmlWriter.class);
    private static final Pattern LANGUAGE_PATTERN = Pattern.compile("\\[(.*?)\\]");
    private static final String SEPARATOR_REGEX   = "\\|\\|";

    private final javax.xml.stream.XMLStreamWriter xml;
    private final OutputStream out;
    private final String schema;

    /**
     * Creates an XmlWriter for the Dublin Core schema.
     *
     * @param outputFile The file to write (will be created/overwritten).
     * @throws IOException if the file cannot be opened.
     */
    public XmlWriter(File outputFile) throws IOException {
        this(outputFile, "dc");
    }

    /**
     * Creates an XmlWriter for a named schema.
     *
     * @param outputFile The file to write.
     * @param schema     Metadata schema prefix (e.g. {@code "dc"}, {@code "local"}).
     * @throws IOException if the file cannot be opened.
     */
    public XmlWriter(File outputFile, String schema) throws IOException {
        this.schema = schema;
        this.out    = new BufferedOutputStream(new FileOutputStream(outputFile));
        try {
            javax.xml.stream.XMLOutputFactory factory = javax.xml.stream.XMLOutputFactory.newInstance();
            this.xml = factory.createXMLStreamWriter(out, "UTF-8");
        } catch (javax.xml.stream.XMLStreamException e) {
            out.close();
            throw new IOException("Failed to create XMLStreamWriter for: " + outputFile, e);
        }
    }

    /**
     * Writes the XML declaration and the root {@code <dublin_core>} element.
     * Must be called once before any {@link #writeField} calls.
     */
    public void start() throws IOException {
        try {
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeCharacters("\n");
            xml.writeStartElement("dublin_core");
            if (!"dc".equalsIgnoreCase(schema)) {
                xml.writeAttribute("schema", schema);
            }
            xml.writeCharacters("\n");
        } catch (javax.xml.stream.XMLStreamException e) {
            throw new IOException("Error writing XML start", e);
        }
    }

    /**
     * Writes one or more {@code <dcvalue>} elements for the given field.
     *
     * <p>Supports multiple values separated by {@code ||} and language tags
     * such as {@code dc.title[en]}.</p>
     *
     * @param fieldHeader Full field name, e.g. {@code dc.title} or
     *                    {@code dc.description.abstract[fr]}.
     * @param fieldValue  Raw cell value from the CSV; may contain {@code ||}.
     */
    public void writeField(String fieldHeader, String fieldValue) throws IOException {
        if (StringUtils.isBlank(fieldValue)) return;

        for (String raw : fieldValue.split(SEPARATOR_REGEX)) {
            String value = raw.trim();
            if (value.isEmpty()) continue;
            writeOneDcValue(fieldHeader, value);
        }
    }

    /** Closes the root element, flushes and closes the underlying stream. */
    public void end() throws IOException {
        try {
            xml.writeCharacters("\n");
            xml.writeEndElement();   // </dublin_core>
            xml.writeEndDocument();
            xml.flush();
            xml.close();
        } catch (javax.xml.stream.XMLStreamException e) {
            throw new IOException("Error writing XML end", e);
        } finally {
            out.close();
        }
    }

    /** Alias for {@link #end()}; satisfies {@link Closeable}. */
    @Override
    public void close() throws IOException {
        end();
    }

    // -------------------------------------------------------------------------
    // Private
    // -------------------------------------------------------------------------

    private void writeOneDcValue(String fieldHeader, String value) throws IOException {
        // Strip language tag from the field name before splitting
        String fieldWithoutLang;
        if (fieldHeader.contains("[")) {
            fieldWithoutLang = fieldHeader.split("\\[")[0];
        } else {
            fieldWithoutLang = fieldHeader;
        }

        String[] pieces = fieldWithoutLang.split("\\.");
        String element   = pieces.length > 1 ? pieces[1] : "";
        String qualifier = pieces.length > 2 ? pieces[2] : "";

        // Extract language, e.g. dc.title[en] → "en"
        Matcher langMatcher = LANGUAGE_PATTERN.matcher(fieldHeader);
        String language = langMatcher.find() ? langMatcher.group(1) : null;

        try {
            xml.writeCharacters("  ");
            xml.writeStartElement("dcvalue");
            if (!element.isEmpty())   xml.writeAttribute("element",   element);
            if (!qualifier.isEmpty()) xml.writeAttribute("qualifier", qualifier);
            if (language  != null)    xml.writeAttribute("language",  language);
            xml.writeCharacters(value);
            xml.writeEndElement();
            xml.writeCharacters("\n");
        } catch (javax.xml.stream.XMLStreamException e) {
            throw new IOException("Error writing dcvalue for field: " + fieldHeader, e);
        }
    }
}

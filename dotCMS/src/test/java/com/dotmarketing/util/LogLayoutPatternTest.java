package com.dotmarketing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.DefaultConfiguration;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Checks the log layouts dotCMS ships: the webapp {@code log4j2.xml} and the default patterns the
 * Docker image exports. A line break inside a log message is written as text, so every message
 * stays on its own line, while stack traces keep one frame per line. Other control characters in
 * a message or in exception text are written as {@code ?}.
 */
class LogLayoutPatternTest {

    private static final File WEBAPP_CONFIG = new File("src/main/webapp/WEB-INF/log4j/log4j2.xml");
    private static final File DOCKER_DEFAULTS =
            new File("src/main/docker/original/ROOT/srv/35-set-logging-defaults.sh");

    private static final String MESSAGE = "first part\r\nsecond part";
    private static final File EXAMPLE_CONFIG =
            new File("src/main/webapp/WEB-INF/log4j/log4j2-example.xml");
    private static final String CR_EXCEPTION_TEXT = "cause\rnext";
    private static final String CONTROL_MESSAGE =
            "a\u001b[2Kb\u0007c\u2028d\u2029e\u0085f col1\tcol2 \u00e9\u65e5\u672c";

    /**
     * Given the console layout from the webapp log4j2.xml,
     * When a message containing a line break is formatted,
     * Then the message is written on one line and keeps its text.
     */
    @Test
    void webappConsolePattern_keepsMessageOnOneLine() throws IOException {
        final Layout<?> layout = webappConsoleLayout();

        assertMessageOnOneLine(format(layout, event(MESSAGE, null)));
    }

    /**
     * Given the console layout from the webapp log4j2.xml,
     * When an event with an exception is formatted,
     * Then the stack trace is still written one frame per line.
     */
    @Test
    void webappConsolePattern_keepsStackTraceFrames() throws IOException {
        final Layout<?> layout = webappConsoleLayout();

        assertStackTraceFrames(format(layout, event("failure", new IllegalStateException("cause"))));
    }

    /**
     * Given the default console and file patterns the Docker image exports,
     * When a message containing a line break is formatted,
     * Then the message is written on one line and keeps its text.
     */
    @Test
    void dockerDefaultPatterns_keepMessageOnOneLine() throws Exception {
        for (final String pattern : dockerDefaultPatterns()) {
            assertMessageOnOneLine(format(layout(pattern), event(MESSAGE, null)));
        }
    }

    /**
     * Given the default console and file patterns the Docker image exports,
     * When an event with an exception is formatted,
     * Then the stack trace is still written one frame per line.
     */
    @Test
    void dockerDefaultPatterns_keepStackTraceFrames() throws Exception {
        for (final String pattern : dockerDefaultPatterns()) {
            assertStackTraceFrames(format(layout(pattern),
                    event("failure", new IllegalStateException("cause"))));
        }
    }

    /**
     * Given the console layout from the webapp log4j2.xml,
     * When a message holds terminal control characters and Unicode line separators,
     * Then each one is written as {@code ?}, while tabs and non-ASCII letters are kept.
     */
    @Test
    void webappConsolePattern_writesControlCharactersAsPlaceholders() throws IOException {
        assertControlCharactersReplaced(format(webappConsoleLayout(), event(CONTROL_MESSAGE, null)));
    }

    /**
     * Given the console layout from the webapp log4j2.xml,
     * When the exception text holds a terminal control character,
     * Then it is written as {@code ?} and the stack trace is printed once, one frame per line.
     */
    @Test
    void webappConsolePattern_writesControlCharactersInExceptionTextAsPlaceholders()
            throws IOException {
        assertExceptionTextReplaced(format(webappConsoleLayout(), event("failure", controlException())));
    }

    /**
     * Given the default console and file patterns the Docker image exports,
     * When a message holds terminal control characters and Unicode line separators,
     * Then each one is written as {@code ?}, while tabs and non-ASCII letters are kept.
     */
    @Test
    void dockerDefaultPatterns_writeControlCharactersAsPlaceholders() throws Exception {
        for (final String pattern : dockerDefaultPatterns()) {
            assertControlCharactersReplaced(format(layout(pattern), event(CONTROL_MESSAGE, null)));
        }
    }

    /**
     * Given the default console and file patterns the Docker image exports,
     * When the exception text holds a terminal control character,
     * Then it is written as {@code ?} and the stack trace is printed once, one frame per line.
     */
    @Test
    void dockerDefaultPatterns_writeControlCharactersInExceptionTextAsPlaceholders()
            throws Exception {
        for (final String pattern : dockerDefaultPatterns()) {
            assertExceptionTextReplaced(format(layout(pattern), event("failure", controlException())));
        }
    }

    /**
     * Given the console layout from the webapp log4j2.xml,
     * When the exception text holds a carriage return that is not part of a line end,
     * Then it is written as {@code ?}.
     */
    @Test
    void webappConsolePattern_writesBareCarriageReturnInExceptionTextAsPlaceholder()
            throws IOException {
        assertBareCarriageReturnReplaced(format(webappConsoleLayout(),
                event("failure", new IllegalStateException(CR_EXCEPTION_TEXT))));
    }

    /**
     * Given the default console and file patterns the Docker image exports,
     * When the exception text holds a carriage return that is not part of a line end,
     * Then it is written as {@code ?}.
     */
    @Test
    void dockerDefaultPatterns_writeBareCarriageReturnInExceptionTextAsPlaceholder()
            throws Exception {
        for (final String pattern : dockerDefaultPatterns()) {
            assertBareCarriageReturnReplaced(format(layout(pattern),
                    event("failure", new IllegalStateException(CR_EXCEPTION_TEXT))));
        }
    }

    /**
     * Given the four message patterns in log4j2-example.xml,
     * When a message or exception text holds control characters,
     * Then each one is written as {@code ?}, and the stack trace is printed once.
     */
    @Test
    void examplePatterns_writeControlCharactersAsPlaceholders() throws Exception {
        for (final String pattern : examplePatterns()) {
            assertControlCharactersReplaced(format(layout(pattern), event(CONTROL_MESSAGE, null)));
            assertExceptionTextReplaced(format(layout(pattern), event("failure", controlException())));
            assertBareCarriageReturnReplaced(format(layout(pattern),
                    event("failure", new IllegalStateException(CR_EXCEPTION_TEXT))));
        }
    }

    private static void assertBareCarriageReturnReplaced(final String output) {
        assertTrue(output.contains("IllegalStateException: cause?next"),
                () -> "output: " + visible(output));
        assertStackTraceFrames(output.replace("cause?next", "cause"));
    }

    /**
     * Reads the message patterns from log4j2-example.xml, with {@code ${CONTROL_CHARS}} resolved.
     */
    private static List<String> examplePatterns() throws Exception {
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        final Document document = factory.newDocumentBuilder().parse(EXAMPLE_CONFIG);
        final NodeList nodes = document.getElementsByTagName("Property");
        final Map<String, String> properties = new HashMap<>();
        for (int i = 0; i < nodes.getLength(); i++) {
            final Element property = (Element) nodes.item(i);
            properties.put(property.getAttribute("name"), property.getTextContent());
        }
        final List<String> patterns = new ArrayList<>();
        for (final String name : List.of("PATTERN", "CONSOLE_PATTERN", "MESSAGE_PATTERN",
                "MDC_MESSAGE_PATTERN")) {
            assertTrue(properties.containsKey(name), () -> "missing property " + name);
            final String pattern = properties.get(name)
                    .replace("${CONTROL_CHARS}", properties.getOrDefault("CONTROL_CHARS", ""));
            assertFalse(pattern.contains("${"), () -> name + " still has a property: " + pattern);
            patterns.add(pattern);
        }
        return patterns;
    }

    private static IllegalStateException controlException() {
        return new IllegalStateException("cause\u001b[1Aend");
    }

    private static void assertControlCharactersReplaced(final String output) {
        assertNoControlCharacters(output);
        assertTrue(output.contains("a?[2Kb?c?d?e?f"), () -> "output: " + visible(output));
        assertTrue(output.contains("col1\tcol2"), () -> "output: " + visible(output));
        assertTrue(output.contains("\u00e9\u65e5\u672c"), () -> "output: " + visible(output));
    }

    private static void assertExceptionTextReplaced(final String output) {
        assertNoControlCharacters(output);
        assertTrue(output.contains("IllegalStateException: cause?[1Aend"),
                () -> "output: " + visible(output));
        assertEquals(1, output.split("IllegalStateException", -1).length - 1,
                () -> "the stack trace is printed once: " + visible(output));
        assertStackTraceFrames(output.replace("cause?[1Aend", "cause"));
    }

    /** Fails on any character a terminal could act on, other than tab and line ends. */
    private static void assertNoControlCharacters(final String output) {
        for (final char c : output.toCharArray()) {
            final boolean allowed = c == '\t' || c == '\n' || c == '\r';
            assertFalse(!allowed && (Character.isISOControl(c) || c == '\u2028' || c == '\u2029'),
                    () -> "output: " + visible(output));
        }
    }

    /** Shows control characters as their code point, so a failure message is readable. */
    private static String visible(final String output) {
        final StringBuilder builder = new StringBuilder();
        for (final char c : output.toCharArray()) {
            if (c != '\n' && c != '\t' && (Character.isISOControl(c) || c == '\u2028' || c == '\u2029')) {
                builder.append(String.format("<U+%04X>", (int) c));
            } else {
                builder.append(c);
            }
        }
        return builder.toString();
    }

    private static void assertMessageOnOneLine(final String output) {
        final String line = output.endsWith(System.lineSeparator())
                ? output.substring(0, output.length() - System.lineSeparator().length())
                : output;
        assertFalse(line.contains("\n") || line.contains("\r"), () -> "output: " + output);
        assertTrue(line.contains("first part") && line.contains("second part"),
                () -> "output: " + output);
    }

    private static void assertStackTraceFrames(final String output) {
        assertTrue(output.contains("IllegalStateException: cause"), () -> "output: " + output);
        assertTrue(output.contains("\n\tat "), () -> "output: " + output);
    }

    private static Layout<?> webappConsoleLayout() throws IOException {
        final LoggerContext context = new LoggerContext("LogLayoutPatternTest");
        try (InputStream in = new FileInputStream(WEBAPP_CONFIG)) {
            final XmlConfiguration configuration =
                    new XmlConfiguration(context, new ConfigurationSource(in, WEBAPP_CONFIG));
            configuration.initialize();
            // The default pattern, not ${CONSOLE_PATTERN}: that one takes CMS_LOG4J_CONSOLE_PATTERN
            // from the environment when it is set.
            return PatternLayout.newBuilder()
                    .withConfiguration(configuration)
                    .withPattern(configuration.getStrSubstitutor().replace("${DEFAULT_CONSOLE_PATTERN}"))
                    .build();
        }
    }

    /**
     * Sources the Docker defaults script in a clean shell and returns the console and file
     * patterns it exports.
     */
    private static List<String> dockerDefaultPatterns() throws Exception {
        final ProcessBuilder builder = new ProcessBuilder("bash", "-c",
                ". \"$0\" && printf '%s\\0%s' \"$CMS_LOG4J_CONSOLE_PATTERN\" \"$CMS_LOG4J_MESSAGE_PATTERN\"",
                DOCKER_DEFAULTS.getAbsolutePath());
        builder.environment().remove("CMS_LOG4J_CONSOLE_PATTERN");
        builder.environment().remove("CMS_LOG4J_MESSAGE_PATTERN");
        builder.environment().remove("CMS_LOG4J_STACK_FILTER");
        builder.redirectErrorStream(true);
        final Process process = builder.start();
        final String output = new String(process.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
        assertTrue(process.waitFor(30, TimeUnit.SECONDS), "the defaults script finished");
        assertEquals(0, process.exitValue(), () -> "script output: " + output);
        final List<String> patterns = List.of(output.split("\0"));
        assertEquals(2, patterns.size(), () -> "script output: " + output);
        return patterns;
    }

    private static Layout<?> layout(final String pattern) {
        return PatternLayout.newBuilder()
                .withConfiguration(new DefaultConfiguration())
                .withPattern(pattern)
                .build();
    }

    private static LogEvent event(final String message, final Throwable thrown) {
        return Log4jLogEvent.newBuilder()
                .setLoggerName(LogLayoutPatternTest.class.getName())
                .setLevel(Level.WARN)
                .setMessage(new SimpleMessage(message))
                .setThrown(thrown)
                .build();
    }

    private static String format(final Layout<?> layout, final LogEvent event) {
        return new String(layout.toByteArray(event), StandardCharsets.UTF_8);
    }
}

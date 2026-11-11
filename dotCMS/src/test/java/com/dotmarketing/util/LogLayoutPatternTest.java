package com.dotmarketing.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

/**
 * Checks the log layouts dotCMS ships: the webapp {@code log4j2.xml} and the default patterns the
 * Docker image exports. A line break inside a log message is written as text, so every message
 * stays on its own line, while stack traces keep one frame per line.
 */
class LogLayoutPatternTest {

    private static final File WEBAPP_CONFIG = new File("src/main/webapp/WEB-INF/log4j/log4j2.xml");
    private static final File DOCKER_DEFAULTS =
            new File("src/main/docker/original/ROOT/srv/35-set-logging-defaults.sh");

    private static final String MESSAGE = "first part\r\nsecond part";

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
            return configuration.getAppender("Console").getLayout();
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

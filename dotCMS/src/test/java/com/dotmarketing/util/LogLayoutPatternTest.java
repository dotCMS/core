package com.dotmarketing.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.ConfigurationSource;
import org.apache.logging.log4j.core.config.xml.XmlConfiguration;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.SimpleMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Checks the log layouts dotCMS ships: the webapp {@code log4j2.xml} and the one the Docker image
 * installs ({@code OVERRIDE/WEB-INF/log4j/log4j2.xml}). A line break inside a log message is
 * written as text and any other control character as "?", so every message stays on its own
 * line, while stack traces keep one frame per line.
 */
class LogLayoutPatternTest {

    private static final File WEBAPP_CONFIG = new File("src/main/webapp/WEB-INF/log4j/log4j2.xml");
    private static final File DOCKER_CONFIG =
            new File("src/main/docker/original/ROOT/srv/OVERRIDE/WEB-INF/log4j/log4j2.xml");

    private static final String MESSAGE = "first part\r\nsecond part";
    private static final String CATALINA_HOME = "catalina.home";

    private static String previousCatalinaHome;

    @BeforeAll
    static void setCatalinaHome() {
        previousCatalinaHome = System.getProperty(CATALINA_HOME);
        System.setProperty(CATALINA_HOME, System.getProperty("java.io.tmpdir"));
    }

    @AfterAll
    static void restoreCatalinaHome() {
        if (previousCatalinaHome == null) {
            System.clearProperty(CATALINA_HOME);
        } else {
            System.setProperty(CATALINA_HOME, previousCatalinaHome);
        }
    }

    /**
     * Given the console layout of each shipped log4j2.xml,
     * When a message containing a line break is formatted,
     * Then the message is written on one line and keeps its text.
     */
    @Test
    void consolePatterns_keepMessageOnOneLine() throws IOException {
        for (final Layout<?> layout : layouts()) {
            assertMessageOnOneLine(format(layout, event(MESSAGE, null)));
        }
    }

    /**
     * Given the console layout of each shipped log4j2.xml,
     * When a message with control characters other than tab is formatted,
     * Then each is written as "?" and the tab is kept.
     */
    @Test
    void consolePatterns_replaceControlCharacters() throws IOException {
        for (final Layout<?> layout : layouts()) {
            final String output = format(layout, event("a\u0000b\u001bc\u0085d\u2028e\tf", null));

            assertTrue(output.contains("a?b?c?d?e\tf"), () -> "output: " + output);
        }
    }

    /**
     * Given the console layout of each shipped log4j2.xml,
     * When an event with an exception is formatted,
     * Then the stack trace is still written one frame per line.
     */
    @Test
    void consolePatterns_keepStackTraceFrames() throws IOException {
        for (final Layout<?> layout : layouts()) {
            final String output = format(layout, event("failure", new IllegalStateException("cause")));

            assertTrue(output.contains("IllegalStateException: cause"), () -> "output: " + output);
            assertTrue(output.contains("\n\tat "), () -> "output: " + output);
        }
    }

    /**
     * Given the console layout of each shipped log4j2.xml,
     * When an exception whose message contains a line break and a control character is formatted,
     * Then the trace is written once and the control character is replaced.
     */
    @Test
    void consolePatterns_sanitizeExceptionText() throws IOException {
        for (final Layout<?> layout : layouts()) {
            final String output = format(layout,
                    event("failure", new IllegalStateException("bad\u0007value")));

            assertTrue(output.contains("IllegalStateException: bad?value"), () -> "output: " + output);
            assertFalse(output.contains("\u0007"), () -> "output: " + output);
            assertTrue(output.indexOf("IllegalStateException") == output.lastIndexOf("IllegalStateException"),
                    () -> "trace written twice: " + output);
        }
    }

    /**
     * Given the file layout of the Docker image's log4j2.xml,
     * When a message containing a line break is formatted,
     * Then the message is written on one line and keeps its text.
     */
    @Test
    void dockerFilePattern_keepsMessageOnOneLine() throws IOException {
        assertMessageOnOneLine(format(appenderLayout(DOCKER_CONFIG, "Generic-File"), event(MESSAGE, null)));
    }

    private static Layout<?>[] layouts() throws IOException {
        return new Layout<?>[] {appenderLayout(WEBAPP_CONFIG, "Console"),
                appenderLayout(DOCKER_CONFIG, "Console")};
    }

    private static void assertMessageOnOneLine(final String output) {
        final String line = output.endsWith(System.lineSeparator())
                ? output.substring(0, output.length() - System.lineSeparator().length())
                : output;
        assertFalse(line.contains("\n") || line.contains("\r"), () -> "output: " + output);
        assertTrue(line.contains("first part") && line.contains("second part"),
                () -> "output: " + output);
    }

    private static Layout<?> appenderLayout(final File file, final String appender) throws IOException {
        final LoggerContext context = new LoggerContext("LogLayoutPatternTest");
        try (InputStream in = new FileInputStream(file)) {
            final XmlConfiguration configuration =
                    new XmlConfiguration(context, new ConfigurationSource(in, file));
            configuration.initialize();
            return configuration.getAppender(appender).getLayout();
        }
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

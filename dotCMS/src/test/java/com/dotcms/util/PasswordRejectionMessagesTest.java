package com.dotcms.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import org.junit.Test;

/**
 * Checks that the three password rejection messages shipped in the language files state the
 * password requirements: 15 to 64 characters, and in English the four character classes.
 */
public class PasswordRejectionMessagesTest {

    private static final Path MESSAGES = Paths.get("src/main/webapp/WEB-INF/messages");
    private static final String[] KEYS = {"User-Info-Save-Password-Failed",
            "reset-password-invalid-password", "please-enter-a-valid-password"};
    private static final String[] FILES = {"Language.properties", "Language_de.properties",
            "Language_es.properties", "Language_fi.properties", "Language_fr.properties",
            "Language_it.properties", "Language_nl.properties", "Language_ru.properties",
            "Language_zh_CN.properties"};

    private static Properties load(final String file) throws IOException {
        final Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(MESSAGES.resolve(file), StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    /**
     * Method to test: the three rejection messages in every language file.
     * Given Scenario: The messages as shipped.
     * Expected Result: each one states 15 and 64 and no longer states the old 8-character rule.
     */
    @Test
    public void test_messages_stateLengthRangeInEveryLanguage() throws IOException {
        for (final String file : FILES) {
            final Properties properties = load(file);
            for (final String key : KEYS) {
                final String text = properties.getProperty(key);
                assertNotNull(file + " must define " + key, text);
                assertTrue(file + " " + key + " must state 15: " + text, text.contains("15"));
                assertTrue(file + " " + key + " must state 64: " + text, text.contains("64"));
                assertFalse(file + " " + key + " must not state the old minimum: " + text,
                        text.matches(".*\\b8\\b.*"));
            }
        }
    }

    /**
     * Method to test: the three rejection messages in English.
     * Given Scenario: The messages as shipped.
     * Expected Result: each one names the four required character classes, and none lists the old
     * special characters.
     */
    @Test
    public void test_englishMessages_nameTheFourClasses() throws IOException {
        final Properties properties = load("Language.properties");
        for (final String key : KEYS) {
            final String text = properties.getProperty(key);
            for (final String requirement : new String[] {"uppercase", "lowercase", "digit", "special"}) {
                assertTrue(key + " must name " + requirement + ": " + text, text.contains(requirement));
            }
            assertFalse(key + " must not list the old special characters: " + text,
                    text.contains("# % + : = ? @"));
        }
    }
}

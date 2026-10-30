package com.dotmarketing.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.liferay.portal.pwd.RegExpToolkit;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Pattern;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Covers how {@link PasswordGenerator} reads the default {@code passwords.regexptoolkit.pattern}
 * shipped in {@code portal.properties}: a run of lookaheads followed by a character class and a
 * {@code {min,max}} quantifier. The pattern is read from the file itself so the test cannot drift
 * from what the product ships.
 */
public class PasswordGeneratorPolicyShapeTest {

    private static final String TOOLKIT = "com.liferay.portal.pwd.RegExpToolkit";
    private static String defaultPattern;
    private static RegExpToolkit validator;

    @BeforeClass
    public static void loadDefaultPattern() throws IOException {
        final Properties properties = new Properties();
        try (InputStream stream = PasswordGeneratorPolicyShapeTest.class.getResourceAsStream("/portal.properties")) {
            assertNotNull("portal.properties must be on the classpath", stream);
            properties.load(stream);
        }
        defaultPattern = properties.getProperty("passwords.regexptoolkit.pattern");
        assertNotNull("the default pattern must be defined", defaultPattern);
        validator = new RegExpToolkit(defaultPattern);
    }

    /**
     * Method to test: {@link PasswordGenerator.Builder#extractCharClass(String)},
     * {@code extractMinLength} and {@code extractMaxLength}
     * Given Scenario: The default pattern, which starts with lookaheads.
     * Expected Result: the printable ASCII class and the bounds 15 and 64 are read from it.
     */
    @Test
    public void test_defaultShape_isRead() {
        assertEquals("[ -~]", PasswordGenerator.Builder.extractCharClass(defaultPattern));
        assertEquals(15, PasswordGenerator.Builder.extractMinLength(defaultPattern));
        assertEquals(64, PasswordGenerator.Builder.extractMaxLength(defaultPattern));
    }

    /**
     * Method to test: {@link PasswordGenerator.Builder#extractCharClass(String)}
     * Given Scenario: The three shapes the extractor already read, and two patterns it cannot read.
     * Expected Result: the old shapes give the same class as before; a pattern with no leading
     * anchor and class, or without a quantifier, gives {@code null}.
     */
    @Test
    public void test_oldShapesStillRead_unreadableShapesStillNull() {
        assertEquals("[A-Za-z0-9]", PasswordGenerator.Builder.extractCharClass("/^[A-Za-z0-9]{8}$/"));
        assertEquals("[A-Za-z0-9]", PasswordGenerator.Builder.extractCharClass("/^[A-Za-z0-9]{8,}$/"));
        assertEquals("[A-Za-z0-9]", PasswordGenerator.Builder.extractCharClass("/^[A-Za-z0-9]{8,20}$/"));
        assertNull(PasswordGenerator.Builder.extractCharClass("/((?=.*\\d)(?=.*[a-z]).{6,})/"));
        assertNull(PasswordGenerator.Builder.extractCharClass("/^(?=.*[a-z])[a-z0-9]+$/"));
        assertNull(PasswordGenerator.Builder.extractCharClass(null));
    }

    /**
     * Method to test: {@link PasswordGenerator#nextPassword()} built with the default pattern.
     * Given Scenario: 1000 generated passwords.
     * Expected Result: each is 16 characters, passes the default pattern, and contains no space.
     */
    @Test
    public void test_defaultShape_generatesCompliantPasswordsWithoutSpaces() {
        final PasswordGenerator generator = new PasswordGenerator.Builder()
                .withRegExpToolkitValues(TOOLKIT, defaultPattern).build();
        for (int i = 0; i < 1000; i++) {
            final String password = generator.nextPassword();
            assertEquals(16, password.length());
            assertTrue("must pass the default pattern: " + password, validator.validate(password));
            assertFalse("must not contain a space: " + password, password.contains(" "));
        }
    }

    /**
     * Method to test: {@link PasswordGenerator.Builder#buildJsCharPattern(String, String)} and
     * {@code buildJsGroupsJson}
     * Given Scenario: The default pattern.
     * Expected Result: the per-character filter is the printable ASCII class, and the four groups
     * handed to the JavaScript generator contain no space.
     */
    @Test
    public void test_defaultShape_jsHelpers() {
        assertEquals("/[ -~]/", PasswordGenerator.Builder.buildJsCharPattern(TOOLKIT, defaultPattern));
        final String groups = PasswordGenerator.Builder.buildJsGroupsJson(TOOLKIT, defaultPattern);
        assertEquals("four groups", 4, groups.split("\",\"").length);
        assertFalse("no group may contain a space: " + groups, groups.contains(" "));
    }

    /**
     * Method to test: {@link PasswordGenerator.Builder#withRegExpToolkitValues(String, String)}
     * Given Scenario: An override with lookaheads, a class of lower case letters and digits, and
     * bounds 10 to 12.
     * Expected Result: generated passwords are 12 characters long and use only that class.
     */
    @Test
    public void test_overrideWithLookaheads_followsItsClassAndBounds() {
        final String override = "/^(?=.*[a-z])(?=.*[0-9])[a-z0-9]{10,12}$/";
        final PasswordGenerator generator = new PasswordGenerator.Builder()
                .withRegExpToolkitValues(TOOLKIT, override).build();
        final Pattern allowed = Pattern.compile("^[a-z0-9]{12}$");
        for (int i = 0; i < 200; i++) {
            final String password = generator.nextPassword();
            assertTrue("must stay inside the override: " + password, allowed.matcher(password).matches());
        }
    }

    /**
     * Method to test: {@link PasswordGenerator.Builder#withRegExpToolkitValues(String, String)}
     * Given Scenario: An override whose shape cannot be read (no leading anchor).
     * Expected Result: the generator falls back to its built-in alphabet and 16 characters.
     */
    @Test
    public void test_unreadableOverride_fallsBackToBuiltInDefaults() {
        final PasswordGenerator generator = new PasswordGenerator.Builder()
                .withRegExpToolkitValues(TOOLKIT, "/(?=.*[a-z]).{20,}/").build();
        assertEquals(16, generator.nextPassword().length());
    }
}

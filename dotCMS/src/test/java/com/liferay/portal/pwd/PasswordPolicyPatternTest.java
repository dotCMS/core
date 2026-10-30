package com.liferay.portal.pwd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Checks the default {@code passwords.regexptoolkit.pattern} shipped in {@code portal.properties}.
 * The pattern is read from the file itself, so the escaping the file needs is exercised too, and the
 * test cannot drift from what the product ships.
 */
public class PasswordPolicyPatternTest extends UnitTestBase {

    private static RegExpToolkit toolkit;

    /** 4 characters that cover the four classes, so the filler decides the length. */
    private static final String ALL_CLASSES = "Aa1!";

    @BeforeClass
    public static void loadDefaultPattern() throws IOException {
        final Properties properties = new Properties();
        try (InputStream stream = PasswordPolicyPatternTest.class.getResourceAsStream("/portal.properties")) {
            assertNotNull("portal.properties must be on the classpath", stream);
            properties.load(stream);
        }
        final String pattern = properties.getProperty("passwords.regexptoolkit.pattern");
        assertNotNull("the default pattern must be defined", pattern);
        toolkit = new RegExpToolkit(pattern);
    }

    private static String withLength(final int length) {
        final StringBuilder password = new StringBuilder(ALL_CLASSES);
        while (password.length() < length) {
            password.append('x');
        }
        return password.toString();
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: Passwords with all four classes whose length is at the edges of the range.
     * Expected Result: 15 and 64 characters are accepted; 14 and 65 are rejected.
     */
    @Test
    public void test_validate_lengthBounds() {
        assertFalse(toolkit.validate(withLength(14)));
        assertTrue(toolkit.validate(withLength(15)));
        assertTrue(toolkit.validate(withLength(64)));
        assertFalse(toolkit.validate(withLength(65)));
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: A 15-character password missing one of the four classes, one at a time.
     * Expected Result: every one of them is rejected.
     */
    @Test
    public void test_validate_missingClass_rejected() {
        assertFalse("no uppercase", toolkit.validate("aa1!" + "xxxxxxxxxxx"));
        assertFalse("no lowercase", toolkit.validate("AA1!" + "XXXXXXXXXXX"));
        assertFalse("no digit", toolkit.validate("Aa!!" + "xxxxxxxxxxx"));
        assertFalse("no special", toolkit.validate("Aa11" + "xxxxxxxxxxx"));
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: A space is the only character that is neither a letter nor a digit.
     * Expected Result: rejected, because a space does not count as a special character.
     */
    @Test
    public void test_validate_spaceIsNotSpecial() {
        assertFalse(toolkit.validate("Aa1 " + "xxxxxxxxxxx"));
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: Passwords with an inner, a leading and a trailing space that meet the rest.
     * Expected Result: all accepted; the space counts toward the length.
     */
    @Test
    public void test_validate_spacesAllowed() {
        assertTrue("inner", toolkit.validate("Aa1! xxxxxxxxxx"));
        assertTrue("leading", toolkit.validate(" Aa1!xxxxxxxxxx"));
        assertTrue("trailing", toolkit.validate("Aa1!xxxxxxxxxx "));
        assertFalse("14 with a space is still too short", toolkit.validate("Aa1! xxxxxxxxx"));
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: A valid password followed by a line feed or a carriage return, and a line feed
     * or a tab inside an otherwise valid one.
     * Expected Result: all rejected. The engine's end-of-input anchor also matches before a trailing
     * line feed, so the pattern has to guard it explicitly.
     */
    @Test
    public void test_validate_controlCharacters_rejected() {
        final String valid = withLength(15);
        assertTrue(toolkit.validate(valid));
        assertFalse("trailing line feed", toolkit.validate(valid + "\n"));
        assertFalse("trailing carriage return", toolkit.validate(valid + "\r"));
        assertFalse("line feed inside", toolkit.validate("Aa1!xxxxx\nxxxxxx"));
        assertFalse("tab inside", toolkit.validate("Aa1!xxxxx\txxxxxx"));
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: A 15-character password with a character outside printable ASCII.
     * Expected Result: rejected.
     */
    @Test
    public void test_validate_nonAscii_rejected() {
        assertFalse(toolkit.validate("Aa1!xxxxxxxxxxé"));
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: Passwords whose only special character is a symbol the previous default did
     * not list, or one that is easy to lose in escaping ({@code _}, {@code \}, {@code "}).
     * Expected Result: all accepted, because a special character is any printable ASCII character
     * that is not a letter, a digit or a space.
     */
    @Test
    public void test_validate_anyPrintableSymbolCountsAsSpecial() {
        assertTrue(toolkit.validate("Aa1_xxxxxxxxxxx"));
        assertTrue(toolkit.validate("Aa1\\xxxxxxxxxxx"));
        assertTrue(toolkit.validate("Aa1\"xxxxxxxxxxx"));
        assertTrue(toolkit.validate("Aa1/xxxxxxxxxxx"));
    }

    /**
     * Method to test: {@link RegExpToolkit#validate(String)}
     * Given Scenario: {@code null}, an empty string and a very long input.
     * Expected Result: rejected, without a noticeable delay.
     */
    @Test
    public void test_validate_nullEmptyAndVeryLong_rejected() {
        assertFalse(toolkit.validate(null));
        assertFalse(toolkit.validate(""));
        final StringBuilder longInput = new StringBuilder(ALL_CLASSES);
        for (int i = 0; i < 500_000; i++) {
            longInput.append('x');
        }
        final long start = System.nanoTime();
        assertFalse(toolkit.validate(longInput.toString()));
        assertTrue("a very long input must be rejected quickly",
                (System.nanoTime() - start) / 1_000_000 < 2_000);
    }
}

package com.dotcms.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.dotcms.UnitTestBase;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.junit.Test;

/**
 * Covers {@link PasswordRejectionReason}: the classification of a rejected password for the
 * rejection log entry, and the clean-up of values before they are logged.
 */
public class PasswordRejectionReasonTest extends UnitTestBase {

    private static final String DEFAULT = PasswordRejectionReason.DEFAULT_PATTERN;

    private static String withLength(final int length) {
        final StringBuilder password = new StringBuilder("Aa1!");
        while (password.length() < length) {
            password.append('x');
        }
        return password.toString();
    }

    /**
     * Method to test: {@link PasswordRejectionReason#DEFAULT_PATTERN}
     * Given Scenario: The constant the classifier compares the configured pattern against.
     * Expected Result: it is the pattern shipped in {@code portal.properties}, so the two cannot
     * drift apart unnoticed.
     */
    @Test
    public void test_defaultPattern_matchesPortalProperties() throws IOException {
        final Properties properties = new Properties();
        try (InputStream stream = PasswordRejectionReasonTest.class.getResourceAsStream("/portal.properties")) {
            assertNotNull(stream);
            properties.load(stream);
        }
        assertEquals(properties.getProperty("passwords.regexptoolkit.pattern"), DEFAULT);
    }

    /**
     * Method to test: {@link PasswordRejectionReason#classify(String, String)}
     * Given Scenario: Rejected passwords under the default pattern, one per reason.
     * Expected Result: too short, too long, a character outside printable ASCII, and a missing
     * class are told apart.
     */
    @Test
    public void test_classify_defaultPattern() {
        assertEquals(PasswordRejectionReason.TOO_SHORT, PasswordRejectionReason.classify(withLength(14), DEFAULT));
        assertEquals(PasswordRejectionReason.TOO_SHORT, PasswordRejectionReason.classify("", DEFAULT));
        assertEquals(PasswordRejectionReason.TOO_SHORT, PasswordRejectionReason.classify(null, DEFAULT));
        assertEquals(PasswordRejectionReason.TOO_LONG, PasswordRejectionReason.classify(withLength(65), DEFAULT));
        assertEquals(PasswordRejectionReason.DISALLOWED_CHARACTER,
                PasswordRejectionReason.classify(withLength(15) + "\n", DEFAULT));
        assertEquals(PasswordRejectionReason.DISALLOWED_CHARACTER,
                PasswordRejectionReason.classify("Aa1!xxxxx\txxxxxx", DEFAULT));
        assertEquals(PasswordRejectionReason.DISALLOWED_CHARACTER,
                PasswordRejectionReason.classify("Aa1!xxxxxxxxxxé", DEFAULT));
        assertEquals(PasswordRejectionReason.MISSING_CLASS,
                PasswordRejectionReason.classify("aa1!xxxxxxxxxxx", DEFAULT));
        assertEquals(PasswordRejectionReason.MISSING_CLASS,
                PasswordRejectionReason.classify("AA1!XXXXXXXXXXX", DEFAULT));
        assertEquals(PasswordRejectionReason.MISSING_CLASS,
                PasswordRejectionReason.classify("Aa!!xxxxxxxxxxx", DEFAULT));
        assertEquals(PasswordRejectionReason.MISSING_CLASS,
                PasswordRejectionReason.classify("Aa11xxxxxxxxxxx", DEFAULT));
        assertEquals("a space does not count as special",
                PasswordRejectionReason.MISSING_CLASS, PasswordRejectionReason.classify("Aa1 xxxxxxxxxxx", DEFAULT));
    }

    /**
     * Method to test: {@link PasswordRejectionReason#classify(String, String)}
     * Given Scenario: A rejected password under a pattern that is not the default, and a password
     * the default accepts.
     * Expected Result: {@code NOT_MET}, because the rules the classifier knows would not describe
     * the customer's pattern, and nothing about the password is inspected.
     */
    @Test
    public void test_classify_otherPattern_orAcceptedPassword() {
        assertEquals(PasswordRejectionReason.NOT_MET,
                PasswordRejectionReason.classify(withLength(14), "/^[a-z]{20,}$/"));
        assertEquals(PasswordRejectionReason.NOT_MET, PasswordRejectionReason.classify(null, null));
        assertEquals(PasswordRejectionReason.NOT_MET, PasswordRejectionReason.classify(withLength(20), DEFAULT));
    }

    /**
     * Method to test: {@link PasswordRejectionReason#sanitizeForLog(String)}
     * Given Scenario: Values with carriage returns, line feeds and other control characters, and a
     * {@code null}.
     * Expected Result: the control characters are removed, ordinary text is kept, {@code null}
     * becomes an empty string.
     */
    @Test
    public void test_sanitizeForLog_removesControlCharacters() {
        assertEquals("ab", PasswordRejectionReason.sanitizeForLog("a\r\nb"));
        assertEquals("x", PasswordRejectionReason.sanitizeForLog("\u0000x\u007f"));
        assertEquals("GET /api/v1/users", PasswordRejectionReason.sanitizeForLog("GET /api/v1/users"));
        assertEquals("", PasswordRejectionReason.sanitizeForLog(null));
    }
}

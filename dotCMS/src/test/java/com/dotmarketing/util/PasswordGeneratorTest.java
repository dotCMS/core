package com.dotmarketing.util;

import static com.dotmarketing.util.PasswordGenerator.Builder.LOWER_CASE_LETTERS_CHARS;
import static com.dotmarketing.util.PasswordGenerator.Builder.NUMBER_CHARS;
import static com.dotmarketing.util.PasswordGenerator.Builder.SPECIAL_CHARS;
import static com.dotmarketing.util.PasswordGenerator.Builder.UPPER_CASE_LETTERS_CHARS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.junit.Test;

public class PasswordGeneratorTest {

    static final List<String> ACCEPTABLE_PASSWORD_CHARS =
            ImmutableList.of(
                    SPECIAL_CHARS, UPPER_CASE_LETTERS_CHARS, LOWER_CASE_LETTERS_CHARS, NUMBER_CHARS
            );

    // Default portal.properties pattern (passwords.regexptoolkit.pattern) after the ? fix
    static final String DEFAULT_REGEXP_PATTERN =
            "/^[A-Za-z0-9!@#$%^&*()_\\-=+.,';:`~<>\\[\\]?]{8,}$/";

    /**
     * Simply test we're getting back chars that match our charsets with the required minimum number of chars
     * Method to test: {@link PasswordGenerator#nextPassword()}
     */
    @Test
    public void Test_Generate_Password_Then_Test_We_Have_At_Least_One_Match_Per_Group() {
        final PasswordGenerator passwordGenerator = new PasswordGenerator.Builder()
                .withDefaultValues().build();

        final Set<String> generatedPasswords = new HashSet<>(100);

        for (final String acceptablePasswordChar : ACCEPTABLE_PASSWORD_CHARS) {
            final String acceptableCharsPattern = "[" + String.join("", acceptablePasswordChar) + "]";
            final Pattern pattern = Pattern.compile(acceptableCharsPattern);
            //Lets do this a few times just to be sure
            for (int i = 0; i <= 100; i++) {
                final String password = passwordGenerator.nextPassword();
                assertTrue(pattern.matcher(password).find());
                assertEquals(password.length(), 16);
                assertTrue(generatedPasswords.add(password));
            }
        }
    }

    /**
     * extractMinLength should return the {n,} minimum from the pattern, or 0 when absent.
     */
    @Test
    public void Test_ExtractMinLength_Returns_Correct_Value() {
        assertEquals(8,  PasswordGenerator.Builder.extractMinLength(DEFAULT_REGEXP_PATTERN));
        assertEquals(6,  PasswordGenerator.Builder.extractMinLength("/((?=.*\\d).{6,})/"));
        assertEquals(10, PasswordGenerator.Builder.extractMinLength("/^[A-Z]{10,20}$/"));
        assertEquals(0,  PasswordGenerator.Builder.extractMinLength(null));
        assertEquals(0,  PasswordGenerator.Builder.extractMinLength(""));
        assertEquals(0,  PasswordGenerator.Builder.extractMinLength("/^[A-Z]+$/"));
    }

    /**
     * extractCharset should return null for null, empty, or complex lookahead patterns.
     */
    @Test
    public void Test_ExtractCharset_Returns_Null_For_Invalid_Or_Complex_Patterns() {
        assertNull(PasswordGenerator.Builder.extractCharset(null));
        assertNull(PasswordGenerator.Builder.extractCharset(""));
        // Complex lookahead pattern — no extractable character class
        assertNull(PasswordGenerator.Builder.extractCharset(
                "/((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{6,})/"));
    }

    /**
     * extractCharset should return a non-empty string of allowed characters for the default pattern.
     * Every character in the result must match the extracted character class.
     */
    @Test
    public void Test_ExtractCharset_Returns_Allowed_Chars_For_Default_Pattern() {
        final String allowedChars = PasswordGenerator.Builder.extractCharset(DEFAULT_REGEXP_PATTERN);
        assertNotNull(allowedChars);
        assertTrue("Charset should not be empty", allowedChars.length() > 0);

        // Every char returned must match the backend validation pattern
        final Pattern validator = Pattern.compile(
                "^[A-Za-z0-9!@#$%^&*()_\\-=+.,';:`~<>\\[\\]?]+$");
        assertTrue("All extracted chars must be accepted by the backend pattern",
                validator.matcher(allowedChars).matches());

        // '?' must be present — it was the root cause of issue #34616
        assertTrue("'?' must be in the extracted charset", allowedChars.contains("?"));
    }

    /**
     * A generator built with extractCharset + extractMinLength from the default pattern must
     * produce passwords whose length is at least the pattern minimum and that contain only
     * characters accepted by the backend validation pattern.
     */
    @Test
    public void Test_Generate_Password_With_RegExp_Pattern_Only_Contains_Allowed_Chars() {
        final String allowedChars = PasswordGenerator.Builder.extractCharset(DEFAULT_REGEXP_PATTERN);
        assertNotNull(allowedChars);

        final int minLength = PasswordGenerator.Builder.extractMinLength(DEFAULT_REGEXP_PATTERN);
        assertEquals("Default pattern minimum length should be 8", 8, minLength);

        // Simulate withRegExpToolkitValues(): honour the minimum length from the pattern
        final int effectiveLength = Math.max(16, minLength);
        final PasswordGenerator generator = new PasswordGenerator.Builder()
                .charset(allowedChars, 0).build();

        final Pattern validator = Pattern.compile(
                "^[A-Za-z0-9!@#$%^&*()_\\-=+.,';:`~<>\\[\\]?]{8,}$");

        for (int i = 0; i < 100; i++) {
            final String password = generator.nextPassword();
            assertEquals(effectiveLength, password.length());
            assertTrue("Password must match the backend pattern: " + password,
                    validator.matcher(password).matches());
        }
    }

}

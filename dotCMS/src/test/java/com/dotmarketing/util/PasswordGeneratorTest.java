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
     * extractCharClass should return the raw [CharClass] token from the default pattern.
     */
    @Test
    public void Test_ExtractCharClass_Returns_Char_Class_For_Default_Pattern() {
        final String charClass = PasswordGenerator.Builder.extractCharClass(DEFAULT_REGEXP_PATTERN);
        assertNotNull(charClass);
        assertTrue("Char class must start with '['", charClass.startsWith("["));
        assertTrue("Char class must end with ']'", charClass.endsWith("]"));
        // '?' must survive extraction — it was the root cause of issue #34616
        assertTrue("'?' must be in the char class", charClass.contains("?"));
    }

    /**
     * extractCharClass should return null for null, empty, or complex lookahead patterns.
     */
    @Test
    public void Test_ExtractCharClass_Returns_Null_For_Invalid_Or_Complex_Patterns() {
        assertNull(PasswordGenerator.Builder.extractCharClass(null));
        assertNull(PasswordGenerator.Builder.extractCharClass(""));
        assertNull(PasswordGenerator.Builder.extractCharClass(
                "/((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{6,})/"));
    }

    /**
     * withFilteredValues() must produce passwords that:
     * - contain at least one character from each default group (special, upper, lower, digit)
     * - contain ONLY characters from the allowed set
     * - are 16 characters long (default length)
     */
    @Test
    public void Test_WithFilteredValues_Produces_Strong_Passwords_Within_Allowed_Set() {
        final String allowedChars = PasswordGenerator.Builder.extractCharset(DEFAULT_REGEXP_PATTERN);
        assertNotNull(allowedChars);

        final PasswordGenerator generator = new PasswordGenerator.Builder()
                .withFilteredValues(allowedChars).build();

        // Backend validator: only chars from the default pattern are allowed
        final Pattern validator = Pattern.compile(
                "^[A-Za-z0-9!@#$%^&*()_\\-=+.,';:`~<>\\[\\]?]{8,}$");

        // Per-group patterns — filtered defaults must still land at least one char per group
        final Pattern hasSpecial   = Pattern.compile("[" + SPECIAL_CHARS + "]");
        final Pattern hasUpper     = Pattern.compile("[" + UPPER_CASE_LETTERS_CHARS + "]");
        final Pattern hasLower     = Pattern.compile("[" + LOWER_CASE_LETTERS_CHARS + "]");
        final Pattern hasDigit     = Pattern.compile("[" + NUMBER_CHARS + "]");

        for (int i = 0; i < 100; i++) {
            final String password = generator.nextPassword();
            assertEquals("Password must be 16 chars", 16, password.length());
            assertTrue("Password must match backend pattern: " + password,
                    validator.matcher(password).matches());
            assertTrue("Password must contain at least one special char", hasSpecial.matcher(password).find());
            assertTrue("Password must contain at least one uppercase letter", hasUpper.matcher(password).find());
            assertTrue("Password must contain at least one lowercase letter", hasLower.matcher(password).find());
            assertTrue("Password must contain at least one digit", hasDigit.matcher(password).find());
        }
    }

    /**
     * withFilteredValues() must remove characters not permitted by the allowed set.
     * When the allowed set excludes all of a group's chars the group is simply omitted
     * rather than causing an error.
     */
    @Test
    public void Test_WithFilteredValues_Removes_Disallowed_Chars() {
        // Allow only lowercase letters — special chars, uppercase and digits are excluded
        final String onlyLower = LOWER_CASE_LETTERS_CHARS;

        final PasswordGenerator generator = new PasswordGenerator.Builder()
                .withFilteredValues(onlyLower).build();

        final Pattern onlyLowerPattern = Pattern.compile("^[a-z]+$");
        for (int i = 0; i < 50; i++) {
            final String password = generator.nextPassword();
            assertTrue("Password must contain only lowercase letters: " + password,
                    onlyLowerPattern.matcher(password).matches());
        }
    }

}

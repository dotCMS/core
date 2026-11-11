package com.liferay.portal.pwd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.Config;
import com.liferay.portal.util.PropsUtil;
import org.junit.Test;

/**
 * Checks that {@link RegExpToolkit#generate()} produces passwords the active pattern accepts.
 */
public class RegExpToolkitGenerateTest extends UnitTestBase {

    /**
     * Method to test: {@link RegExpToolkit#generate()}
     * Given Scenario: 1000 generated passwords, validated by the toolkit built from the active
     * pattern.
     * Expected Result: every one is accepted and none contains a space.
     */
    @Test
    public void test_generate_satisfiesTheActivePattern() {
        final RegExpToolkit toolkit = new RegExpToolkit();
        for (int i = 0; i < 1000; i++) {
            final String password = toolkit.generate();
            assertTrue("must pass the active pattern: " + password, toolkit.validate(password));
            assertFalse("must not contain a space: " + password, password.contains(" "));
        }
    }

    /**
     * Method to test: {@link RegExpToolkit#generate()}
     * Given Scenario: The pattern is overridden with a fixed length of 6, shorter than the generator
     * can fill with its per-group minimums.
     * Expected Result: a password is still returned, as it was before the generator was used here,
     * instead of an exception.
     */
    @Test
    public void test_generate_shortFixedLengthOverride_stillReturnsAPassword() {
        final String key = "passwords.regexptoolkit.pattern";
        final String original = PropsUtil.get(key);
        try {
            Config.setProperty(key, "/^[A-Za-z0-9!@#]{6}$/");

            assertNotNull(new RegExpToolkit().generate());
        } finally {
            Config.setProperty(key, original);
        }
    }

    /**
     * Method to test: {@link RegExpToolkit#generate()}
     * Given Scenario: A readable pattern whose lookahead demands a punctuation character from {@code !} to
     * {@code /}, which the generator
     * does not interpret, so about a quarter of single attempts lack it.
     * Expected Result: every generated password is accepted by the pattern.
     */
    @Test
    public void test_generate_lookaheadTheGeneratorDoesNotInterpret_stillValid() {
        final String key = "passwords.regexptoolkit.pattern";
        final String original = PropsUtil.get(key);
        try {
            final String pattern = "/^(?=.*[!-/])[ -~]{15,64}$/";
            Config.setProperty(key, pattern);
            final RegExpToolkit toolkit = new RegExpToolkit(pattern);

            for (int i = 0; i < 500; i++) {
                final String password = toolkit.generate();
                assertTrue("must pass the configured pattern: " + password, toolkit.validate(password));
            }
        } finally {
            Config.setProperty(key, original);
        }
    }

    /**
     * Method to test: {@link RegExpToolkit#generate()}
     * Given Scenario: A pattern the generator cannot fill, so the default generator is used.
     * Expected Result: the fallback password is at least 15 characters long.
     */
    @Test
    public void test_generate_fallbackPassword_meetsTheDefaultMinimumLength() {
        final String key = "passwords.regexptoolkit.pattern";
        final String original = PropsUtil.get(key);
        try {
            Config.setProperty(key, "/^[A-Za-z0-9!@#]{6}$/");

            assertTrue(new RegExpToolkit().generate().length() >= 15);
        } finally {
            Config.setProperty(key, original);
        }
    }
}

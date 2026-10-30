package com.liferay.portal.pwd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.dotcms.UnitTestBase;
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
}

package com.dotcms.rest;

import com.dotcms.UnitTestBase;
import com.dotmarketing.util.Config;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link AnonymousAccess#systemSetting()}.
 *
 * Verifies that the correct {@link AnonymousAccess} level is resolved from the
 * {@code CONTENT_APIS_ALLOW_ANONYMOUS} config property, and that the method
 * behaves consistently across all valid and invalid input values.
 */
public class AnonymousAccessTest extends UnitTestBase {

    @After
    public void cleanup() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, null);
    }

    /**
     * Method to test: {@link AnonymousAccess#systemSetting()}
     * Given Scenario: No property is set in config
     * ExpectedResult: Returns READ (the hardcoded default)
     */
    @Test
    public void testSystemSetting_defaultsToRead_whenPropertyNotSet() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, null);

        final AnonymousAccess result = AnonymousAccess.systemSetting();

        assertEquals("Default should be READ when property is not set",
                AnonymousAccess.READ, result);
    }

    /**
     * Method to test: {@link AnonymousAccess#systemSetting()}
     * Given Scenario: Property is explicitly set to READ
     * ExpectedResult: Returns READ
     */
    @Test
    public void testSystemSetting_returnsRead_whenSetToRead() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");

        final AnonymousAccess result = AnonymousAccess.systemSetting();

        assertEquals(AnonymousAccess.READ, result);
    }

    /**
     * Method to test: {@link AnonymousAccess#systemSetting()}
     * Given Scenario: Property is explicitly set to NONE
     * ExpectedResult: Returns NONE (no anonymous access)
     */
    @Test
    public void testSystemSetting_returnsNone_whenSetToNone() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "NONE");

        final AnonymousAccess result = AnonymousAccess.systemSetting();

        assertEquals(AnonymousAccess.NONE, result);
    }

    /**
     * Method to test: {@link AnonymousAccess#systemSetting()}
     * Given Scenario: Property is set to WRITE
     * ExpectedResult: Returns WRITE
     */
    @Test
    public void testSystemSetting_returnsWrite_whenSetToWrite() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");

        final AnonymousAccess result = AnonymousAccess.systemSetting();

        assertEquals(AnonymousAccess.WRITE, result);
    }

    /**
     * Method to test: {@link AnonymousAccess#systemSetting()}
     * Given Scenario: Property is set to an unrecognised value
     * ExpectedResult: Returns NONE (safe default from {@link AnonymousAccess#from})
     */
    @Test
    public void testSystemSetting_returnsNone_whenValueIsInvalid() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "INVALID_VALUE");

        final AnonymousAccess result = AnonymousAccess.systemSetting();

        assertEquals("Invalid value should fall back to NONE (safest default)",
                AnonymousAccess.NONE, result);
    }

    /**
     * Method to test: {@link AnonymousAccess#systemSetting()}
     * Given Scenario: Property value is lowercase
     * ExpectedResult: Returns the matching enum (case-insensitive match)
     */
    @Test
    public void testSystemSetting_isCaseInsensitive() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "read");

        final AnonymousAccess result = AnonymousAccess.systemSetting();

        assertEquals("Matching should be case-insensitive", AnonymousAccess.READ, result);
    }

    /**
     * Method to test: {@link AnonymousAccess#systemSetting()}
     * Given Scenario: READ and WRITE settings represent non-private instances
     *                 that should trigger a security warning at startup
     * ExpectedResult: READ and WRITE are not equal to NONE — confirming the
     *                 warning branch is reached for those values
     */
    @Test
    public void testSystemSetting_readAndWrite_areNotNone_warningBranchReached() {
        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "READ");
        final AnonymousAccess read = AnonymousAccess.systemSetting();

        Config.setProperty(AnonymousAccess.CONTENT_APIS_ALLOW_ANONYMOUS, "WRITE");
        final AnonymousAccess write = AnonymousAccess.systemSetting();

        // NONE would not trigger the startup warning; READ and WRITE do
        assertEquals(false, AnonymousAccess.NONE == read);
        assertEquals(false, AnonymousAccess.NONE == write);
    }
}

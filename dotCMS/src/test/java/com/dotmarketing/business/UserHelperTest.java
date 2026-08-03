package com.dotmarketing.business;

import com.dotmarketing.exception.DotDataException;
import com.liferay.util.SystemProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Unit tests for {@link UserHelper#validateName(String, String)} — the stored-XSS
 * hardening added on the user save path (private-issues#651).
 *
 * These lock in two behaviors that automated review flagged:
 *   - null first/last names must NOT throw (the {@code UtilMethods.isSet} guard is null-safe), and
 *   - names carrying HTML metacharacters ({@code <}, {@code >}) must be rejected.
 */
public class UserHelperTest {

    private static String previousPattern;

    @BeforeClass
    public static void setUp() {
        // The pattern's shipped default lives in system.properties (read via SystemProperties,
        // NOT Config). Set it explicitly so the test is deterministic regardless of what the
        // unit-test classpath loaded, mirroring the shipped value.
        previousPattern = SystemProperties.get("UserName.regexp.pattern");
        SystemProperties.set("UserName.regexp.pattern", "^(?!.*[>|<|\\t|\\n|\\r|\\f].*)");
    }

    @AfterClass
    public static void tearDown() {
        if (previousPattern != null) {
            SystemProperties.set("UserName.regexp.pattern", previousPattern);
        }
    }

    @Test
    public void validateName_allowsCleanNames() throws DotDataException {
        UserHelper.validateName("Ada", "Lovelace");
    }

    @Test(expected = DotDataException.class)
    public void validateName_rejectsAngleBracketsInFirstName() throws DotDataException {
        UserHelper.validateName("<script>alert(1)</script>", "Doe");
    }

    @Test(expected = DotDataException.class)
    public void validateName_rejectsAngleBracketsInLastName() throws DotDataException {
        UserHelper.validateName("John", "Doe<img src=x onerror=alert(1)>");
    }

    /**
     * The save path passes {@code userToSave.getFirstName()}/{@code getLastName()} straight through,
     * and either may be null. The {@code isSet} guard short-circuits before the regex, so this must
     * complete without a NullPointerException.
     */
    @Test
    public void validateName_nullNamesDoNotThrow() throws DotDataException {
        UserHelper.validateName(null, null);
    }
}

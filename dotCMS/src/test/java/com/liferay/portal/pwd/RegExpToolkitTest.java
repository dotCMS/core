package com.liferay.portal.pwd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.dotcms.UnitTestBase;

public class RegExpToolkitTest extends UnitTestBase {

    RegExpToolkit toolkit = new RegExpToolkit("/((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{6,})/");

    // Default pattern from portal.properties (passwords.regexptoolkit.pattern)
    RegExpToolkit defaultToolkit = new RegExpToolkit(
            "/^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9 ])(?!.*\\n)[ -~]{15,64}$/");

    @Test
    public void testInvalidPasswordFormat() {
        assertFalse(toolkit.validate(null));
        assertFalse(toolkit.validate(""));

        // Numbers
        assertFalse(toolkit.validate("123"));
        assertFalse(toolkit.validate("12345"));
        assertFalse(toolkit.validate("123456"));

        // Letters and numbers
        assertFalse(toolkit.validate("12345ETTTTTTTTTT"));
        assertFalse(toolkit.validate("12345wewewersre"));
        assertFalse(toolkit.validate("12345wewewersreREWSW"));

        // Letters only
        assertFalse(toolkit.validate("wewewersreREWSW"));

        // Letters and symbols
        assertFalse(toolkit.validate("we%$@#REWSW"));

        // All valid but invalid length
        assertFalse(toolkit.validate("1T@e"));
    }

    @Test
    public void testValidPasswordFormat() {
        assertTrue(toolkit.validate("123456Eqazxsw%"));
        assertTrue(toolkit.validate("123456Eqazxsw#"));
        assertTrue(toolkit.validate("123456Eqazxsw$"));
        assertTrue(toolkit.validate("123456Eqazxsw@"));
        assertTrue(toolkit.validate("1Test%"));
        assertTrue(toolkit.validate("1T@e$s#t%"));
        assertTrue(toolkit
                .validate("1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%1T@e$s#t%"));
    }

    @Test
    public void testDefaultPatternRejectsInvalidPasswords() {
        // Too short
        assertFalse(defaultToolkit.validate("Abc1234!Abc123"));
        // Missing a class (no special character; a space does not count)
        assertFalse(defaultToolkit.validate("Abc 12345Abc1234"));
        // null
        assertFalse(defaultToolkit.validate(null));
    }

    @Test
    public void testDefaultPatternAcceptsValidPasswords() {
        // Minimum length with all four classes
        assertTrue(defaultToolkit.validate("Abcdefghijkl12!"));
        // With special chars from the allowed set
        assertTrue(defaultToolkit.validate("Abcdefghijk1!@#"));
        // With '?' — previously rejected, now allowed (issue #34616)
        assertTrue(defaultToolkit.validate("Abcdefghijkl12?"));
        assertTrue(defaultToolkit.validate("Ab?defghijkl12#"));
        // Mixed special chars including '?'
        assertTrue(defaultToolkit.validate("P@ssw0rd?P@ssw0rd?"));
    }
}

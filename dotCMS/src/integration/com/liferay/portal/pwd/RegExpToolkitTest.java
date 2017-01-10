package com.liferay.portal.pwd;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import com.dotcms.TestBase;

public class RegExpToolkitTest extends TestBase {

    RegExpToolkit toolkit = new RegExpToolkit("/((?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%]).{6,})/");

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
}

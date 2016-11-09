package com.dotmarketing.util;

import static org.junit.Assert.assertNotNull;

import java.util.Date;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit test for {@link UtilMethods}
 */
public class UtilMethodsTest extends BaseMessageResources {

	@Test
    public void testValidateFileName_Valid() {
		String fileName = "abc.html";

		String result = UtilMethods.validateFileName(fileName);

        assertEquals(fileName, result);
    }

	@Test(expected=IllegalArgumentException.class)
    public void testValidateFileName_Exception() {
		String fileName = "a,bc.html";

		UtilMethods.validateFileName(fileName);
    }


	@Test
    public void testGetValidFileName_Valid() {
		String fileName = "abc.html";

		String result = UtilMethods.getValidFileName(fileName);

        assertEquals(fileName, result);
    }

	@Test
    public void testGetValidFileName_Rewritten() {
		String fileName = "a,bc.html";

		String result = UtilMethods.getValidFileName(fileName);

        assertEquals("a0x2Cbc.html", result);
    }

	@Test(expected=IllegalArgumentException.class)
    public void testGetValidFileName_Invalid() {
		String fileName = null;

		UtilMethods.getValidFileName(fileName);
    }
}

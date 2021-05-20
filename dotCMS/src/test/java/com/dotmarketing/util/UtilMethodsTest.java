package com.dotmarketing.util;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.dotcms.UnitTestBase;
import org.junit.Test;

/**
 * Unit test for {@link UtilMethods}
 */
public class UtilMethodsTest extends UnitTestBase {

	@Test
	public void testIsValidURL_Valid() {

		final String url = "https://demo.dotcms.com";

		assertTrue(UtilMethods.isValidURL(url));
	}

	@Test
	public void testIsValidURL_NULL() {

		final String url = null;

		assertFalse(UtilMethods.isValidURL(url));
	}

	@Test
	public void testIsValidURL_Invalid() {

		final String url = "xxx";

		assertFalse(UtilMethods.isValidURL(url));
	}

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

	@Test
	public void test_CharArrayIsSet_Valid() {
		assertFalse(UtilMethods.isSet("null".toCharArray()));
		assertFalse(UtilMethods.isSet(new char[]{}));
		assertFalse(UtilMethods.isSet((char[]) null));

		assertTrue(UtilMethods.isNotSet("null".toCharArray()));
		assertTrue(UtilMethods.isNotSet(new char[]{}));
		assertTrue(UtilMethods.isNotSet((char[]) null));
		assertTrue(UtilMethods.isNotSet("".toCharArray()));

		assertTrue(UtilMethods.isSet("abcdefghijklmnopqrstuvwxyz".toCharArray()));
		assertTrue(UtilMethods.isSet(new char[]{'1','2','3'}));

		assertFalse(UtilMethods.isNotSet("abcdefghijklmnopqrstuvwxyz".toCharArray()));
		assertFalse(UtilMethods.isNotSet(new char[]{'1','2','3'}));
	}

    /**
     * Test method {@link UtilMethods#trimCharArray(char[])}
     * Given scenario: The method is invoked with a valid char[] array with leading and trailing whitespaces
     * Expected result: The char[] array should be returned without leading and trailing whitespaces
     */
	@Test
	public void Test_TrimCharArray_WithValidValue_ShouldSuccess(){
        assertEquals("my-value",
                String.valueOf(UtilMethods.trimCharArray("   my-value   ".toCharArray())));
    }

    /**
     * Test method {@link UtilMethods#trimCharArray(char[])}
     * Given scenario: The method is invoked with a null char[]
     * Expected result: The method should return null
     */
    @Test
    public void Test_TrimCharArray_WithNullArray_ReturnsNull(){
        assertNull(UtilMethods.trimCharArray(null));
    }

    /**
     * Test method {@link UtilMethods#trimCharArray(char[])}
     * Given scenario: The method is invoked with an empty char[]
     * Expected result: The method should return the empty char[]
     */
    @Test
    public void Test_TrimCharArray_WithEmptyArray_ReturnsEmptyArray(){
        final char[] result = UtilMethods.trimCharArray(new char[]{});
        assertNotNull(result);
        assertEquals(0, result.length);
    }
}

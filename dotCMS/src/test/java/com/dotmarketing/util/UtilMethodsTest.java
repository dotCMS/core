package com.dotmarketing.util;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.dotcms.UnitTestBase;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.liferay.portal.model.User;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

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

	@Test
	public void test_isSet_with_null_supplier(){

		final Contentlet fakeObject=null;
		assertTrue(UtilMethods.isEmpty(()->fakeObject.getMap()));
		assertFalse(UtilMethods.isSet(()->fakeObject.getMap()));

	}



	@Test
	public void test_isSet_with_zero_string_supplier(){

		final String emptyString="";
		assertTrue(UtilMethods.isEmpty(()->emptyString));
		assertFalse(UtilMethods.isSet(()->emptyString));

	}

	@Test
	public void test_isSet_with_real_string_supplier(){

		final String emptyString="Not Empty";
		assertTrue(UtilMethods.isSet(()->emptyString));
		assertFalse(UtilMethods.isEmpty(()->emptyString));

	}

	@Test
	public void test_isSet_with_object_supplier(){

		final Contentlet contentlet=new Contentlet();
		assertTrue(UtilMethods.isSet(()->contentlet.getMap()));
		assertFalse(UtilMethods.isEmpty(()->contentlet.getMap()));

	}
	/**
	 * Test method {@link UtilMethods#exceedsMaxLength(CharSequence, int)}
	 * Given scenario: The method is invoked with a valid string value and maximum length allowed
	 * Expected result: This returns the false when the length of given string value doesn't exceed the maximum value length
	 */
	@Test
	public void testStringWithinMaxLength() {
		assertFalse(UtilMethods.exceedsMaxLength("hello", 10));
	}

	/**
	 * Test method {@link UtilMethods#exceedsMaxLength(CharSequence, int)}
	 * Given scenario: The method is invoked with a valid string value and maximum length allowed
	 * Expected result: This returns the true when the length of given string value doesn't exceed the maximum value length
	 */
	@Test
	public void testStringMaxLength() {
		assertTrue(UtilMethods.exceedsMaxLength("J7uQX9vLsI6MwP8oYgqK4jVt2A0L5jXt2W4hS9bE8pZ7yM3iR1oV6nL3eZ2hK4tD9", 10));
	}


	static String[] goodImageNames = {"default-persona.png", "default.PnG", "testing-avif.avif",
			"here is a tiff.tiff", "here is another tiff.tif", "another-Gif.Gif", "look a jpeg.jpeg",
			"Guess this is a jpg.jpg", "My bigSVG.SvG"};

	static String[] badImageNames = {"default-personapng", "default-PnG", "testing.pdf", "testing..pdf", "testing_pdf", "testing..pdff", "testing-avif-avf",
			"here is a tiff", "another-Gif", "look a jpeg!", "Guess this is a jpg*", "here is a.vtl"};


	@Test
	public void test_isImage_method(){
		for(String imageName:goodImageNames){
			assertTrue(UtilMethods.isImage(imageName));
		}
		for(String imageName:badImageNames){
			assertFalse(UtilMethods.isImage(imageName));
		}
	}

	/**
	 * Scenario: Extracting user ID from a User object
	 * Given a null User object
	 * When the user ID is extracted
	 * Then the result should be null
	 *
	 * Given a mocked User object with no user ID
	 * When the user ID is extracted
	 * Then the result should be null
	 *
	 * Given a mocked User object with a user ID "userId"
	 * When the user ID is extracted
	 * Then the result should be "userId"
	 */
	@Test
	public void test_extractUserIdOrNull(){
		assertNull(UtilMethods.extractUserIdOrNull(null));

		final User user = mock(User.class);
		assertNull(UtilMethods.extractUserIdOrNull(user));

		when(user.getUserId()).thenReturn("userId");
		assertEquals("userId", UtilMethods.extractUserIdOrNull(user));
	}

	final static String[] rasterImagesExtensions = new String[]{"webp", "png", "gif", "jpg"};
	final static String[] vectorImagesExtensions = new String[]{"svg", "eps", "ai", "dxf"};

	/**
	 * Given vector image extensions (SVG or EPS),
	 * When checking if are vector images,
	 * Then the method should return true for all vector extensions.
	 */
	@Test
	public void testIsVectorImageWithVectorExtensions() {
		// Given
		// When & Then
		for (String vectorExtension : vectorImagesExtensions) {
			Assertions.assertTrue(UtilMethods.isVectorImage(vectorExtension),
					"Expected transformation to be skipped for vector extension: " + vectorExtension);
		}
	}

	/**
	 * Given raster image extensions (JPG, PNG, etc.),
	 * When checking if are vector images,
	 * Then the method should return false for all raster extensions.
	 */
	@Test
	public void testIsVectorImageWithRasterExtensions() {
		// Given
		// When & Then
		for (String rasterExtension : rasterImagesExtensions) {
			Assertions.assertFalse(UtilMethods.isVectorImage(rasterExtension),
					"Expected transformation not to be skipped for raster extension: " + rasterExtension);
		}
	}

}

package com.dotmarketing.util;

import static com.dotmarketing.util.UtilMethods.isValidDotCMSPath;
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
import java.util.Base64;
import org.junit.Assert;
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
			"Guess this is a jpg.jpg", "My bigSVG.SvG",
			"http://www.example.com/image.png",
			"https://www.example.com/image.jpg?param=value",
	};

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

	/**
	 * Test method {@link UtilMethods#isLuceneQuery(String)}
	 * Given scenario: Given a string that want to be checked if it is a lucene query
	 * Expected result: Return true if it is valid lucene query, otherwise return false
	 */
	@Test
	public void testIsLuceneQuery(){
		final String luceneQuery = "+title:hello";
		final String invalidLuceneQuery = "badExample";

		assertTrue(UtilMethods.isLuceneQuery(luceneQuery));
		assertFalse(UtilMethods.isLuceneQuery(invalidLuceneQuery));
	}

	/**
	 * Test method {@link UtilMethods#isValidDotCMSPath(String)}
	 * Given scenario: Given a string that want to be checked if it is a valid dotCMS path
	 * Expected result: Return true if it is valid dotCMS path, otherwise return false
	 */
	@Test
	public void testValidPaths() {
		// Basic valid paths
		Assert.assertTrue("Root path should be valid", isValidDotCMSPath("/"));
		Assert.assertTrue("Simple asset path should be valid", isValidDotCMSPath("/asset"));
		Assert.assertTrue("Folder/asset path should be valid", isValidDotCMSPath("/folder/asset"));
		Assert.assertTrue("Nested folder path should be valid", isValidDotCMSPath("/folder/subfolder/asset"));

		// Valid with extensions
		Assert.assertTrue("Image path should be valid", isValidDotCMSPath("/image.jpg"));
		Assert.assertTrue("Document path should be valid", isValidDotCMSPath("/folder/document.pdf"));
		Assert.assertTrue("Video path should be valid", isValidDotCMSPath("/folder/subfolder/video.mp4"));

		// Valid with special characters
		Assert.assertTrue("Dash in filename should be valid", isValidDotCMSPath("/file-name.txt"));
		Assert.assertTrue("Underscore in filename should be valid", isValidDotCMSPath("/file_name.txt"));
		Assert.assertTrue("Multiple dots should be valid", isValidDotCMSPath("/folder/file.name.extension"));
		Assert.assertTrue("Numbers should be valid", isValidDotCMSPath("/folder123/asset456"));

		// Complex valid paths
		Assert.assertTrue("Complex banner path should be valid",
				isValidDotCMSPath("/content/images/banners/banner-2024.jpg"));
		Assert.assertTrue("Legal document path should be valid",
				isValidDotCMSPath("/documents/legal/terms-of-service.pdf"));
		Assert.assertTrue("Video path should be valid",
				isValidDotCMSPath("/media/videos/promotional/intro_video.mp4"));
	}

	/**
	 * Test method {@link UtilMethods#isValidDotCMSPath(String)}
	 * Given scenario: Given a string that want to be checked if it is a valid dotCMS path
	 * Expected result: Return true if it is valid dotCMS path, otherwise return false
	 */
	@Test
	public void testInvalidPaths() {
		// Missing leading slash
		assertFalse("Path without leading slash should be invalid", isValidDotCMSPath("asset"));
		assertFalse("Folder path without leading slash should be invalid", isValidDotCMSPath("folder/asset"));
		assertFalse("Nested path without leading slash should be invalid", isValidDotCMSPath("folder/subfolder/asset"));

		// Trailing slash (except root)
		assertFalse("Asset with trailing slash should be invalid", isValidDotCMSPath("/asset/"));
		assertFalse("Folder with trailing slash should be invalid", isValidDotCMSPath("/folder/"));
		assertFalse("Nested path with trailing slash should be invalid", isValidDotCMSPath("/folder/asset/"));

		// Double slashes
		assertFalse("Double slash at start should be invalid", isValidDotCMSPath("//asset"));
		assertFalse("Double slash in middle should be invalid", isValidDotCMSPath("/folder//asset"));
		assertFalse("Double slash in nested path should be invalid", isValidDotCMSPath("/folder/subfolder//asset"));
		assertFalse("Triple slash should be invalid", isValidDotCMSPath("/folder///asset"));

		// Empty segments
		assertFalse("Only double slashes should be invalid", isValidDotCMSPath("//"));
		assertFalse("Double slash with folder should be invalid", isValidDotCMSPath("/folder//"));
		assertFalse("Triple slash with folder should be invalid", isValidDotCMSPath("///folder"));
	}

	/**
	 * Test method {@link UtilMethods#isValidDotCMSPath(String)}
	 * Given scenario: Given a string that want to be checked if it is a valid dotCMS path
	 * Expected result: Return true if it is valid dotCMS path, otherwise return false
	 */
	@Test
	public void testNullAndEmptyPaths() {
		assertFalse("Null path should be invalid", isValidDotCMSPath(null));
		assertFalse("Empty path should be invalid", isValidDotCMSPath(""));
	}

	/**
	 * Test method {@link UtilMethods#isValidDotCMSPath(String)}
	 * Given scenario: Given a string that want to be checked if it is a valid dotCMS path
	 * Expected result: Return true if it is valid dotCMS path, otherwise return false
	 */
	@Test
	public void testEdgeCases() {
		// Root path is valid
		Assert.assertTrue("Root path should be valid", isValidDotCMSPath("/"));

		// Single character segments
		Assert.assertTrue("Single character asset should be valid", isValidDotCMSPath("/a"));
		Assert.assertTrue("Single character folders should be valid", isValidDotCMSPath("/a/b"));
		Assert.assertTrue("Number segments should be valid", isValidDotCMSPath("/1/2/3"));

		// Very long paths
		String longPath = "/very/long/path/with/many/segments/that/goes/deep/into/folder/structure/file.txt";
		Assert.assertTrue("Long path should be valid", isValidDotCMSPath(longPath));

		// Paths with numbers only
		Assert.assertTrue("Numeric asset should be valid", isValidDotCMSPath("/123"));
		Assert.assertTrue("Mixed numeric path should be valid", isValidDotCMSPath("/folder/456/asset"));

		// Paths with dots and dashes
		Assert.assertTrue("Complex filename should be valid", isValidDotCMSPath("/my-folder/my.file.name.txt"));
		Assert.assertTrue("Underscore and dash should be valid", isValidDotCMSPath("/folder_name/file-name.extension"));
	}

	/**
	 * Test method {@link UtilMethods#isValidDotCMSPath(String)}
	 * Given scenario: Given a string that want to be checked if it is a valid dotCMS path
	 * Expected result: Return true if it is valid dotCMS path, otherwise return false
	 */
	@Test
	public void testInvalidCharacters() {
		// Spaces (based on current regex)
		assertFalse("Space in folder name should be invalid", isValidDotCMSPath("/folder name/asset"));
		assertFalse("Space in asset name should be invalid", isValidDotCMSPath("/folder/asset name"));

		// Special characters not allowed
		assertFalse("@ character should be invalid", isValidDotCMSPath("/folder@name/asset"));
		assertFalse("# character should be invalid", isValidDotCMSPath("/folder/asset#tag"));
		assertFalse("$ character should be invalid", isValidDotCMSPath("/folder$/asset"));
		assertFalse("% character should be invalid", isValidDotCMSPath("/folder%encoded/asset"));
		assertFalse("& character should be invalid", isValidDotCMSPath("/folder&name/asset"));
		assertFalse("* character should be invalid", isValidDotCMSPath("/folder*/asset"));
		assertFalse("+ character should be invalid", isValidDotCMSPath("/folder+plus/asset"));
		assertFalse("= character should be invalid", isValidDotCMSPath("/folder=equals/asset"));
		assertFalse("? character should be invalid", isValidDotCMSPath("/folder?query/asset"));
		assertFalse("[ character should be invalid", isValidDotCMSPath("/folder[bracket]/asset"));
		assertFalse("{ character should be invalid", isValidDotCMSPath("/folder{brace}/asset"));
		assertFalse("| character should be invalid", isValidDotCMSPath("/folder|pipe/asset"));
		assertFalse("\\ character should be invalid", isValidDotCMSPath("/folder\\backslash/asset"));
	}

	/**
	 * Test method {@link UtilMethods#isValidDotCMSPath(String)}
	 * Given scenario: Given a string that want to be checked if it is a valid dotCMS path
	 * Expected result: Return true if it is valid dotCMS path, otherwise return false
	 */
	@Test
	public void testFileExtensions() {
		// Common file extensions should be valid
		Assert.assertTrue("PDF extension should be valid", isValidDotCMSPath("/document.pdf"));
		Assert.assertTrue("JPG extension should be valid", isValidDotCMSPath("/image.jpg"));
		Assert.assertTrue("JPEG extension should be valid", isValidDotCMSPath("/image.jpeg"));
		Assert.assertTrue("PNG extension should be valid", isValidDotCMSPath("/image.png"));
		Assert.assertTrue("GIF extension should be valid", isValidDotCMSPath("/image.gif"));
		Assert.assertTrue("MP4 extension should be valid", isValidDotCMSPath("/video.mp4"));
		Assert.assertTrue("AVI extension should be valid", isValidDotCMSPath("/video.avi"));
		Assert.assertTrue("MP3 extension should be valid", isValidDotCMSPath("/audio.mp3"));
		Assert.assertTrue("ZIP extension should be valid", isValidDotCMSPath("/archive.zip"));
		Assert.assertTrue("XLSX extension should be valid", isValidDotCMSPath("/spreadsheet.xlsx"));
		Assert.assertTrue("PPTX extension should be valid", isValidDotCMSPath("/presentation.pptx"));
		Assert.assertTrue("TXT extension should be valid", isValidDotCMSPath("/text.txt"));
		Assert.assertTrue("HTML extension should be valid", isValidDotCMSPath("/webpage.html"));
		Assert.assertTrue("CSS extension should be valid", isValidDotCMSPath("/style.css"));
		Assert.assertTrue("JS extension should be valid", isValidDotCMSPath("/script.js"));

		// Multiple dots in filename
		Assert.assertTrue("Multiple dots should be valid", isValidDotCMSPath("/file.name.with.dots.txt"));
		Assert.assertTrue("Backup filename should be valid", isValidDotCMSPath("/backup.2024.01.15.zip"));
	}

	/**
	 * Test method {@link UtilMethods#isValidDotCMSPath(String)}
	 * Given scenario: Given a string that want to be checked if it is a valid dotCMS path
	 * Expected result: Return true if it is valid dotCMS path, otherwise return false
	 */
	@Test
	public void testRealWorldExamples() {
		// Typical dotCMS content paths
		Assert.assertTrue("Theme CSS path should be valid",
				isValidDotCMSPath("/application/themes/quest/css/main.css"));
		Assert.assertTrue("Theme image path should be valid",
				isValidDotCMSPath("/application/themes/quest/images/logo.png"));
		Assert.assertTrue("Asset with UUID path should be valid",
				isValidDotCMSPath("/dA/48/dA487c29-14de-4765-a3e9-30628330ac8e/fileAsset/image.jpg"));
		Assert.assertTrue("Content asset path should be valid",
				isValidDotCMSPath("/contentAsset/image/inode/123456"));
		Assert.assertTrue("Legal document path should be valid",
				isValidDotCMSPath("/documents/legal/privacy-policy.pdf"));

		// Asset paths with UUIDs (common in dotCMS)
		Assert.assertTrue("UUID asset path should be valid",
				isValidDotCMSPath("/assets/12345678-1234-1234-1234-123456789012/document.pdf"));

		// Deep nested structures
		Assert.assertTrue("Deep nested path should be valid",
				isValidDotCMSPath("/sites/default/application/themes/travel/images/banners/hero-banner.jpg"));
	}


   /**
    * Method to test: base64Encode
    * Given Scenario: Given a valid string to encode
    * ExpectedResult: The string should be properly base64 encoded
    */
   @Test
   public void test_base64Encode_validString() {
      final String input = "Hello World";
      final String expected = Base64.getEncoder().encodeToString(input.getBytes());

      final String result = UtilHTML.base64Encode(input);

      assertNotNull(result);
      assertEquals(expected, result);
      assertEquals("SGVsbG8gV29ybGQ=", result);
   }

   /**
    * Method to test: base64Encode
    * Given Scenario: Given an empty string to encode
    * ExpectedResult: The empty string should be properly base64 encoded
    */
   @Test
   public void test_base64Encode_emptyString() {
      final String input = "";
      final String expected = Base64.getEncoder().encodeToString(input.getBytes());

      final String result = UtilHTML.base64Encode(input);

      assertNotNull(result);
      assertEquals(expected, result);
      assertEquals("", result);
   }

   /**
    * Method to test: base64Encode
    * Given Scenario: Given a null string to encode
    * ExpectedResult: The method should return null
    */
   @Test
   public void test_base64Encode_nullString() {
      final String result = UtilHTML.base64Encode(null);

      assertNull(result);
   }

   /**
    * Method to test: base64Encode
    * Given Scenario: Given a string with special characters to encode
    * ExpectedResult: The string should be properly base64 encoded
    */
   @Test
   public void test_base64Encode_specialCharacters() {
      final String input = "Test with special chars: !@#$%^&*()_+-=[]{}|;':,.<>?";
      final String expected = Base64.getEncoder().encodeToString(input.getBytes());

      final String result = UtilHTML.base64Encode(input);

      assertNotNull(result);
      assertEquals(expected, result);
   }

   /**
    * Method to test: base64Encode
    * Given Scenario: Given a string with unicode characters to encode
    * ExpectedResult: The string should be properly base64 encoded
    */
   @Test
   public void test_base64Encode_unicodeCharacters() {
      final String input = "Unicode test: 你好世界 🌍 café résumé";
      final String expected = Base64.getEncoder().encodeToString(input.getBytes());

      final String result = UtilHTML.base64Encode(input);

      assertNotNull(result);
      assertEquals(expected, result);
   }

   /**
    * Method to test: base64Decode
    * Given Scenario: Given a valid base64 encoded string to decode
    * ExpectedResult: The string should be properly decoded
    */
   @Test
   public void test_base64Decode_validString() {
      final String input = "SGVsbG8gV29ybGQ=";
      final String expected = "Hello World";

      final String result = UtilHTML.base64Decode(input);

      assertNotNull(result);
      assertEquals(expected, result);
   }

   /**
    * Method to test: base64Decode
    * Given Scenario: Given an empty base64 string to decode
    * ExpectedResult: The empty string should be properly decoded
    */
   @Test
   public void test_base64Decode_emptyString() {
      final String input = "";
      final String expected = "";

      final String result = UtilHTML.base64Decode(input);

      assertNotNull(result);
      assertEquals(expected, result);
   }

   /**
    * Method to test: base64Decode
    * Given Scenario: Given a null string to decode
    * ExpectedResult: The method should return null
    */
   @Test
   public void test_base64Decode_nullString() {
      final String result = UtilHTML.base64Decode(null);

      assertNull(result);
   }

   /**
    * Method to test: base64Decode
    * Given Scenario: Given a base64 string with special characters to decode
    * ExpectedResult: The string should be properly decoded
    */
   @Test
   public void test_base64Decode_specialCharacters() {
      final String originalString = "Test with special chars: !@#$%^&*()_+-=[]{}|;':,.<>?";
      final String encodedString = Base64.getEncoder().encodeToString(originalString.getBytes());

      final String result = UtilHTML.base64Decode(encodedString);

      assertNotNull(result);
      assertEquals(originalString, result);
   }

   /**
    * Method to test: base64Decode
    * Given Scenario: Given a base64 string with unicode characters to decode
    * ExpectedResult: The string should be properly decoded
    */
   @Test
   public void test_base64Decode_unicodeCharacters() {
      final String originalString = "Unicode test: 你好世界 🌍 café résumé";
      final String encodedString = Base64.getEncoder().encodeToString(originalString.getBytes());

      final String result = UtilHTML.base64Decode(encodedString);

      assertNotNull(result);
      assertEquals(originalString, result);
   }

   /**
    * Method to test: base64Encode and base64Decode
    * Given Scenario: Round-trip test - encode then decode various strings
    * ExpectedResult: The decoded string should match the original input
    */
   @Test
   public void test_base64_roundTrip() {
      final String[] testStrings = {
              "Simple text",
              "Text with spaces and numbers 123",
              "Special chars: !@#$%^&*()",
              "Unicode: 你好 🌍 café",
              "Multi-line\ntext\nwith\ntabs\t",
              "JSON-like: {\"key\": \"value\", \"number\": 42}",
              "HTML: <div class=\"test\">Content</div>",
              "Very long string that exceeds typical buffer sizes and contains various characters including numbers 1234567890 and symbols !@#$%^&*()_+-=[]{}|;':,.<>? and unicode characters like 你好世界 🌍 café résumé naïve"
      };

      for (String original : testStrings) {
         final String encoded = UtilHTML.base64Encode(original);
         final String decoded = UtilHTML.base64Decode(encoded);

         assertNotNull("Encoded string should not be null for: " + original, encoded);
         assertNotNull("Decoded string should not be null for: " + original, decoded);
         assertEquals("Round-trip should preserve original string: " + original, original, decoded);
      }
   }

   /**
    * Method to test: base64Decode
    * Given Scenario: Given an invalid base64 string (should throw exception)
    * ExpectedResult: IllegalArgumentException should be thrown
    */
   @Test(expected = IllegalArgumentException.class)
   public void test_base64Decode_invalidBase64String() {
      final String invalidBase64 = "This is not a valid base64 string!";
      UtilHTML.base64Decode(invalidBase64);
   }
}

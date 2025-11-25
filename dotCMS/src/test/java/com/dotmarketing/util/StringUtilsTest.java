package com.dotmarketing.util;

import static com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

@RunWith(DataProviderRunner.class)
public class StringUtilsTest {

    public Map<String, Object> createParams() {

        final Map<String, Object> params = new HashMap<>();

        params.put("sys:user.home", "jsanca");
        params.put("hostId", "1.dotcms.com");
        params.put("hostname", "dotcms.com");
        params.put("languageId", "en");
        params.put("language-Id", "en");
        params.put("language.Id", "en");
        params.put("language:I.d", "en");
        return params;
    }

    @Test
    public void testDoNothingInterpolate() {


        final Map<String, Object> params = createParams();
        final String expression = "nothing";
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals(expression, expected);
    }

    @Test
    public void testCheckNullInterpolate() {
        final Map<String, Object> params = createParams();
        final String expected = StringUtils.interpolate(null, params);

        assertNotNull(expected);
        assertEquals(expected, StringPool.BLANK);
    }

    @Test
    public void testCheckNull2Interpolate() {
        final String expression = "nothing";
        final String expected = StringUtils.interpolate(expression, null);

        assertNotNull(expected);
        assertEquals(expression, expected);
    }

    @Test
    public void testCheckNull3Interpolate() {
        final String expected = StringUtils.interpolate(null, null);

        assertNotNull(expected);
        assertEquals(expected, StringPool.BLANK);
    }

    @Test
    public void testDoInterpolate() {


        final Map<String, Object> params = createParams();
        final String expression = "{hostId}-mycustomname";
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals("1.dotcms.com-mycustomname", expected);
    }

    @Test
    public void testDoInterpolateParamsNull() {
        final String expression = "{hostId}-mycustomname";
        final String expected = StringUtils.interpolate(expression, null);

        assertNotNull(expected);
        assertEquals("{hostId}-mycustomname", expected);
    }

    @Test
    public void testDoMoreThanOneInterpolate() {


        final Map<String, Object> params = createParams();
        final String expression = "xxx{hostId}/{hostname}-mycustomname/{languageId}";
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals("xxx1.dotcms.com/dotcms.com-mycustomname/en", expected);
    }

    @Test
    public void testDoMoreThanOneButWithAMissingParamInterpolate() {


        final Map<String, Object> params = createParams();
        final String expression = "xxx{missingParam}/{hostname}-mycustomname/{languageId}";
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals("xxx{missingParam}/dotcms.com-mycustomname/en", expected);
    }

    @Test
    public void testDoMoreThanOneButWithARepeatedParamInterpolate() {


        final Map<String, Object> params = createParams();
        final String expression = "xxx{languageId}/{hostname}-mycustomname/{languageId}";
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals("xxxen/dotcms.com-mycustomname/en", expected);
    }

    @Test
    public void testDotVar() {


        final Map<String, Object> params = createParams();
        final String expression = "customvar_{language-Id}:{sys:user.home}-hostId:{hostId}--{language.Id}>>:{language:I.d}-";
        System.out.println(expression);
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals("customvar_en:jsanca-hostId:1.dotcms.com--en>>:en-", expected);
    }

    @Test
    public void testIsHtml_WhenInvalidHtml_ShouldBeFalse() {

        assertFalse(StringUtils.isHtml("This is not an html"));
    }

    @Test
    public void testIsHtml_WhenIncompleHtml_ShouldBeFalse() {

        assertFalse(StringUtils.isHtml("<This is not an html"));
    }

    @Test
    public void testIsHtml_WhenIncompleHtml2_ShouldBeFalse() {

        assertFalse(StringUtils.isHtml("This is not an html >"));
    }

    @Test
    public void testIsHtml_WhenValidHtml_ShouldBeTrue() {

        assertTrue(StringUtils.isHtml("<strong>This is an html </strong>"));
    }

    @Test
    public void testIsHtml_WhenValidMultiLineHtml_ShouldBeTrue() {

        assertTrue(StringUtils.isHtml("<html>\n" +
                                                    "<body>\n" +
                                                    "<p>This is a HTML</p>\n" +
                                                    "</body>\n" +
                                                  "<html>\n"
        ));
    }

    @Test
    public void test_Null_Sequence() {
        assertEquals("", StringUtils.builder().toString());
        assertEquals("", StringUtils.builder((CharSequence) null).toString());
        assertEquals("", StringUtils.builder(null, null, null, null).toString());
    }

    @Test
    public void test_Valid_Sequences() {

        assertEquals("hello world 2", StringUtils.builder("hello", " ", "world", " ", "2").toString());
        assertEquals("hello world 2", StringUtils.builder("hello", " ", "world").append(" ").append(2).toString());
        assertEquals("hello world 2", StringUtils.builder("hello", " ", "world", " ", 2).toString());
    }

    @Test
    public void test_Valid_and_Invalid_Sequences() {

        assertEquals("hello world 2", StringUtils.builder("hello", " ", null, null, "world", null, " ", null, "2").toString());
        assertEquals("hello world 2", StringUtils.builder(null, "hello", " ", null, null, "world", null).append(" ").append(2).toString());
        assertEquals("hello world 2", StringUtils.builder("hello", null, " ", null, "world", null, " ", 2).toString());
    }

    /**
     * Test of {@link StringUtils#camelCaseLower(String)}
     */
    @Test
    public void test_camelCaseLower() {
        testCamelCaseLower("MP3 ./,test", "mp3Test");
        testCamelCaseLower("MP3      test", "mp3Test");
        testCamelCaseLower("MP3 test", "mp3Test");
        testCamelCaseLower("-MP3 ./,test", "mp3Test");
        testCamelCaseLower("3MP3 ./,test", "mp3Test");
        testCamelCaseLower("3MP3 -- -- test", "mp3Test");
        testCamelCaseLower("3MP3      test", "mp3Test");
        testCamelCaseLower("3MP3  --  --  --  test", "mp3Test");
        testCamelCaseLower("Simple Test", "simpleTest");
        testCamelCaseLower("-Simple Test", "simpleTest");
        testCamelCaseLower("3Simple Test", "simpleTest");
        testCamelCaseLower("3MP3 ..///..//--- 65test36", "mp365test36");
        testCamelCaseLower("simple --   test", "simpleTest");
        testCamelCaseLower("mp3 ./,test", "mp3Test");
        testCamelCaseLower("3simple test", "simpleTest");
        testCamelCaseLower("-simple test", "simpleTest");
        testCamelCaseLower("-simple Test", "simpleTest");
        testCamelCaseLower("simple Test", "simpleTest");
        testCamelCaseLower("simple       Test", "simpleTest");
        testCamelCaseLower("Simple !@#$%^&*()_-+= Test", "simpleTest");
        testCamelCaseLower("你好吗", "");
        testCamelCaseLower("你好吗3", "");
        testCamelCaseLower("你好吗34", "");
        testCamelCaseLower("你好吗t", "t");
        testCamelCaseLower("你好吗tt", "tt");
        testCamelCaseLower("D", "d");
        testCamelCaseLower("Dw", "dw");
    }

    private void testCamelCaseLower(final String toConvert, final String expected) {
        String result = StringUtils.camelCaseLower(toConvert);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    /**
     * Test of {@link StringUtils#camelCaseUpper(String)}
     */
    @Test
    public void test_camelCaseUpper() {
        testCamelCaseUpper("MP3 ./,test", "Mp3Test");
        testCamelCaseUpper("Mp3 ./,test", "Mp3Test");
        testCamelCaseUpper("MP3      test", "Mp3Test");
        testCamelCaseUpper("MP3 test", "Mp3Test");
        testCamelCaseUpper("-MP3 ./,test", "Mp3Test");
        testCamelCaseUpper("3MP3 ./,test", "Mp3Test");
        testCamelCaseUpper("3MP3 -- -- test", "Mp3Test");
        testCamelCaseUpper("3MP3      test", "Mp3Test");
        testCamelCaseUpper("3MP3  --  --  --  test", "Mp3Test");
        testCamelCaseUpper("Simple Test", "SimpleTest");
        testCamelCaseUpper("-Simple Test", "SimpleTest");
        testCamelCaseUpper("3Simple Test", "SimpleTest");
        testCamelCaseUpper("3MP3 ..///..//--- 65test36", "Mp365test36");
        testCamelCaseUpper("simple --   test", "SimpleTest");
        testCamelCaseUpper("mp3 ./,test", "Mp3Test");
        testCamelCaseUpper("3simple test", "SimpleTest");
        testCamelCaseUpper("-simple test", "SimpleTest");
        testCamelCaseUpper("-simple Test", "SimpleTest");
        testCamelCaseUpper("simple Test", "SimpleTest");
        testCamelCaseUpper("simple       Test", "SimpleTest");
        testCamelCaseUpper("Simple !@#$%^&*()_-+= Test", "SimpleTest");
        testCamelCaseUpper("你好吗", "");
        testCamelCaseUpper("你好吗3", "");
        testCamelCaseUpper("你好吗34", "");
        testCamelCaseUpper("你好吗t", "T");
        testCamelCaseUpper("你好吗tt", "Tt");
        testCamelCaseUpper("D", "D");
        testCamelCaseUpper("Dw", "Dw");
    }

    private void testCamelCaseUpper(final String toConvert, final String expected) {
        String result = StringUtils.camelCaseUpper(toConvert);
        assertNotNull(result);
        assertEquals(expected, result);
    }

    @DataProvider
    public static Object[][] testCasesLowercase() {
        return new String[][] {
                {"+contentType: (webpagecontent or blog)", "+contenttype: (webpagecontent or blog)"},
                {"+contentType: (webpagecontent OR blog)", "+contenttype: (webpagecontent OR blog)"},
                {"+blog.title:\"to live or to die\"", "+blog.title:\"to live or to die\""},
                {"+blog.title:\"TO live OR to die\"", "+blog.title:\"to live OR to die\""},
                {"+blog.title:\"to live OR TO die\"", "+blog.title:\"to live OR TO die\""}
        };
    }

    @Test
    @UseDataProvider("testCasesLowercase")
    public void testLowercaseStringExceptMatchingTokens(final String query,
            final String expectedQuery) {
        final String resultingQuery =
                StringUtils.lowercaseStringExceptMatchingTokens(query,
                        LUCENE_RESERVED_KEYWORDS_REGEX);

        assertEquals(expectedQuery, resultingQuery);

    }

    @DataProvider
    public static Object[][] quotedTextProvider() {
        return new Object[][]{
                {"'Tumblr' is an amazing app", 1},
                {"Tumblr is an amazing 'app'", 1},
                {"Tumblr is an 'amazing' app", 1 },
                {"Tumblr is 'awesome' and 'amazing' ", 2},
                {"Tumblr's users' are disappointed ", 0},
                {"Tumblr's 'acquisition' complete but users' loyalty doubtful", 1},
                {"Tumblr's la la la '123 456' text can be hard to parse. ", 0}, //<-- by design literals with blanks are excluded.
                { "## Container: Blank Container\n"
                + "## This is autogenerated code that cannot be changed\n"
                + "#parseContainer('d71d56b4-0a8b-4bb2-be15-ffa5a23366ea','1539784124854')\n", 2},
                {" \"lol\" \"Hahaha\" ", 2},
                { "## Container: Blank Container in double quotes \n"
                        + "## This is autogenerated code that cannot be changed\n"
                        + "#parseContainer(\"d71d56b4-0a8b-4bb2-be15-ffa5a23366ea\",'1539784124854')\n", 2}, //Mixed match

        };
    }

    @Test
    @UseDataProvider("quotedTextProvider")
    public void testQuotedTextMatch(String textBlock, int expectedResult){
        final List<String> strings = StringUtils.quotedLiteral(textBlock);
        assertEquals(strings.size(), expectedResult);
    }

    @Test
    public void Test_Has_White_Spaces(){
        assertTrue(StringUtils.hasWhiteSpaces("/test-folder/file-name 123.jpg"));
        assertTrue(StringUtils.hasWhiteSpaces("/test folder/file-name-123.jpg"));
        assertTrue(StringUtils.hasWhiteSpaces(" /test-folder/file-name 123.jpg"));
        assertFalse(StringUtils.hasWhiteSpaces(""));
        assertFalse(StringUtils.hasWhiteSpaces("lol"));
    }

    @Test
    public void test_getBasePath() {
        assertEquals("/a/b/c", StringUtils.getBasePath("/a/b/c/d.ext"));
        assertEquals("", StringUtils.getBasePath(""));
        assertEquals("", StringUtils.getBasePath(null));
    }

    @Test
    public void test_shareSamePath() {
        assertTrue(StringUtils.shareSamePath("/a/b/c/d.ext", "/a/b/c/z.ext"));
        assertTrue(StringUtils.shareSamePath("", ""));
        assertTrue(StringUtils.shareSamePath(null, null));
        assertFalse(StringUtils.shareSamePath("/a/b/c/d.ext", "/g/f/e/d.ext"));
    }

    @Test
    public void Test_ConvertCamelToSnake_NullParameter_ShouldReturnBlankString(){
        final String output =  StringUtils.convertCamelToSnake(null);
        assertEquals(StringPool.BLANK, output);
    }

    @Test
    public void Test_ConvertCamelToSnake_BlankStringParameter_ShouldReturnBlankString(){
        final String output =  StringUtils.convertCamelToSnake(StringPool.BLANK);
        assertEquals(StringPool.BLANK, output);
    }

    @Test
    public void Test_ConvertCamelToSnake_StringOfLength1Parameter_ShouldReturnLowerCaseString(){
        final String output =  StringUtils.convertCamelToSnake("A");
        assertEquals("a", output);
    }

    @Test
    public void Test_ConvertCamelToSnake_CamelCaseStringParameter_ShouldReturnSnakeCaseString(){
        final String output =  StringUtils.convertCamelToSnake("inputString");
        assertEquals("input_string", output);
    }

    @Test
    public void Test_ConvertCamelToSnake_SnakeCaseStringParameter_ShouldReturnSnakeCaseString(){
        final String output =  StringUtils.convertCamelToSnake("input_string");
        assertEquals("input_string", output);
    }

    @Test
    public void testToCharArray() {
        // Test when input string is not null
        final String input = "Hello";
        final char[] expected = {'H', 'e', 'l', 'l', 'o'};
        assertArrayEquals(expected, StringUtils.toCharArray(input));

        // Test when input string is null
        assertNull(StringUtils.toCharArray(null));
    }

    @Test
    public void testToCharArraySafe() {
        // Test when input string is not null
        final String input = "Hello";
        final char[] expected = {'H', 'e', 'l', 'l', 'o'};
        assertArrayEquals(expected, StringUtils.toCharArraySafe(input));

        // Test when input string is null
        assertArrayEquals(StringUtils.BLANK_CHARS, StringUtils.toCharArraySafe(null));
    }

    @Test
    public void testDefensiveCopy() {
        // Test when input array is not null
        final char[] input = {'a', 'b', 'c'};
        final char[] result = StringUtils.defensiveCopy(input);
        assertArrayEquals(input, result); // Check if the content is equal
        assertNotSame(input, result); // Check if it's a different instance

        // Test when input array is null
        assertArrayEquals(StringUtils.BLANK_CHARS, StringUtils.defensiveCopy(null)); // Check if the content is equal
    }

    /**
     * Given a text as input
     * When the method hashText is invoked
     * Then it returns the SHA-256 hash of the input text
     *
     * @return The SHA-256 hash of the input text
     */
    @Test
    public void testHashText() {
        String input = "Hello, World!";
        String actualOutput = StringUtils.hashText(input);
        assertEquals(64, actualOutput.length());
    }

    /**
     * Given a text input
     * When calling the joinOneCharElements method
     * Then verify that one-length elements in an underscore delimited String are joined together in the new string.
     */
    @Test
    public void testJoinOneCharElements() {
        String input = "a_b_c_d_e_f";
        String expectedOutput = "abcdef";
        String actualOutput = StringUtils.joinOneCharElements(input);
        Assertions.assertEquals(expectedOutput, actualOutput);

        input = "abc_def_ghi";
        expectedOutput = "abc_def_ghi";
        actualOutput = StringUtils.joinOneCharElements(input);
        Assertions.assertEquals(expectedOutput, actualOutput);

        input = "";
        expectedOutput = "";
        actualOutput = StringUtils.joinOneCharElements(input);
        Assertions.assertEquals(expectedOutput, actualOutput);
    }

}

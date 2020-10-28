package com.dotmarketing.util;


import static com.dotcms.content.elasticsearch.business.ESContentFactoryImpl.LUCENE_RESERVED_KEYWORDS_REGEX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.liferay.util.StringPool;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;
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
        final String expression = null;
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals(expected, StringPool.BLANK);
    }

    @Test
    public void testCheckNull2Interpolate() {


        final Map<String, Object> params = null;
        final String expression = "nothing";
        final String expected = StringUtils.interpolate(expression, params);

        assertNotNull(expected);
        assertEquals(expression, expected);
    }

    @Test
    public void testCheckNull3Interpolate() {


        final Map<String, Object> params = null;
        final String expression = null;
        final String expected = StringUtils.interpolate(expression, params);

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


        final Map<String, Object> params = null;
        final String expression = "{hostId}-mycustomname";
        final String expected = StringUtils.interpolate(expression, params);

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
        assertEquals("", StringUtils.builder(null).toString());
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
}
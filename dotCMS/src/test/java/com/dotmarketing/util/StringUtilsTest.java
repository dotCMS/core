package com.dotmarketing.util;


import com.liferay.util.StringPool;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

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
}
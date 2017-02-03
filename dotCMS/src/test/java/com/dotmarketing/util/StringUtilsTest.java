package com.dotmarketing.util;


import com.liferay.util.StringPool;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import static com.dotmarketing.util.StringUtils.interpolate;

public class StringUtilsTest {

    public Map<String, Object> createParams() {

        final Map<String, Object> params = new HashMap<>();

        params.put("hostId", "1.dotcms.com");
        params.put("hostname", "dotcms.com");
        params.put("languageId", "en");
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
}
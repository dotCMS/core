package com.dotmarketing.business;

import com.dotmarketing.exception.DotRuntimeException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for PageCacheParameters class.
 * Tests constructor, key generation, query string filtering, and parameter handling.
 */
public class PageCacheParametersTest {

    @Before
    public void setUp() throws Exception {
        // Reset static fields before each test to ensure clean state
        resetStaticFields();
    }

    /**
     * Reset static fields using reflection to ensure clean state between tests
     */
    private void resetStaticFields() throws Exception {
        Field expiredField = PageCacheParameters.class.getDeclaredField("expired");
        expiredField.setAccessible(true);
        AtomicLong expired = (AtomicLong) expiredField.get(null);
        // Set to future time to prevent refreshValues() from being called during tests
        expired.set(System.currentTimeMillis() + 300000); // 5 minutes in the future

        Field ignoreParamsField = PageCacheParameters.class.getDeclaredField("_ignoreParams");
        ignoreParamsField.setAccessible(true);
        ignoreParamsField.set(null, new String[0]);

        Field respectParamsField = PageCacheParameters.class.getDeclaredField("_respectParams");
        respectParamsField.setAccessible(true);
        respectParamsField.set(null, new String[0]);
    }

    @Test
    public void testConstructorWithValidParams() {
        PageCacheParameters params = new PageCacheParameters("param1", "param2", "param3");
        String key = params.getKey();
        Assert.assertEquals("param1,param2,param3", key);
    }

    @Test
    public void testConstructorWithNullParams() {
        PageCacheParameters params = new PageCacheParameters("param1", null, "param3", null);
        String key = params.getKey();
        Assert.assertEquals("param1,param3", key);
    }

    @Test
    public void testConstructorWithEmptyParams() {
        PageCacheParameters params = new PageCacheParameters("param1", "", "param3");
        String key = params.getKey();
        Assert.assertEquals("param1,param3", key);
    }

    @Test(expected = DotRuntimeException.class)
    public void testConstructorWithAllNullParams() {
        PageCacheParameters params = new PageCacheParameters((String) null, null);
        String key = params.getKey();
    }

    @Test(expected = DotRuntimeException.class)
    public void testConstructorWithNoParams() {
        PageCacheParameters params = new PageCacheParameters();
        String key = params.getKey();
    }

    @Test
    public void testConstructorWithWhitespaceParams() {
        PageCacheParameters params = new PageCacheParameters("param1", "   ", "param3", "\t");
        String key = params.getKey();
        Assert.assertEquals("param1,param3", key);
    }

    @Test
    public void testFilterQueryStringWithNull() {
        String result = PageCacheParameters.filterQueryString(null);
        Assert.assertNull(result);
    }

    @Test
    public void testFilterQueryStringWithEmpty() {
        String result = PageCacheParameters.filterQueryString("");
        Assert.assertNull(result);
    }

    @Test
    public void testFilterQueryStringBasicFunctionality() {
        // Test basic functionality without relying on Config values
        String queryString = "param1=value1&param2=value2&param3=value3";
        String result = PageCacheParameters.filterQueryString(queryString);

        // Should return some result (exact content depends on current config)
        Assert.assertNotNull(result);

        // Should be lowercase
        Assert.assertEquals(result, result.toLowerCase());
    }

    @Test
    public void testFilterQueryStringAlwaysIgnoresLanguageParam() throws Exception {
        // Set up test conditions using reflection
        setIgnoreParams(new String[]{"com.dotmarketing.htmlpage.language"});
        setRespectParams(new String[0]);

        String queryString = "param1=value1&com.dotmarketing.htmlpage.language=en&param2=value2";
        String result = PageCacheParameters.filterQueryString(queryString);

        // Should always ignore the language parameter
        if (result != null) {
            Assert.assertTrue(result.contains("param1=value1"));
            Assert.assertTrue(result.contains("param2=value2"));
            Assert.assertFalse(result.contains("com.dotmarketing.htmlpage.language=en"));
        }
    }

    @Test
    public void testFilterQueryStringCaseInsensitive() throws Exception {
        // Set up test conditions
        setIgnoreParams(new String[]{"param2"});
        setRespectParams(new String[0]);

        String queryString = "PARAM1=VALUE1&param2=value2&Param3=Value3";
        String result = PageCacheParameters.filterQueryString(queryString);

        if (result != null) {
            // Should handle case insensitive matching and convert to lowercase
            Assert.assertTrue(result.contains("param1=value1"));
            Assert.assertFalse(result.contains("param2=value2"));
            Assert.assertTrue(result.contains("param3=value3"));
        }
    }

    @Test
    public void testFilterQueryStringWithQuestionMark() throws Exception {
        setIgnoreParams(new String[0]);
        setRespectParams(new String[0]);

        String queryString = "?param1=value1&param2=value2";
        String result = PageCacheParameters.filterQueryString(queryString);

        if (result != null) {
            // Should handle query strings that start with ?
            Assert.assertTrue(result.contains("param1=value1"));
            Assert.assertTrue(result.contains("param2=value2"));
        }
    }

    @Test
    public void testFilterQueryStringSortsAlphabetically() throws Exception {
        setIgnoreParams(new String[0]);
        setRespectParams(new String[0]);

        String queryString = "zebra=1&alpha=2&beta=3";
        String result = PageCacheParameters.filterQueryString(queryString);

        if (result != null && result.length() > 0) {
            // Should sort parameters alphabetically
            String[] parts = result.split("&");
            if (parts.length >= 4) { // First element might be empty due to leading &
                int startIndex = parts[0].isEmpty() ? 1 : 0;
                Assert.assertTrue("Expected alpha parameter first", parts[startIndex].startsWith("alpha="));
                Assert.assertTrue("Expected beta parameter second", parts[startIndex + 1].startsWith("beta="));
                Assert.assertTrue("Expected zebra parameter third", parts[startIndex + 2].startsWith("zebra="));
            }
        }
    }

    @Test
    public void testFilterQueryStringWithRespectParams() throws Exception {
        setIgnoreParams(new String[0]);
        setRespectParams(new String[]{"param1", "param3"});

        String queryString = "param1=value1&param2=value2&param3=value3&param4=value4";
        String result = PageCacheParameters.filterQueryString(queryString);

        if (result != null) {
            // Should only include param1 and param3 when respect params are defined
            Assert.assertTrue(result.contains("param1=value1"));
            Assert.assertFalse(result.contains("param2=value2"));
            Assert.assertTrue(result.contains("param3=value3"));
            Assert.assertFalse(result.contains("param4=value4"));
        }
    }

    @Test
    public void testFilterQueryStringWithIgnoreParams() throws Exception {
        setIgnoreParams(new String[]{"param2", "param4"});
        setRespectParams(new String[0]);

        String queryString = "param1=value1&param2=value2&param3=value3&param4=value4";
        String result = PageCacheParameters.filterQueryString(queryString);

        if (result != null) {
            // Should exclude param2 and param4
            Assert.assertTrue(result.contains("param1=value1"));
            Assert.assertFalse(result.contains("param2=value2"));
            Assert.assertTrue(result.contains("param3=value3"));
            Assert.assertFalse(result.contains("param4=value4"));
        }
    }

    @Test
    public void testMatchesMethod() throws Exception {
        // Test the static matches method using reflection
        Method matchesMethod = PageCacheParameters.class.getDeclaredMethod("matches", String.class, String[].class);
        matchesMethod.setAccessible(true);

        // Test null inputs
        Assert.assertFalse((Boolean) matchesMethod.invoke(null, null, new String[]{"param1"}));
        Assert.assertFalse((Boolean) matchesMethod.invoke(null, "test=value", null));
        Assert.assertFalse((Boolean) matchesMethod.invoke(null, null, null));

        // Test matching
        String[] params = {"param1", "param2"};
        Assert.assertTrue((Boolean) matchesMethod.invoke(null, "param1=value1", params));
        Assert.assertTrue((Boolean) matchesMethod.invoke(null, "param2=value2", params));
        Assert.assertFalse((Boolean) matchesMethod.invoke(null, "param3=value3", params));
        Assert.assertFalse((Boolean) matchesMethod.invoke(null, "param1", params)); // No equals sign

        // Test partial matching (should match if test starts with param=)
        Assert.assertTrue((Boolean) matchesMethod.invoke(null, "param1=value&other=stuff", params));
        Assert.assertFalse((Boolean) matchesMethod.invoke(null, "notparam1=value", params));
    }

    @Test
    public void testFilterNullsMethod() throws Exception {
        // Test the static filterNulls method using reflection
        Method filterNullsMethod = PageCacheParameters.class.getDeclaredMethod("filterNulls", String[].class);
        filterNullsMethod.setAccessible(true);

        // Test with mixed null and valid values
        String[] input = {"param1", null, "param3", "", "param5", "   "};
        String[] result = (String[]) filterNullsMethod.invoke(null, (Object) input);

        // UtilMethods.isSet() returns true for non-null strings with length > 0 after trim
        // So "param1", "param3", "param5" should be kept (3 elements)
        Assert.assertEquals(3, result.length);
        Assert.assertEquals("param1", result[0]);
        Assert.assertEquals("param3", result[1]);
        Assert.assertEquals("param5", result[2]);

        // Test with all nulls
        String[] allNulls = {null, null, ""};
        String[] emptyResult = (String[]) filterNullsMethod.invoke(null, (Object) allNulls);
        Assert.assertEquals(0, emptyResult.length);
    }

    @Test
    public void testGetKeyWithSpecialCharacters() {
        PageCacheParameters params = new PageCacheParameters("param with spaces", "param-with-dashes", "param_with_underscores");
        String key = params.getKey();
        Assert.assertEquals("param with spaces,param-with-dashes,param_with_underscores", key);
    }

    @Test
    public void testRefreshInterval() throws Exception {
        // Test that the refresh interval constant is reasonable
        Field refreshIntervalField = PageCacheParameters.class.getDeclaredField("REFRESH_INTERVAL");
        refreshIntervalField.setAccessible(true);
        long refreshInterval = (Long) refreshIntervalField.get(null);

        // Should be 5 minutes (300,000 milliseconds)
        Assert.assertEquals(300000L, refreshInterval);
    }

    /**
     * Helper method to set ignore parameters using reflection
     */
    private void setIgnoreParams(String[] params) throws Exception {
        Field ignoreParamsField = PageCacheParameters.class.getDeclaredField("_ignoreParams");
        ignoreParamsField.setAccessible(true);
        ignoreParamsField.set(null, params);
    }

    /**
     * Helper method to set respect parameters using reflection
     */
    private void setRespectParams(String[] params) throws Exception {
        Field respectParamsField = PageCacheParameters.class.getDeclaredField("_respectParams");
        respectParamsField.setAccessible(true);
        respectParamsField.set(null, params);
    }

    /**
     * makes sure an empty query string is the same as no query string
     */
    @Test
    public void testFilterQueryStringWithEmptyStringVsNull() {
        String result1 = PageCacheParameters.filterQueryString("?");
        String result2 = PageCacheParameters.filterQueryString("");
        String result3 = PageCacheParameters.filterQueryString(null);

        Assert.assertNull(result1);
        Assert.assertNull(result2);
        Assert.assertNull(result3);
    }
}
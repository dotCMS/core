package com.dotcms.contenttype.test;

import com.dotcms.contenttype.util.KeyValueFieldUtil;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * KeyValueFieldUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KeyValueFieldUtilTest {

    /**
     * Test all contentlet json content on field text_area1
     * @deprecated Not used since it pings the contentlet table and it is a heavy process
     */
    @Test
    public void testJsonKeyValueMapAgainstContentletTextArea1() throws Exception {
        /*
        DotConnect dc = new DotConnect();
        dc.setSQL("select * from contentlet where text_area1 is not null and text_area1 like '%{%}' limit 20");

        List<Map<String, Object>> contentlets = dc.loadObjectResults();
        assertThat("Verify list is not empty", contentlets.size() > 0);

        for (Map<String, Object> contentlet : contentlets) {
            String json = (String) contentlet.get("text_area1");
            Map<String, Object> data = KeyValueFieldUtil.JSONValueToHashMap(json);

            Assert.assertNotNull(data);
            assertThat("Map data size > 0", data.size() > 0);
        }
        */
    }

    /**
     * Test a valid json to convert using KeyValueFieldUtil
     */
    @Test
    public void testJsonKeyValueMap() throws Exception {
        String validJson = "{\"fileSize\":\"42\",\"contentType\":\"text/plain; charset=ISO-8859-1\",\"contentEncoding\":\"ISO-8859-1\"}";
        Map<String, Object> data = KeyValueFieldUtil.JSONValueToHashMap(validJson);

        Assert.assertNotNull(data);
        assertThat("Map data size > 0", data.size() > 0);
    }

    /**
     * Test an invalid json to convert using KeyValueFieldUtil
     */
    @Test
    public void testInvalidJsonKeyValueMap() throws Exception {
        String validJson = "{\"fileSize\"9874\",\"contentType\":image/svg+xml\"}";
        Map<String, Object> data;
        try {
            data = KeyValueFieldUtil.JSONValueToHashMap(validJson);
        } catch (Exception ex) {
            data = null;
        }

        assertThat("Map data is null", data == null);
    }

    /**
     * Test a json with backslash to escape it using JS escaped char
     */
    @Test
    public void testJsonKeyValueMapWithBackslash() throws Exception {
        String json = String.format("{\"data\":\"%s\"}", "\\\\WIN-L9IE0C5QN6I\\userdata\\form.xlsx");
        Map<String, Object> data = KeyValueFieldUtil.JSONValueToHashMap(json);

        Assert.assertNotNull(data);
        assertThat("Map data size > 0", data.size() > 0);

        String value = (String) data.get("data");
        Assert.assertNotNull(value);
        assertThat("Value contains escaped backslash", value.contains("&#92;"));
    }
    
    
    /**
     * Test a json with no quotes
     */
    @Test
    public void testJsonKeyValueMapWithNoQuotes() throws Exception {
        String json = String.format("{data:test1, data2:test2, data3:test3}");
        Map<String, Object> data = KeyValueFieldUtil.JSONValueToHashMap(json);

        Assert.assertNotNull(data);
        assertThat("Map data size == 3", data.size() ==3);

        String value = (String) data.get("data2");
        assertThat("data2==test2", "test2".equals(value));

    }
    
    
    
}

package com.dotcms.contenttype.test;

import com.dotcms.contenttype.util.KeyValueFieldUtil;
import com.dotmarketing.common.db.DotConnect;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * KeyValueFieldUtilTest
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class KeyValueFieldUtilTest extends ContentTypeBaseTest {

    /**
     * Test all contentlet json content on field text_area1
     */
    @Test
    public void testJsonKeyValueMapAgainstContentletTextArea1() throws Exception {
        DotConnect dc = new DotConnect();
        dc.setSQL("select * from contentlet where text_area1 is not null and text_area1 like '%{%}'");

        List<Map<String, Object>> contentlets = dc.loadObjectResults();
        assertThat("Verify list is not empty", contentlets.size() > 0);

        for (Map<String, Object> contentlet : contentlets) {
            String json = (String) contentlet.get("text_area1");
            Map<String, Object> data = KeyValueFieldUtil.JSONValueToHashMap(json);

            Assert.assertNotNull(data);
            assertThat("Map data size > 0", data.size() > 0);
        }
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
}

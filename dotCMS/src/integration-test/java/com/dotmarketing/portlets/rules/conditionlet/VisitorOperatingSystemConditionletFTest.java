package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.LicenseTestUtil;
import eu.bitwalker.useragentutils.OperatingSystem;

import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.IntegrationTestInitService;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URLConnection;


import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class VisitorOperatingSystemConditionletFTest extends ConditionletFTest{

    protected Condition getCondition(String id, String value) {
        //Creating the Conditionlet for the Browser language
        Condition condition = conditionDataGen.next();
        condition.setConditionletId(VisitorOperatingSystemConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, id);
        condition.addValue(VisitorOperatingSystemConditionlet.OS_NAME__KEY, value);
        return condition;
    }

    private URLConnection makeRequest(String url, String userAgentHeaderValue) throws IOException {
        ApiRequest apiRequest = new ApiRequest(request, "User-Agent");
        return apiRequest.makeRequest(url, userAgentHeaderValue);
    }

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @Test
    public void testIsComparison () throws IOException {

        Condition condition = getCondition(IS.getId(), OperatingSystem.WINDOWS_81.getName());
        String[] keyAndValu = createRule(condition,  Rule.FireOn.EVERY_REQUEST);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];


        URLConnection conn = makeRequest("about-us/index", "Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = makeRequest("about-us/index", "Mozilla/5.0 (Android; Mobile; rv:30.0) Gecko/30.0 Firefox/30.0");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = makeRequest("about-us/index", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:10.0) Gecko/20100101 Firefox/33.0");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = makeRequest("about-us/index", "Mozilla/5.0 (X11; Linux i686; rv:10.0) Gecko/20100101 Firefox/33.0");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

    }

    @Test
    public void testIsNotComparison () throws IOException {

        Condition condition = getCondition(IS_NOT.getId(), OperatingSystem.WINDOWS_81.getName());
        String[] keyAndValu = createRule(condition,  Rule.FireOn.EVERY_REQUEST);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];


        URLConnection conn = makeRequest("about-us/index", "Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = makeRequest("about-us/index", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = makeRequest("about-us/index", "Mozilla/5.0 (Android; Mobile; rv:30.0) Gecko/30.0 Firefox/30.0");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = makeRequest("about-us/index", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:10.0) Gecko/20100101 Firefox/33.0");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = makeRequest("about-us/index", "Mozilla/5.0 (X11; Linux i686; rv:10.0) Gecko/20100101 Firefox/33.0");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

    }




}
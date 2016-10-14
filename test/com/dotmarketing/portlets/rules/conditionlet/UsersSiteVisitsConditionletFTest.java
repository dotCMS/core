package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.LicenseTestUtil;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.IntegrationTestInitService;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.net.URLConnection;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by freddyrodriguez on 10/3/16.
 */
public class UsersSiteVisitsConditionletFTest extends ConditionletFTest{

    private UsersSiteVisitsUtilTest usersSiteVisitsUtilTest;

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @Before
    public void innerInit () {
        usersSiteVisitsUtilTest = new UsersSiteVisitsUtilTest(request);
    }

    @Before
    public void cleanCookies(){
        usersSiteVisitsUtilTest.clean();
    }

    protected Condition getCondition(String id, String value) {
        //Creating the Conditionlet for the Browser language
        Condition condition = conditionDataGen.next();
        condition.setConditionletId(UsersSiteVisitsConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, id);
        condition.addValue(UsersSiteVisitsConditionlet.SITE_VISITS_KEY, value);
        return condition;
    }

    private void testEqualsComparison (Rule.FireOn fireOn) throws IOException {

        Condition condition = getCondition(EQUAL.getId(), "2");
        String[] keyAndValu = createRule(condition, fireOn);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];


        URLConnection conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

    }

    public void testLessThanComparison (Rule.FireOn fireOn) throws IOException {

        Condition condition = getCondition(LESS_THAN.getId(), "2");
        String[] keyAndValu = createRule(condition, fireOn);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];


        URLConnection conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertNull("Specified response header should be present in the Response.", conn.getHeaderField(randomKey));

    }

    public void testGreaterThanComparison (Rule.FireOn fireOn) throws IOException {

        Condition condition = getCondition(GREATER_THAN.getId(), "2");
        String[] keyAndValu = createRule(condition, fireOn);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];


        URLConnection conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertEquals("Specified response header should be NOT present in the Response.", value, conn.getHeaderField(randomKey));

    }

    public void testLessThanOrEqualsComparison (Rule.FireOn fireOn) throws IOException {

        Condition condition = getCondition(LESS_THAN_OR_EQUAL.getId(), "2");
        String[] keyAndValu = createRule(condition, fireOn);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];


        URLConnection conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertNull("Specified response header should be present in the Response.", conn.getHeaderField(randomKey));
    }

    public void testGreaterThanOrEqualsComparison (Rule.FireOn fireOn) throws IOException {

        Condition condition = getCondition(GREATER_THAN_OR_EQUAL.getId(), "2");
        String[] keyAndValu = createRule(condition, fireOn);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];

        URLConnection conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertEquals(value, conn.getHeaderField(randomKey));

        conn = usersSiteVisitsUtilTest.makeNewSessionRequest("about-us/index");
        assertEquals(value, conn.getHeaderField(randomKey));
    }

    @Test
    public void testEqualsComparisonEveryRequestRule () throws IOException {

        testEqualsComparison( Rule.FireOn.EVERY_REQUEST );
    }

    @Test
    public void testEqualsComparisonEveryPageRule () throws IOException {
        testEqualsComparison( Rule.FireOn.EVERY_PAGE );
    }

    @Test
    public void testLessThanComparisonEveryRequestRule () throws IOException {
        testLessThanComparison( Rule.FireOn.EVERY_REQUEST );
    }

    @Test
    public void testLessThanComparisonEveryPageRule () throws IOException {
        testLessThanComparison( Rule.FireOn.EVERY_PAGE );
    }

    @Test
    public void testGreaterThanComparisonEveryRequestRule () throws IOException {
        testGreaterThanComparison( Rule.FireOn.EVERY_REQUEST );
    }

    @Test
    public void testGreaterThanComparisonEveryPageRule () throws IOException {
        testGreaterThanComparison( Rule.FireOn.EVERY_PAGE );
    }

    @Test
    public void testLessThanOrEqualsComparisonEveryRequestRule () throws IOException {
        testLessThanOrEqualsComparison( Rule.FireOn.EVERY_REQUEST );
    }

    @Test
    public void testLessThanOrEqualsComparisoEveryPageRule () throws IOException {
        testLessThanOrEqualsComparison( Rule.FireOn.EVERY_PAGE );
    }

    @Test
    public void testGreaterThanEquasComparisonEveryRequestRule () throws IOException {
        testGreaterThanOrEqualsComparison( Rule.FireOn.EVERY_REQUEST );
    }

    @Test
    public void testGreaterThanEqualsComparisonEveryPageRule () throws IOException {
        testGreaterThanOrEqualsComparison( Rule.FireOn.EVERY_PAGE );
    }


}

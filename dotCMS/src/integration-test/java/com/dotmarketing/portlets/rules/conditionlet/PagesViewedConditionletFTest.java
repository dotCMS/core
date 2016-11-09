package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.LicenseTestUtil;
import com.dotcms.repackage.com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.IntegrationTestInitService;
import com.dotmarketing.util.WebKeys;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLConnection;
import java.util.List;
import java.util.Random;
import com.dotcms.visitor.domain.Visitor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.*;

/**
 * Created by freddy on 27/01/16.
 */
public class PagesViewedConditionletFTest {

    private Random random = new Random();
    private HttpServletRequest request;

    private ConditionDataGen conditionDataGen = new ConditionDataGen();
    private ConditionletTestUtil conditionletTestUtil = new ConditionletTestUtil();

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @Before
    public void init () {
        request = ServletTestRunner.localRequest.get();

        HttpSession session = request.getSession(false);

        if ( session != null ) {
            session.invalidate();
        }

        session = request.getSession(true);
        session.setAttribute(WebKeys.VISITOR, new Visitor());
    }

    @After
    public void tearDown () throws Exception {
        conditionletTestUtil.clear();
    }

    @Test
    public void testEqualsComparison () throws IOException {

        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";
        Condition condition = getCondition(EQUAL.getId(), "3");


        //Persist the Conditionlet
        String ruleName = String.format("PagesViewedConditionletFTest - fireOnEveryRequest %s", random.nextInt());
        conditionletTestUtil.createRandomSetResponseHeaderRule(condition, randomKey, value, ruleName);

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        System.out.println("about-us/index");

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        System.out.println("products/");
        conn = apiRequest.makeRequest("products/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        System.out.println("products/");
        conn = apiRequest.makeRequest("products/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        //Admin page dont have to count
        System.out.println("admin/");
        conn = apiRequest.makeRequest("admin/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));

        //File request dont have to count
        System.out.println("images/404.jpg");
        conn = apiRequest.makeRequest("images/404.jpg");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));


        System.out.println("contact-us/");
        conn = apiRequest.makeRequest("contact-us/");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
    }

    @Test
    public void testNotEqualsComparison () throws IOException {

        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";

        Condition condition = getCondition(NOT_EQUAL.getId(), "2");

        //Persist the Conditionlet
        String ruleName = String.format("PagesViewedConditionletFTest - fireOnEveryRequest %s", random.nextInt());
        conditionletTestUtil.createRandomSetResponseHeaderRule(condition, randomKey, value, ruleName);

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
    }

    @Test
    public void testLessThanComparison () throws IOException {

        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";

        Condition condition = getCondition(LESS_THAN.getId(), "2");

        //Persist the Conditionlet
        String ruleName = String.format("PagesViewedConditionletFTest - fireOnEveryRequest %s", random.nextInt());
        conditionletTestUtil.createRandomSetResponseHeaderRule(condition, randomKey, value, ruleName);

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
    }

    @Test
    public void testGreaterThanComparison () throws IOException {

        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";

        Condition condition = getCondition(GREATER_THAN.getId(), "2");

        //Persist the Conditionlet
        String ruleName = String.format("PagesViewedConditionletFTest - fireOnEveryRequest %s", random.nextInt());
        conditionletTestUtil.createRandomSetResponseHeaderRule(condition, randomKey, value, ruleName);

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
    }

    @Test
    public void testLessThanOrEqualsComparison () throws IOException {

        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";

        Condition condition = getCondition(LESS_THAN_OR_EQUAL.getId(), "2");

        //Persist the Conditionlet
        String ruleName = String.format("PagesViewedConditionletFTest - fireOnEveryRequest %s", random.nextInt());
        conditionletTestUtil.createRandomSetResponseHeaderRule(condition, randomKey, value, ruleName);

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertEquals("Specified response header should be NOT present in the Response.", value, conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
    }

    @Test
    public void testGreaterThanOrEqualsComparison () throws IOException {

        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";

        Condition condition = getCondition(GREATER_THAN_OR_EQUAL.getId(), "2");

        //Persist the Conditionlet
        String ruleName = String.format("PagesViewedConditionletFTest - fireOnEveryRequest %s", random.nextInt());
        conditionletTestUtil.createRandomSetResponseHeaderRule(condition, randomKey, value, ruleName);

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
    }

    private Condition getCondition(String id, String value) {
        //Creating the Conditionlet for the Browser language
        Condition condition = conditionDataGen.next();
        condition.setConditionletId(PagesViewedConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, id);
        condition.addValue(PagesViewedConditionlet.NUMBER_PAGES_VIEWED_INPUT_KEY, value);
        return condition;
    }

}

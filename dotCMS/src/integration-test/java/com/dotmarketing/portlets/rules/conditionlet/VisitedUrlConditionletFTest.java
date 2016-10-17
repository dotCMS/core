package com.dotmarketing.portlets.rules.conditionlet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.CONTAINS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.ENDS_WITH;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.IS_NOT;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.REGEX;
import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.STARTS_WITH;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.dotcms.LicenseTestUtil;
import com.dotcms.repackage.com.google.common.collect.Lists;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotmarketing.portlets.rules.ParameterDataGen;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.actionlet.RuleActionDataGen;
import com.dotmarketing.portlets.rules.actionlet.SetResponseHeaderActionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.IntegrationTestInitService;

public class VisitedUrlConditionletFTest {

    private Random random = new Random();
    private HttpServletRequest request;
    private RuleDataGen ruleDataGen;
    private ConditionGroupDataGen conditionGroupDataGen = new ConditionGroupDataGen();
    private ConditionDataGen conditionDataGen = new ConditionDataGen();

    private List<Rule> rulesToRemove = Lists.newArrayList();

    @BeforeClass
    public static void prepare () throws Exception {
        //Setting web app environment
        IntegrationTestInitService.getInstance().init();
        LicenseTestUtil.getLicense();
    }

    @Before
    public void init() {
        request = ServletTestRunner.localRequest.get();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
    }

    @After
    public void tearDown() throws Exception {
        for (Rule rule : rulesToRemove) {
            ruleDataGen.remove(rule);
        }
        rulesToRemove.clear();
    }

    @Test
    public void testIsComparison() throws IOException {
        final String randomKey = "test-" + random.nextInt();
        final String value = randomKey + "-value";

        Condition condition = conditionDataGen.next();
        condition.setConditionletId(VisitedUrlConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, IS.getId());
        condition.addValue(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/about-us/");
        createRandomSetResponseHeaderRule(condition, randomKey, value);

        ApiRequest apiRequest = new ApiRequest(request);
        URLConnection conn = apiRequest.makeRequest("about-us/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertEquals("Specified response header should be present in the Response.", value,
                conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        assertEquals("Specified response header should be present in the Response.", value,
                conn.getHeaderField(randomKey));
    }

    @Test
    public void testIsNotComparison() throws IOException {
        final String randomKey = "test-" + random.nextInt();
        final String value = randomKey + "-value";

        Condition condition = conditionDataGen.next();
        condition.setConditionletId(VisitedUrlConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, IS_NOT.getId());
        condition.addValue(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/contact-us/");
        createRandomSetResponseHeaderRule(condition, randomKey, value);

        ApiRequest apiRequest = new ApiRequest(request);
        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response: ", value,
                conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertEquals("Specified response header should be present in the Response: ", value,
                conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        assertEquals("Specified response header should be present in the Response: ", value,
                conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("services/first-time-investors");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
    }

    @Test
    public void testStartWithComparison() throws IOException {
        final String randomKey = "test-" + random.nextInt();
        final String value = randomKey + "-value";

        Condition condition = conditionDataGen.next();
        condition.setConditionletId(VisitedUrlConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, STARTS_WITH.getId());
        condition.addValue(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "/contac");
        createRandomSetResponseHeaderRule(condition, randomKey, value);

        ApiRequest apiRequest = new ApiRequest(request);
        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("contact-us/");
        conn = apiRequest.makeRequest("about-us/index");
        assertEquals("Specified response header should be present in the Response.", value,
                conn.getHeaderField(randomKey));
    }

    @Test
    public void testEndsWithComparison() throws IOException {
        final String randomKey = "test-" + random.nextInt();
        final String value = randomKey + "-value";

        Condition condition = conditionDataGen.next();
        condition.setConditionletId(VisitedUrlConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, ENDS_WITH.getId());
        condition.addValue(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "dex");
        createRandomSetResponseHeaderRule(condition, randomKey, value);

        ApiRequest apiRequest = new ApiRequest(request);
        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertEquals("Specified response header should be present in the Response.", value,
                conn.getHeaderField(randomKey));
    }

    @Test
    public void testContainsComparison() throws IOException {
        final String randomKey = "test-" + random.nextInt();
        final String value = randomKey + "-value";

        Condition condition = conditionDataGen.next();
        condition.setConditionletId(VisitedUrlConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, CONTAINS.getId());
        condition.addValue(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, "duct");
        createRandomSetResponseHeaderRule(condition, randomKey, value);

        ApiRequest apiRequest = new ApiRequest(request);
        URLConnection conn = apiRequest.makeRequest("about-us/");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        conn = apiRequest.makeRequest("about-us/");
        assertEquals("Specified response header should be present in the Response.", value,
                conn.getHeaderField(randomKey));
    }

    @Test
    public void testRegexComparison() throws IOException {
        final String randomKey = "test-" + random.nextInt();
        final String value = randomKey + "-value";

        Condition condition = conditionDataGen.next();
        condition.setConditionletId(VisitedUrlConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, REGEX.getId());
        condition.addValue(VisitedUrlConditionlet.PATTERN_URL_INPUT_KEY, ".*-us.*");
        createRandomSetResponseHeaderRule(condition, randomKey, value);

        ApiRequest apiRequest = new ApiRequest(request);
        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        conn = apiRequest.makeRequest("products/");
        assertEquals("Specified response header should be present in the Response.", value,
                conn.getHeaderField(randomKey));
    }

    private Rule createRandomSetResponseHeaderRule(Condition condition, String randomKey, String value) {
        assertNull("Test key should not be present on the session already: ",
                request.getSession().getAttribute(randomKey));

        ruleDataGen = new RuleDataGen(Rule.FireOn.EVERY_REQUEST).name(String.format(
                "SetResponseHeaderActionletFTest - fireOnEveryRequest %s", random.nextInt()));
        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        ConditionGroup group = conditionGroupDataGen.ruleId(rule.getId()).nextPersisted();

        condition.setConditionGroup(group.getId());
        conditionDataGen.persist(condition);

        RuleActionDataGen actionDataGen = new RuleActionDataGen().ruleId(rule.getId());
        RuleAction action = actionDataGen.actionlet(SetResponseHeaderActionlet.class).priority(random.nextInt(100) + 1)
                .next();

        ParameterDataGen pDataGen = new ParameterDataGen().ownerId(action.getId());
        action.addParameter(pDataGen.key(SetResponseHeaderActionlet.HEADER_KEY).value(randomKey).next());
        action.addParameter(pDataGen.key(SetResponseHeaderActionlet.HEADER_VALUE).value(value).next());

        actionDataGen.persist(action);
        return rule;
    }

    private class ApiRequest {

        private final String baseUrl;
        private final String jSessionIdCookie;

        public ApiRequest() {
            this(ServletTestRunner.localRequest.get());
        }

        public ApiRequest(HttpServletRequest request) {
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String jSessionId = request.getSession().getId();
            baseUrl = String.format("http://%s:%s/", serverName, serverPort);
            jSessionIdCookie = "JSESSIONID=" + jSessionId;
        }

        public URLConnection makeRequest() throws IOException {
            return makeRequest(new URL(baseUrl + "robots.txt?t=" + System.currentTimeMillis()));
        }

        public URLConnection makeRequest(String path) throws IOException {
            return makeRequest(new URL(baseUrl + path));
        }

        public URLConnection makeRequest(URL url, String... cookies) throws IOException {
            URLConnection con = url.openConnection();

            StringBuilder cookiesSB = new StringBuilder();

            if (jSessionIdCookie != null) {
                con.setRequestProperty("Cookie", jSessionIdCookie);
                cookiesSB.append(jSessionIdCookie).append("; ");
            }

            if (cookies != null) {
                for (String cookie : cookies) {
                    cookiesSB.append(cookie).append("; ");
                }
            }

            if (cookiesSB.length() > 0) {
                con.setRequestProperty("Cookie", cookiesSB.toString());
            }

            con.connect();
            con.getInputStream();
            return con;
        }
    }
}

package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.LicenseTestUtil;
import com.dotcms.TestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.After;
import com.dotmarketing.portlets.rules.ApiRequest;
import com.dotmarketing.portlets.rules.ParameterDataGen;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionDataGen;
import com.dotmarketing.portlets.rules.conditionlet.ConditionGroupDataGen;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Logger;
import java.io.IOException;
import java.net.URLConnection;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * jUnit test used to verify the results of calling the actionlets provided
 * out of the box in dotCMS.
 *
 * @author Geoff M. Granum
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class SetResponseHeaderActionletFTest extends TestBase {

    private final Random random = new Random();
    HttpServletRequest request = ServletTestRunner.localRequest.get();
    private RuleDataGen ruleDataGen;

    private List<Rule> rulesToRemove = Lists.newArrayList();

    public SetResponseHeaderActionletFTest() {
    }

    @BeforeClass
    public static void prepare () throws Exception {
        LicenseTestUtil.getLicense();
    }

    @Test
    public void testFireOnEveryRequest() throws Exception {
        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";

        createRandomSetResponseHeaderRule(randomKey, value);

        ApiRequest apiRequest = new ApiRequest();
        URLConnection conn = apiRequest.makeRequest();
        assertEquals("Specified response header should be present in the Response: ", value, conn.getHeaderField(randomKey));

    }

    @Test
    public void testHowManyExecutionsInFiveSeconds() throws Exception {
        ApiRequest apiRequest = new ApiRequest();
        int seconds = 5;
        int count = runForDuration(apiRequest, null, null, TimeUnit.SECONDS.toMillis(seconds));
        Logger.info(SetResponseHeaderActionletFTest.class,
                    String.format("Executed %s requests with no rules present, for %s requests per second.",
                                  count,
                                  count / (double)seconds ));

        String randomKey = null;
        String value = null;
        for(int i = 0; i < 10; i++){
            randomKey = "test-" + random.nextInt();
            value = randomKey + "-value";
            createRandomSetResponseHeaderRule(randomKey, value);
        }

        count = runForDuration(apiRequest, randomKey, value, TimeUnit.SECONDS.toMillis(seconds));
        Logger.info(SetResponseHeaderActionletFTest.class,
                    String.format("Warmup: Executed %s requests, each with %s simple rules in 5 seconds, for %s requests per second.",
                                         count,
                                         10,
                                  count / (double)seconds));


        count = runForDuration(apiRequest, randomKey, value, TimeUnit.SECONDS.toMillis(seconds));
        Logger.info(SetResponseHeaderActionletFTest.class,
                    String.format("Executed %s requests, each with %s simple rules in 5 seconds, for %s requests per second.",
                                         count,
                                         10,
                                  count / (double)seconds));


    }

    private int runForDuration(ApiRequest apiRequest, String randomKey, String value, long millis) throws IOException {
        int count = 0;
        Long end = System.currentTimeMillis() + millis;
        do {
            URLConnection conn = apiRequest.makeRequest();
            // just test for the presence of one.
            if(randomKey != null) {
                assertEquals("Specified response header should be present in the Response: ", value, conn.getHeaderField(randomKey));
            }
            count++;
        } while (System.currentTimeMillis() < end);
        return count;
    }

    private Rule createRandomSetResponseHeaderRule(String randomKey, String value) {
        assertNull("Test key should not be present on the session already: ", request.getSession().getAttribute(randomKey));

        ruleDataGen =
            new RuleDataGen(Rule.FireOn.EVERY_REQUEST).name(String.format("SetResponseHeaderActionletFTest - fireOnEveryRequest %s", random.nextInt()));
        Rule rule = ruleDataGen.nextPersisted();
        rulesToRemove.add(rule);

        RuleActionDataGen actionDataGen = new RuleActionDataGen().ruleId(rule.getId());
        RuleAction action = actionDataGen.actionlet(SetResponseHeaderActionlet.class).priority(random.nextInt(100) + 1).next();

        ParameterDataGen pDataGen = new ParameterDataGen().ownerId(action.getId());
        action.addParameter(pDataGen.key(SetResponseHeaderActionlet.HEADER_KEY).value(randomKey).next());
        action.addParameter(pDataGen.key(SetResponseHeaderActionlet.HEADER_VALUE).value(value).next());

        actionDataGen.persist(action);
        return rule;
    }

    @After
    public void tearDown() throws Exception {
        for (Rule rule : rulesToRemove) {
            ruleDataGen.remove(rule);
        }
        rulesToRemove.clear();
    }
}







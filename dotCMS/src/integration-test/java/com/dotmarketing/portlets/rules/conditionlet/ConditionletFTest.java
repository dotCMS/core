package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.LicenseTestUtil;
import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.WebKeys;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLConnection;
import java.util.Random;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EQUAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by freddyrodriguez on 10/3/16.
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public abstract class  ConditionletFTest {

    protected Random random = new Random();
    protected HttpServletRequest request;

    protected ConditionDataGen conditionDataGen = new ConditionDataGen();
    protected ConditionletTestUtil conditionletTestUtil = new ConditionletTestUtil();

    @BeforeClass
    public static void prepare () throws Exception {
        LicenseTestUtil.getLicense();
    }

    @Before
    public void init () {
        request = ServletTestRunner.localRequest.get();

        HttpSession session = request.getSession(false);

        if ( session != null ) {
            session.invalidate();
        }
    }

    @After
    public void tearDown () throws Exception {
        conditionletTestUtil.clear();
    }

    protected abstract Condition getCondition(String id, String value);

    protected String[] createRule(Condition condition, Rule.FireOn fireOn){
        String randomKey = "test-" + random.nextInt();
        String value = randomKey + "-value";

        //Persist the Conditionlet
        String ruleName = String.format(this.getClass().getSimpleName() + " - fireOnEveryRequest %s", random.nextInt());
        conditionletTestUtil.createRandomSetResponseHeaderRule(condition, randomKey, value, ruleName, fireOn);

        return new String[]{ randomKey, value };
    }

}

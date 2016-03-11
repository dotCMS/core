package com.dotmarketing.portlets.rules.conditionlet;

import com.dotcms.visitor.domain.Visitor;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.CookieUtilTest;
import com.dotmarketing.util.WebKeys;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.dotmarketing.portlets.rules.parameter.comparison.Comparison.EQUAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Created by freddyrodriguez on 10/3/16.
 */
public class UsersSiteVisitsConditionletFTest extends ConditionletFTest{


    protected Condition getCondition(String id, String value) {
        //Creating the Conditionlet for the Browser language
        Condition condition = conditionDataGen.next();
        condition.setConditionletId(UsersSiteVisitsConditionlet.class.getSimpleName());
        condition.addValue(Conditionlet.COMPARISON_KEY, id);
        condition.addValue(UsersSiteVisitsConditionlet.SITE_VISITS_KEY, value);
        return condition;
    }

    @Test
    public void testEqualsComparisonEveryRequestRule () throws IOException {

        Condition condition = getCondition(EQUAL.getId(), "2");
        String[] keyAndValu = createRule(condition, Rule.FireOn.EVERY_REQUEST);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        String[] cookiesAsString = CookieUtilTest.getCookiesAsString(conn);

        conn = apiRequest.makeRequest("about-us/index",  null, cookiesAsString);
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        cookiesAsString = getCookiesToNextRequest(conn, cookiesAsString);
        cookiesAsString = deleteOncePerVisitCookie(cookiesAsString);

        conn = apiRequest.makeRequest("about-us/index", null, cookiesAsString);
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
    }

    @Test
    public void testEqualsComparisonEveryPageRule () throws IOException {

        Condition condition = getCondition(EQUAL.getId(), "2");
        String[] keyAndValu = createRule(condition, Rule.FireOn.EVERY_PAGE);
        String randomKey = keyAndValu[0];
        String value = keyAndValu[1];

        //Execute some requests and validate the responses
        ApiRequest apiRequest = new ApiRequest(request);

        URLConnection conn = apiRequest.makeRequest("about-us/index");
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        String[] cookiesAsString = CookieUtilTest.getCookiesAsString(conn);

        conn = apiRequest.makeRequest("about-us/index",  null, cookiesAsString);
        assertNull("Specified response header should be NOT present in the Response.", conn.getHeaderField(randomKey));
        cookiesAsString = getCookiesToNextRequest(conn, cookiesAsString);
        cookiesAsString = deleteOncePerVisitCookie(cookiesAsString);

        conn = apiRequest.makeRequest("about-us/index", null, cookiesAsString);
        assertEquals("Specified response header should be present in the Response.", value, conn.getHeaderField(randomKey));
    }

    private String[] getCookiesToNextRequest(URLConnection conn, String[] cookiesAsString) {
        String[] newCookiesAsString = CookieUtilTest.getCookiesAsString(conn);

        if (newCookiesAsString == null) {
            return cookiesAsString;
        }else{
            return newCookiesAsString;
        }
    }

    private String[] deleteOncePerVisitCookie(String[] cookiesAsString) {

        List<String> result = new ArrayList<>();

        for (String cookie : cookiesAsString) {
            if (!cookie.startsWith( WebKeys.ONCE_PER_VISIT_COOKIE )){
                result.add( cookie );
            }
        }

        String[] arrayResult = new String[ result.size() ];
        return result.toArray( arrayResult );
    }
}

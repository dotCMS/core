package com.dotmarketing.portlets.rules.actionlet;

import com.dotcms.LicenseTestUtil;
import com.dotcms.TestBase;
import com.dotcms.repackage.com.google.common.collect.Lists;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotcms.enterprise.rules.RulesAPI;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

/**
 * jUnit test used to verify the results of calling the actionlets provided
 * out of the box in dotCMS.
 *
 * @author Oscar Arrieta
 * @version 1.0
 * @since 09-22-2015
 *
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class SetSessionAttributeActionletFTest extends TestBase {

    private HttpServletRequest request;
    String ruleId;
    private final String robotsTxtUrl;
    private final String indexUrl;

    public SetSessionAttributeActionletFTest(){
        request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        robotsTxtUrl = String.format("http://%s:%s/robots.txt?t=", serverName, serverPort);
        indexUrl = String.format("http://%s:%s/", serverName, serverPort);
        ruleId = "";
    }

    @BeforeClass
    public static void prepare () throws Exception {
        LicenseTestUtil.getLicense();
    }

    @Test
    public void testFireOnEveryRequest() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.EVERY_REQUEST, firstTime);

        makeRequest(robotsTxtUrl + System.currentTimeMillis(),
                "JSESSIONID=" + request.getSession().getId());

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest(robotsTxtUrl,
                "JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(secondTime, secondTimeRequest);

        assertNotSame(firstTimeRequest, secondTimeRequest);

        deleteRule();
    }

    @Test
    public void testFireOnEveryPage() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.EVERY_PAGE, firstTime);

        makeRequest(indexUrl,
                "JSESSIONID=" + request.getSession().getId());

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest(robotsTxtUrl,
                "JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, secondTimeRequest);

        deleteRule();
    }

    @Test
    public void testFireOnOncePerVisit() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.ONCE_PER_VISIT, firstTime);

        URLConnection conn = makeRequest(indexUrl,
                "JSESSIONID=" + request.getSession().getId());

        String oncePerVisitCookie = getCookie(conn, com.dotmarketing.util.WebKeys.ONCE_PER_VISIT_COOKIE);

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest(indexUrl,
                oncePerVisitCookie + ";JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, secondTimeRequest);

        deleteRule();
    }

    @Test
    public void testFireOnOncePerVisitor() throws Exception {

        String firstTime = String.valueOf(System.currentTimeMillis());
        createRule(Rule.FireOn.ONCE_PER_VISITOR, firstTime);

        URLConnection conn = makeRequest(indexUrl,
                "JSESSIONID=" + request.getSession().getId());

        String longLivedCookie = getCookie(conn, com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

        String firstTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, firstTimeRequest);

        String secondTime = String.valueOf(System.currentTimeMillis());
        updateRuleActionParam(secondTime);

        makeRequest(indexUrl,
                longLivedCookie + ";JSESSIONID=" + request.getSession().getId());

        String secondTimeRequest = (String)request.getSession().getAttribute("time");
        assertEquals(firstTime, secondTimeRequest);

        deleteRule();
    }

    private URLConnection makeRequest(String urlStr, String cookie) throws IOException {
        URL url = new URL(urlStr);
        URLConnection con = url.openConnection();

        if (cookie != null) {
            con.setRequestProperty("Cookie", cookie);
        }

        con.connect();
        con.getInputStream();
        return con;
    }

    private String getCookie(URLConnection conn, String cookieName) {

        String longLivedCookie = null;
        Map<String, List<String>> headerFields = conn.getHeaderFields();

        Set<String> headerFieldsSet = headerFields.keySet();
        Iterator<String> hearerFieldsIter = headerFieldsSet.iterator();

        while (hearerFieldsIter.hasNext()) {

            String headerFieldKey = hearerFieldsIter.next();

            if ("Set-Cookie".equalsIgnoreCase(headerFieldKey)) {

                List<String> headerFieldValue = headerFields.get(headerFieldKey);

                for (String headerValue : headerFieldValue) {
                    String[] fields = headerValue.split(";");
                    String cookieValue = fields[0];
                    if (cookieValue.contains(cookieName)) {
                        longLivedCookie = cookieValue;
                        break;
                    }
                }
            }

            if (longLivedCookie != null)
                break;

        }
        return longLivedCookie;
    }

    private void updateRuleActionParam(String sessionValue) throws Exception{
        RulesAPI rulesAPI = APILocator.getRulesAPI();
        User user = APILocator.getUserAPI().getSystemUser();

        Rule rule = rulesAPI.getRuleById(ruleId, user, false);
        //As we created the rule, we are sure there is only one in there.
        RuleAction ruleAction = rule.getRuleActions().get(0);

        //Updating just the sessionValue RuleActionParameter.
        List<ParameterModel> parameterModels = Lists.newArrayList(ruleAction.getParameters().values());
        for (ParameterModel parameterModel : parameterModels) {
            if (parameterModel.getKey().equals("sessionValue")){
                parameterModel.setValue(sessionValue);
            }
        }

        //Saving updated Action.
        rulesAPI.saveRuleAction(ruleAction, user, false);
    }

    private void createRule(Rule.FireOn fireOn, String sessionValue) throws Exception {
        RulesAPI rulesAPI = APILocator.getRulesAPI();

        User user = APILocator.getUserAPI().getSystemUser();

        HostAPI hostAPI = APILocator.getHostAPI();
        Host defaultHost = hostAPI.findDefaultHost(user, false);

        // Create Rule
        Rule rule = new Rule();
        rule.setName(fireOn.name() + "Rule");
        rule.setParent(defaultHost.getIdentifier());
        rule.setEnabled(true);
        rule.setFireOn(fireOn);

        rulesAPI.saveRule(rule, user, false);

        ruleId = rule.getId();



        RuleAction action = new RuleAction();
        action.setActionlet(SetSessionAttributeActionlet.class.getSimpleName());
        action.setRuleId(rule.getId());

        ParameterModel timeKeyParam = new ParameterModel();
        timeKeyParam.setOwnerId(action.getId());
        timeKeyParam.setKey("sessionKey");
        timeKeyParam.setValue("time");

        ParameterModel timeValueParam = new ParameterModel();
        timeValueParam.setOwnerId(action.getId());
        timeValueParam.setKey("sessionValue");
        timeValueParam.setValue(sessionValue);

        List<ParameterModel> params = new ArrayList<>();
        params.add(timeKeyParam);
        params.add(timeValueParam);

        action.setParameters(params);

        rulesAPI.saveRuleAction(action, user, false);
    }

    @After
    public void deleteRule() throws DotDataException, DotSecurityException {
        if (ruleId != null) {
            APILocator.getRulesAPI().deleteRule(
                    APILocator.getRulesAPI().getRuleById(ruleId, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false);
            ruleId = null;
        }
    }

}

package com.dotmarketing.portlets.rules.business;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.junit.After;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.util.GeoIp2CityDbUtil;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.actionlet.CountRequestsActionlet;
import com.dotmarketing.portlets.rules.actionlet.ThrowErrorActionlet;
import com.dotmarketing.portlets.rules.conditionlet.*;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;

import static com.dotcms.repackage.org.junit.Assert.*;

public class RulesAPITest extends TestBase {

	private HttpServletRequest request;
    private String serverName;
    private Integer serverPort;
    String ruleId;
    
	public RulesAPITest(){		
		request = ServletTestRunner.localRequest.get();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        ruleId = "";
	}
	
	
	@Test
	public void testFireOnEveryRequest() throws Exception {

		createRule(Rule.FireOn.EVERY_REQUEST);

		makeRequest(
				"http://" + serverName + ":" + serverPort + "/robots.txt?t=" + System.currentTimeMillis());
		Integer count = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_REQUEST.name());

		makeRequest(
				"http://" + serverName + ":" + serverPort + "/robots.txt?t=" + System.currentTimeMillis());
		Integer newCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_REQUEST.name());

		assertTrue(newCount > count);

	}

	@Test
	public void testFireOnEveryPage() throws Exception {

		createRule(Rule.FireOn.EVERY_PAGE);
		makeRequest("http://" + serverName + ":" + serverPort);
		Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

		makeRequest(
				"http://" + serverName + ":" + serverPort + "/html/images/star_on.gif?t=" + System.currentTimeMillis());
		Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

		assertEquals(firstCount, secondCount);

		makeRequest("http://" + serverName + ":" + serverPort);
		Integer thirdCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

		assertTrue(thirdCount > secondCount);
	}

	@Test
	public void testFireOnOncePerVisit() throws Exception {

		createRule(Rule.FireOn.ONCE_PER_VISIT);

		URLConnection conn = makeRequest("http://" + serverName + ":" + serverPort);

		String oncePerVisitCookie = getCookie(conn, com.dotmarketing.util.WebKeys.ONCE_PER_VISIT_COOKIE);

		Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISIT.name());

		makeRequest("http://" + serverName + ":" + serverPort, oncePerVisitCookie);
		Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISIT.name());

		assertEquals(firstCount, secondCount);

	}

	@Test
	public void testFireOnOncePerVisitor() throws Exception {

		createRule(Rule.FireOn.ONCE_PER_VISITOR);

		URLConnection conn = makeRequest("http://" + serverName + ":" + serverPort);

		String longLivedCookie = getCookie(conn, com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

		Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISITOR.name());

		makeRequest("http://" + serverName + ":" + serverPort, longLivedCookie);
		Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISITOR.name());

		assertEquals(firstCount, secondCount);

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

	private URLConnection makeRequest(String urlStr) throws IOException {
		return makeRequest(urlStr, null);
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

	private void createRule(Rule.FireOn fireOn) throws Exception {
		RulesAPI rulesAPI = APILocator.getRulesAPI();
        rulesAPI.addConditionlet(MockTrueConditionlet.class);

		User user = APILocator.getUserAPI().getSystemUser();

		HostAPI hostAPI = APILocator.getHostAPI();

		// Setting the test user

		Host defaultHost = hostAPI.findDefaultHost(user, false);

		// Create Rule
		Rule rule = new Rule();
		rule.setName(fireOn.name() + "Rule");
		rule.setHost(defaultHost.getIdentifier());
		rule.setEnabled(true);
		rule.setFireOn(fireOn);

		rulesAPI.saveRule(rule, user, false);
		
		ruleId = rule.getId();
		
		ConditionGroup group = new ConditionGroup();
		group.setRuleId(rule.getId());
		group.setOperator(Condition.Operator.AND);

		rulesAPI.saveConditionGroup(group, user, false);

		Condition condition = new Condition();
		condition.setName("testCondition");
		condition.setConditionGroup(group.getId());
		condition.setConditionletId(MockTrueConditionlet.class.getSimpleName());
		condition.setOperator(Condition.Operator.AND);
		condition.setComparison("is");

		rulesAPI.saveCondition(condition, user, false);

		RuleAction action = new RuleAction();
		action.setActionlet(CountRequestsActionlet.class.getSimpleName());
		action.setRuleId(rule.getId());
		action.setName(fireOn.getCamelCaseName() + "Actionlet");

		RuleActionParameter fireOnParam = new RuleActionParameter();
		fireOnParam.setRuleActionId(action.getId());
		fireOnParam.setKey("fireOn");
		fireOnParam.setValue(fireOn.name());

		List<RuleActionParameter> params = new ArrayList<>();
		params.add(fireOnParam);

		action.setParameters(params);

		rulesAPI.saveRuleAction(action, user, false);

	}

	@Test
	public void testRefreshConditionletsMapNoExceptionWhenErrorInCustomConditionlet() {
		RulesAPI rulesAPI = new RulesAPIImpl();
		// addConditionlet calls refreshConditionletsMap under the cover
		// shouldn't throw error
		rulesAPI.addConditionlet(ThrowErrorConditionlet.class);
	}

	@Test
	public void testRefreshActionletsMapNoExceptionWhenErrorInCustomActionlet() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		RulesAPI rulesAPI = new RulesAPIImpl();
		// addRuleActionlet calls refreshActionletsMap under the cover
		// shouldn't throw error
		rulesAPI.addRuleActionlet(ThrowErrorActionlet.class);
	}

	@After
    public void deleteRule() throws DotDataException, DotSecurityException {
        if (ruleId != null) {
            APILocator.getRulesAPI().deleteRule(
                    APILocator.getRulesAPI().getRuleById(ruleId, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false);
        }
    }


}




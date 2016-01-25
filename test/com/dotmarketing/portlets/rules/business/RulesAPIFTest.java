package com.dotmarketing.portlets.rules.business;

import com.dotcms.enterprise.rules.RulesAPI;
import com.dotcms.enterprise.rules.RulesAPIImpl;
import com.dotmarketing.portlets.rules.actionlet.ThrowErrorActionlet;
import com.dotmarketing.portlets.rules.conditionlet.ThrowErrorConditionlet;
import com.dotmarketing.portlets.rules.model.ParameterModel;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.dotcms.TestBase;
import com.dotcms.repackage.org.junit.After;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.actionlet.CountRequestsActionlet;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.liferay.portal.model.User;

import static com.dotcms.repackage.org.junit.Assert.*;

public class RulesAPIFTest extends TestBase {

	private HttpServletRequest request;
    private String ruleId;
	private final String robotsTxtUrl;
	private final String indexUrl;

	public RulesAPIFTest(){
		request = ServletTestRunner.localRequest.get();
		String serverName = request.getServerName();
		int serverPort = request.getServerPort();
		robotsTxtUrl = String.format("http://%s:%s/robots.txt?t=", serverName, serverPort);
		indexUrl = String.format("http://%s:%s/", serverName, serverPort);
        ruleId = "";
	}
	
	
	@Test
	public void testFireOnEveryRequest() throws Exception {

		createRule(Rule.FireOn.EVERY_REQUEST);

		makeRequest(robotsTxtUrl + System.currentTimeMillis());
		Integer count = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_REQUEST.name());

		makeRequest(robotsTxtUrl + System.currentTimeMillis());
		Integer newCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_REQUEST.name());

		assertTrue(newCount > count);

	}

	@Test
	public void testFireOnEveryPage() throws Exception {

		createRule(Rule.FireOn.EVERY_PAGE);
		makeRequest(indexUrl);
		Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

		makeRequest(robotsTxtUrl + System.currentTimeMillis());
		Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

		assertEquals(firstCount, secondCount);

		makeRequest(indexUrl);
		Integer thirdCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

		assertTrue(thirdCount > secondCount);
	}

	@Test
	public void testFireOnOncePerVisit() throws Exception {

		createRule(Rule.FireOn.ONCE_PER_VISIT);

		URLConnection conn = makeRequest(indexUrl);

		String oncePerVisitCookie = getCookie(conn, com.dotmarketing.util.WebKeys.ONCE_PER_VISIT_COOKIE);

		Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISIT.name());

		makeRequest(indexUrl, oncePerVisitCookie);
		Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISIT.name());

		assertEquals(firstCount, secondCount);

	}

	@Test
	public void testFireOnOncePerVisitor() throws Exception {

		createRule(Rule.FireOn.ONCE_PER_VISITOR);

		URLConnection conn = makeRequest(indexUrl);

		String longLivedCookie = getCookie(conn, com.dotmarketing.util.WebKeys.LONG_LIVED_DOTCMS_ID_COOKIE);

		Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISITOR.name());

		makeRequest(indexUrl, longLivedCookie);
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

		User user = APILocator.getUserAPI().getSystemUser();

		HostAPI hostAPI = APILocator.getHostAPI();

		// Setting the test user

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
		action.setActionlet(CountRequestsActionlet.class.getSimpleName());
		action.setRuleId(rule.getId());
		action.setName(fireOn.getCamelCaseName() + "Actionlet");

		ParameterModel fireOnParam = new ParameterModel();
		fireOnParam.setOwnerId(action.getId());
		fireOnParam.setKey("fireOn");
		fireOnParam.setValue(fireOn.name());

		List<ParameterModel> params = new ArrayList<>();
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




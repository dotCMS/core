package com.dotmarketing.portlets.rules.business;

import com.dotcms.TestBase;
import com.dotcms.repackage.com.sun.jersey.api.client.Client;
import com.dotcms.repackage.com.sun.jersey.api.client.ClientResponse;
import com.dotcms.repackage.com.sun.jersey.api.client.WebResource;
import com.dotcms.repackage.com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.apache.commons.io.IOUtils;
import com.dotcms.repackage.org.apache.http.client.methods.HttpGet;
import com.dotcms.repackage.org.junit.After;
import com.dotcms.repackage.org.junit.AfterClass;
import com.dotcms.repackage.org.junit.BeforeClass;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.actionlet.CountRequestsActionlet;
import com.dotmarketing.portlets.rules.conditionlet.MockTrueConditionlet;
import com.dotmarketing.portlets.rules.model.*;
import com.dotmarketing.portlets.rules.conditionlet.UsersCountryConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.org.junit.Assert.*;

import com.dotcms.repackage.org.mockito.Mockito;
import com.liferay.util.Http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class RulesAPITest extends TestBase {

    private String ruleId;
    HttpServletRequest request;
    String serverName;
    Integer serverPort;

    @BeforeClass
    private void init() {
        request = ServletTestRunner.localRequest.get();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
    }

    @Test
    public void testCRUD() throws Exception {

        HttpServletRequest request = ServletTestRunner.localRequest.get();
        String serverName = request.getServerName();
        Integer serverPort = request.getServerPort();

        HostAPI hostAPI = APILocator.getHostAPI();

        //Setting the test user
        User user = APILocator.getUserAPI().getSystemUser();
        Host defaultHost = hostAPI.findDefaultHost(user, false);

        Client client = Client.create();
        client.addFilter(new HTTPBasicAuthFilter("admin@dotcms.com", "admin"));

        WebResource resource = client.resource("http://" + serverName + ":" + serverPort + "/api/rules-engine");

        final String modifiedRuleName = "testRuleModified";
        final String modifiedConditionName = "testConditionModified";

        // Create new Rule

        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("site", defaultHost.getIdentifier());
        ruleJSON.put("name", "testRule");
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        ClientResponse response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, ruleJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        String responseStr = response.getEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        ruleId = (String) responseJSON.get("id");


        // Create new Condition Group

        JSONObject groupJSON = new JSONObject();
        groupJSON.put("ruleId", ruleId);
        groupJSON.put("operator", Condition.Operator.AND.name());

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, groupJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        String groupId = (String) responseJSON.get("id");

        // Create new Condition

        JSONObject conditionJSON = new JSONObject();
        conditionJSON.put("name", "testCondition");
        conditionJSON.put("conditionlet", UsersCountryConditionlet.class.getSimpleName());
        conditionJSON.put("comparison", "is");
        conditionJSON.put("operator", Condition.Operator.AND.name());

        JSONObject valueJSON = new JSONObject();
        valueJSON.put("value", "FR");
        valueJSON.put("priority", 0);

        JSONArray valuesJSON = new JSONArray();
        valuesJSON.add(valueJSON);

        conditionJSON.put("values", valuesJSON);

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups/" + groupId + "/conditions").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, conditionJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        String conditionId = (String) responseJSON.get("id");
        JSONObject conditionValues = (JSONObject) responseJSON.get("values");

        // Create new Rule Action

        JSONObject actionJSON = new JSONObject();
        actionJSON.put("actionlet", "TestActionlet");

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/actions").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, actionJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        String actionId = (String) responseJSON.get("id");

        // Update Rule

        ruleJSON = new JSONObject();
        ruleJSON.put("site", defaultHost.getIdentifier());
        ruleJSON.put("name", modifiedRuleName);
        ruleJSON.put("enabled", "false");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, ruleJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
        assertTrue(rule.getName().equals(modifiedRuleName));
        assertFalse(rule.isEnabled());

        // Update Condition Group

        groupJSON = new JSONObject();
        groupJSON.put("ruleId", ruleId);
        groupJSON.put("operator", Condition.Operator.OR.name());

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups/" + groupId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, groupJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        ConditionGroup group = APILocator.getRulesAPI().getConditionGroupById(groupId, user, false);
        assertTrue(group.getOperator() == Condition.Operator.OR);

        // Update Condition

        conditionJSON = new JSONObject();
        conditionJSON.put("name", modifiedConditionName);
        conditionJSON.put("ruleId", ruleId);
        conditionJSON.put("conditionGroupId", groupId);
        conditionJSON.put("conditionlet", UsersCountryConditionlet.class.getSimpleName());
        conditionJSON.put("comparison", "is");
        conditionJSON.put("operator", Condition.Operator.OR.name());

        valueJSON = new JSONObject();
        String valueId = (String) conditionValues.keys().next();
        valueJSON.put("id", valueId);
        valueJSON.put("value", "VE");
        valueJSON.put("priority", 0);

        valuesJSON = new JSONArray();
        valuesJSON.add(valueJSON);

        conditionJSON.put("values", valuesJSON);

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups/" + groupId + "/conditions/" + conditionId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, conditionJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        Condition condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
        assertTrue(condition.getName().equals(modifiedConditionName));
        assertTrue(condition.getOperator() == Condition.Operator.OR);
        assertTrue(condition.getValues().get(0).getValue().equals("VE"));

        // Update Rule Action

        actionJSON = new JSONObject();
        actionJSON.put("actionlet", "TestActionlet");
        actionJSON.put("ruleId", ruleId);
        actionJSON.put("priority", 10);

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/actions/" + actionId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, actionJSON.toString());

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        RuleAction action = APILocator.getRulesAPI().getRuleActionById(actionId, user, false);
        assertTrue(action.getPriority() == 10);

        // Get Rules

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        ruleJSON = (JSONObject) responseJSON.get(ruleId);
        assertTrue(ruleJSON.getString("name").equals(modifiedRuleName));

        // Get Rule

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        ruleJSON = new JSONObject(responseStr);
        assertTrue(ruleJSON.getString("name").equals(modifiedRuleName));

        // Get Conditions

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups/" + groupId + "/conditions").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        JSONObject conditionsJSON = new JSONObject(responseStr);
        conditionJSON = (JSONObject) conditionsJSON.get(conditionId);
        assertTrue(conditionJSON.getString("name").equals(modifiedConditionName));

        // Get Condition

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups/" + groupId + "/conditions/" + conditionId).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        conditionJSON = new JSONObject(responseStr);
        assertTrue(conditionJSON.getString("name").equals(modifiedConditionName));

        // Get Conditionlets

        response = resource.path("/conditionlets").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

        responseStr = response.getEntity(String.class);
        JSONObject conditionletJSON = new JSONObject(responseStr);

        assertTrue(conditionletJSON.getString(UsersCountryConditionlet.class.getSimpleName()) != null);


        // Delete Condition

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups/" + groupId + "/conditions/" + conditionId).type(MediaType.APPLICATION_JSON_TYPE).delete(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_NO_CONTENT);

        condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
        assertNull(condition);

        // Delete Condition Group

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/conditiongroups/" + groupId).type(MediaType.APPLICATION_JSON_TYPE).delete(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_NO_CONTENT);

        group = APILocator.getRulesAPI().getConditionGroupById(conditionId, user, false);
        assertNull(group);

        // Delete Rule Action

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId + "/actions/" + actionId).type(MediaType.APPLICATION_JSON_TYPE).delete(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_NO_CONTENT);

        action = APILocator.getRulesAPI().getRuleActionById(actionId, user, false);
        assertNull(action);

        // Delete Rule

        response = resource.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleId).type(MediaType.APPLICATION_JSON_TYPE).delete(ClientResponse.class);

        assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_NO_CONTENT);

        rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
        assertNull(rule);

    }

    @Test
    public void testFireOnEveryRequest() throws Exception {

        createRule(Rule.FireOn.EVERY_REQUEST);


        makeRequest("http://" + serverName + ":" + serverPort + "/html/images/star_on.gif?t=" + System.currentTimeMillis());
        Integer count = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_REQUEST.name());

        makeRequest("http://" + serverName + ":" + serverPort + "/html/images/star_on.gif?t=" + System.currentTimeMillis());
        Integer newCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_REQUEST.name());

        assertTrue(newCount>count);

    }

    @Test
    public void testFireOnEveryPage() throws Exception {

        createRule(Rule.FireOn.EVERY_PAGE);
        makeRequest("http://" + serverName + ":" + serverPort);
        Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

        makeRequest("http://" + serverName + ":" + serverPort + "/html/images/star_on.gif?t=" + System.currentTimeMillis());
        Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

        assertEquals(firstCount,secondCount);

        makeRequest("http://" + serverName + ":" + serverPort);
        Integer thirdCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.EVERY_PAGE.name());

        assertTrue(thirdCount>secondCount);
    }

    @Test
    public void testFireOnOncePerVisit() throws Exception {

        createRule(Rule.FireOn.ONCE_PER_VISIT);

        makeRequest("http://" + serverName + ":" + serverPort);
        Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISIT.name());

        makeRequest("http://" + serverName + ":" + serverPort);
        Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISIT.name());

        assertEquals(firstCount, secondCount);

    }

    @Test
    public void testFireOnOncePerVisitor() throws Exception {

        createRule(Rule.FireOn.ONCE_PER_VISITOR);

        makeRequest("http://" + serverName + ":" + serverPort);
        Integer firstCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISITOR.name());

        makeRequest("http://" + serverName + ":" + serverPort);
        Integer secondCount = (Integer) request.getServletContext().getAttribute(Rule.FireOn.ONCE_PER_VISITOR.name());

        assertEquals(firstCount, secondCount);

    }

    private int makeRequest(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.connect();
        return con.getResponseCode();
    }

    private void createRule(Rule.FireOn fireOn) throws Exception {
        RulesAPI rulesAPI = APILocator.getRulesAPI();
        User user = APILocator.getUserAPI().getSystemUser();

        HostAPI hostAPI = APILocator.getHostAPI();

        //Setting the test user

        Host defaultHost = hostAPI.findDefaultHost(user, false);

        // Create Rule
        Rule rule = rule = new Rule();
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

        Condition condition =new Condition();
        condition.setName("testCondition");
        condition.setRuleId(rule.getId());
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

    @After
    public void deleteRule() throws DotDataException, DotSecurityException {
        if (ruleId != null) {
            APILocator.getRulesAPI().deleteRule(
                    APILocator.getRulesAPI().getRuleById(ruleId, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false);
        }
    }


}

package com.dotmarketing.portlets.rules.business;

import com.dotcms.TestBase;
import com.dotcms.repackage.com.sun.jersey.api.client.Client;
import com.dotcms.repackage.com.sun.jersey.api.client.ClientResponse;
import com.dotcms.repackage.com.sun.jersey.api.client.WebResource;
import com.dotcms.repackage.com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.junit.AfterClass;
import com.dotcms.repackage.org.junit.Test;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.portlets.rules.conditionlet.VisitorsCountryConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.ConditionGroup;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.portlets.rules.model.RuleAction;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.dotmarketing.util.json.JSONArray;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

import static com.dotcms.repackage.org.junit.Assert.assertFalse;
import static com.dotcms.repackage.org.junit.Assert.assertTrue;

public class RulesAPITest extends TestBase {

    private static String ruleId;

    @Test
    public void testRule() throws Exception {

        try {
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

            ClientResponse response = resource.path("/rules").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, ruleJSON.toString());

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            String responseStr = response.getEntity(String.class);
            JSONObject responseJSON = new JSONObject(responseStr);
            ruleId = (String) responseJSON.get("id");


            // Create new Condition Group

            JSONObject groupJSON = new JSONObject();
            groupJSON.put("ruleId", ruleId);
            groupJSON.put("operator", Condition.Operator.AND.name());

            response = resource.path("/rules/" + ruleId + "/conditiongroups").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, groupJSON.toString());

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            responseStr = response.getEntity(String.class);
            responseJSON = new JSONObject(responseStr);
            String groupId = (String) responseJSON.get("id");

            // Create new Condition

            JSONObject conditionJSON = new JSONObject();
            conditionJSON.put("name", "testCondition");
            conditionJSON.put("conditionlet", VisitorsCountryConditionlet.class.getSimpleName());
            conditionJSON.put("comparison", "is");
            conditionJSON.put("operator", Condition.Operator.AND.name());

            JSONObject valueJSON = new JSONObject();
            valueJSON.put("value", "FR");
            valueJSON.put("priority", 0);

            JSONArray valuesJSON = new JSONArray();
            valuesJSON.add(valueJSON);

            conditionJSON.put("values", valuesJSON);

            response = resource.path("/rules/conditiongroups/" + groupId + "/conditions").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, conditionJSON.toString());

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            responseStr = response.getEntity(String.class);
            responseJSON = new JSONObject(responseStr);
            String conditionId = (String) responseJSON.get("id");
            JSONObject conditionValues = (JSONObject) responseJSON.get("values");

            // Create new Rule Action

            JSONObject actionJSON = new JSONObject();
            actionJSON.put("actionlet", "TestActionlet");

            response = resource.path("/rules/" + ruleId + "/actions").type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, actionJSON.toString());

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

            response = resource.path("/rules/" + ruleId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, ruleJSON.toString());

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
            assertTrue(rule.getName().equals(modifiedRuleName));
            assertFalse(rule.isEnabled());

            // Update Condition Group

            groupJSON = new JSONObject();
            groupJSON.put("ruleId", ruleId);
            groupJSON.put("operator", Condition.Operator.OR.name());

            response = resource.path("/rules/conditiongroups/" + groupId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, groupJSON.toString());

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            ConditionGroup group = APILocator.getRulesAPI().getConditionGroupById(groupId, user, false);
            assertTrue(group.getOperator() == Condition.Operator.OR);

            // Update Condition

            conditionJSON = new JSONObject();
            conditionJSON.put("name", modifiedConditionName);
            conditionJSON.put("ruleId", ruleId);
            conditionJSON.put("conditionGroupId", groupId);
            conditionJSON.put("conditionlet", VisitorsCountryConditionlet.class.getSimpleName());
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

            response = resource.path("/rules/conditiongroups/conditions/" + conditionId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, conditionJSON.toString());

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

            response = resource.path("/rules/actions/"+actionId).type(MediaType.APPLICATION_JSON_TYPE).put(ClientResponse.class, actionJSON.toString());

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            RuleAction action = APILocator.getRulesAPI().getRuleActionById(actionId, user, false);
            assertTrue(action.getPriority()==10);

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

            response = resource.path("/rules/" + ruleId + "/conditions").type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            responseStr = response.getEntity(String.class);
            JSONObject groupsJSON = new JSONObject(responseStr);
            groupJSON = (JSONObject) groupsJSON.get(groupId);
            assertTrue(groupJSON.getString("operator").equals(Condition.Operator.OR.name()));

            conditionJSON = (JSONObject) groupJSON.get(conditionId);
            assertTrue(conditionJSON.getString("name").equals(modifiedConditionName));

            // Get Condition

            response = resource.path("/rules/" + ruleId + "/conditions/" + conditionId).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            responseStr = response.getEntity(String.class);
            conditionJSON = new JSONObject(responseStr);
            assertTrue(conditionJSON.getString("name").equals(modifiedConditionName));

            // Delete Condition

            response = resource.path("/rules/conditiongroups/conditions/" + conditionId).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
            assertTrue(condition == null);

            // Delete Condition Group

            response = resource.path("/rules/conditiongroups/" + groupId).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            group = APILocator.getRulesAPI().getConditionGroupById(conditionId, user, false);
            assertTrue(group == null);

            // Delete Rule Action

            response = resource.path("/rules/" + ruleId + "/actions/" + actionId).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            action = APILocator.getRulesAPI().getRuleActionById(actionId, user, false);
            assertTrue(action == null);

            // Delete Rule

            response = resource.path("/rules/" + ruleId).type(MediaType.APPLICATION_JSON_TYPE).get(ClientResponse.class);

            assertTrue(response.getClientResponseStatus().getStatusCode() == HttpStatus.SC_OK);

            rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
            assertTrue(rule == null);

        } catch (Exception e) {
            deleteRule();

            throw e;
        }

    }

    @AfterClass
    public static void deleteRule() throws DotSecurityException, DotDataException {

        if(ruleId!=null) {
            APILocator.getRulesAPI().deleteRule(
                    APILocator.getRulesAPI().getRuleById(ruleId, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false);
        }

    }

}

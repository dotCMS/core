package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.TestBase;
import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import com.dotcms.repackage.org.junit.After;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.rest.RestClientBuilder;
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
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;
import com.liferay.portal.model.User;

import javax.servlet.http.HttpServletRequest;

import static com.dotcms.repackage.org.junit.Assert.*;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

public class RuleActionsRestAPITest extends TestBase {

    private HttpServletRequest request;
    private String serverName;
    private Integer serverPort;
    private User user;
    Host defaultHost;
    Client client;

    public RuleActionsRestAPITest() {
        request = ServletTestRunner.localRequest.get();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        HostAPI hostAPI = APILocator.getHostAPI();

        //Setting the test user
        try{
	        user = APILocator.getUserAPI().getSystemUser();
	        defaultHost = hostAPI.findDefaultHost(user, false);
        }catch(DotDataException dd){
        	dd.printStackTrace();
        }catch(DotSecurityException ds){
        	ds.printStackTrace();
        }
        
        client = RestClientBuilder.newClient();
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("admin@dotcms.com", "admin");
        client.register(feature);

    }

    /**
     * Used to create as many rules as needed for testing, based on simple rule creation
     * @param ruleID
     * @return
     * @throws JSONException
     */
    private String createRule(String ruleID) throws JSONException{
    	//setup
    	JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", ruleID);
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = client.target("http://" + serverName + ":" + serverPort + "/api/v1");
        
        // create
        Response response = target.path("/sites/" + defaultHost.getIdentifier() + "/rules").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(ruleJSON.toString()));
        
        // response
        String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String rule = (String) responseJSON.get("id");
    	
    	return rule;
    }
    
    /**
     * Used to delete as many rules as needed for testing
     * @param ruleID
     * @return
     * @throws JSONException
     */
    private void deleteRule(String ruleID){
        WebTarget target = client.target("http://" + serverName + ":" + serverPort + "/api/v1");
    	Response response = target.path("/sites/" + defaultHost.getIdentifier() + "/rules/" + ruleID).request(MediaType.APPLICATION_JSON_TYPE).delete();
    }    
    
    @Test
    public void testRuleAction() throws JSONException{
    	// setup
    	String rule = createRule("ruleActionRule");
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "myTestRuleAction");
    	actionJSON.put("actionlet", "TestActionlet");
    	actionJSON.put("owningRule", rule);

    	WebTarget target = client.target("http://" + serverName + ":" + serverPort + "/api/v1");
    	Response response = target.path("/sites/" + defaultHost.getIdentifier() + "/ruleengine/ruleActions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(actionJSON.toString()));

    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String actionId = (String) responseJSON.get("id");
    	
    	response = target.path("/sites/" + defaultHost.getIdentifier() + "/ruleengine/ruleActions/" + actionId).request(MediaType.APPLICATION_JSON_TYPE).delete();
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
    	
    	deleteRule(rule);
    }
    
    @Test
    public void testRuleActionUpdate() throws JSONException{
    	// set up
    	String rule = createRule("ruleActionRuleUpdate");
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "myTestRuleActionUpdate");
    	actionJSON.put("actionlet", "TestActionletUpdate");
    	actionJSON.put("owningRule", rule);

    	// create
    	WebTarget target = client.target("http://" + serverName + ":" + serverPort + "/api/v1");
    	Response response = target.path("/sites/" + defaultHost.getIdentifier() + "/ruleengine/ruleActions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(actionJSON.toString()));
    	
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String actionId = (String) responseJSON.get("id");
    	
    	//update
    	
    	actionJSON = new JSONObject();
		actionJSON.put("actionlet", "TestActionletUpdate");
		actionJSON.put("name", "My Updated Action");
		actionJSON.put("owningRule", rule);
		actionJSON.put("priority", 10);
		
		response = target.path("/sites/" + defaultHost.getIdentifier() + "/ruleengine/ruleActions/" + actionId).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(actionJSON.toString()));
		
		// update done
		assertTrue(response.getStatus() == HttpStatus.SC_OK);
		
		// get to review updated value		
		response = target.path("/sites/" + defaultHost.getIdentifier() + "/ruleengine/ruleActions/" + actionId).request(MediaType.APPLICATION_JSON_TYPE).get();
		assertTrue(response.getStatus() == HttpStatus.SC_OK);

        responseStr = response.readEntity(String.class);
        actionJSON = new JSONObject(responseStr);
        assertTrue(actionJSON.getInt("priority") == 10);
        
        // delete (clean up)
        response = target.path("/sites/" + defaultHost.getIdentifier() + "/ruleengine/ruleActions/" + actionId).request(MediaType.APPLICATION_JSON_TYPE).delete();
        deleteRule(rule);
				
    }
    
    
//    @Test
//    public void testCRUD() throws Exception {
//
//    	String defaultHostIdentifier = defaultHost.getIdentifier();
//
//        Client client = RestClientBuilder.newClient();
//        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("admin@dotcms.com", "admin");
//        client.register(feature);
//
//        WebTarget target = client.target("http://" + serverName + ":" + serverPort + "/api/v1");
//
//        final String modifiedRuleName = "testRuleModified";
//        final String modifiedConditionName = "testConditionModified";
//
//        // Create new Rule
//
//        Response response = null;
//    	try{
//    		response = createRule("testRule");
//    	}catch(JSONException je){
//    		je.printStackTrace();
//    	}
//        String responseStr = response.readEntity(String.class);
//        JSONObject responseJSON = new JSONObject(responseStr);
//        ruleId = (String) responseJSON.get("id");
//
//
//        // Create new Condition Group
//
//        JSONObject groupJSON = new JSONObject();
//        groupJSON.put("operator", Condition.Operator.AND.name());
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/rules/" + ruleId + "/conditionGroups").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(groupJSON.toString()));
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        responseJSON = new JSONObject(responseStr);
//        String groupId = (String) responseJSON.get("id");
//
//        // Create new Condition
//
//        JSONObject conditionJSON = new JSONObject();
//        conditionJSON.put("name", "testCondition");
//        conditionJSON.put("conditionlet", UsersCountryConditionlet.class.getSimpleName());
//        conditionJSON.put("comparison", "is");
//        conditionJSON.put("operator", Condition.Operator.AND.name());
//        conditionJSON.put("owningGroup", groupId);
//
//        JSONObject valueJSON = new JSONObject();
//        valueJSON.put("value", "FR");
//        valueJSON.put("priority", 0);
//
//        JSONObject valuesJSON = new JSONObject();
//        valuesJSON.put("123", valueJSON);
//
//        conditionJSON.put("values", valuesJSON);
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/conditions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(conditionJSON.toString()));
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        responseJSON = new JSONObject(responseStr);
//        String conditionId = (String) responseJSON.get("id");
//
//        // Create new Rule Action
//
//        JSONObject actionJSON = new JSONObject();
//        actionJSON.put("name", "myTestRuleAction");
//        actionJSON.put("actionlet", "TestActionlet");
//        actionJSON.put("owningRule", ruleId);
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/ruleActions").request(MediaType.APPLICATION_JSON_TYPE).post(Entity.json(actionJSON.toString()));
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        responseJSON = new JSONObject(responseStr);
//        String actionId = (String) responseJSON.get("id");
//
//        // Update Rule
//
//        JSONObject ruleJSON = new JSONObject();
//
//        ruleJSON = new JSONObject();
//        ruleJSON.put("name", modifiedRuleName);
//        ruleJSON.put("enabled", "false");
//        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/rules/" + ruleId).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(ruleJSON.toString()));
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        Rule rule = APILocator.getRulesAPI().getRuleById(ruleId, user, false);
//        assertTrue(rule.getName().equals(modifiedRuleName));
//        assertFalse(rule.isEnabled());
//
//        // Update Condition Group
//
//        groupJSON = new JSONObject();
//        groupJSON.put("operator", Condition.Operator.OR.name());
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/rules/" + ruleId + "/conditionGroups/" + groupId).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(groupJSON.toString()));
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        ConditionGroup group = APILocator.getRulesAPI().getConditionGroupById(groupId, user, false);
//        assertTrue(group.getOperator() == Condition.Operator.OR);
//
//        // Update Condition
//
//        conditionJSON = new JSONObject();
//        conditionJSON.put("name", modifiedConditionName);
//        conditionJSON.put("owningGroup", groupId);
//        conditionJSON.put("conditionlet", UsersCountryConditionlet.class.getSimpleName());
//        conditionJSON.put("comparison", "is");
//        conditionJSON.put("operator", Condition.Operator.OR.name());
//
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/conditions/" + conditionId + "/conditionValues" ).request(MediaType.APPLICATION_JSON_TYPE).get();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        responseJSON = new JSONObject(responseStr);
//        String valueId = (String) responseJSON.keys().next();
//
//        valueJSON = new JSONObject();
//        valueJSON.put("id", valueId);
//        valueJSON.put("value", "VE");
//        valueJSON.put("priority", 0);
//
//        valuesJSON = new JSONObject();
//        valuesJSON.put("123", valueJSON);
//
//        conditionJSON.put("values", valuesJSON);
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/conditions/" + conditionId).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(conditionJSON.toString()));
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        Condition condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
//        assertTrue(condition.getName().equals(modifiedConditionName));
//        assertTrue(condition.getOperator() == Condition.Operator.OR);
//        assertTrue(condition.getValues().get(0).getValue().equals("VE"));
//
//        // Update Rule Action
//
//        actionJSON = new JSONObject();
//        actionJSON.put("actionlet", "TestActionlet");
//        actionJSON.put("name", "My Updated Action");
//        actionJSON.put("owningRule", ruleId);
//        actionJSON.put("priority", 10);
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/ruleActions/" + actionId).request(MediaType.APPLICATION_JSON_TYPE).put(Entity.json(actionJSON.toString()));
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        RuleAction action = APILocator.getRulesAPI().getRuleActionById(actionId, user, false);
//        assertTrue(action.getPriority() == 10);
//
//        // Get Rules
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/rules").request(MediaType.APPLICATION_JSON_TYPE).get();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        responseJSON = new JSONObject(responseStr);
//        ruleJSON = (JSONObject) responseJSON.get(ruleId);
//        assertTrue(ruleJSON.getString("name").equals(modifiedRuleName));
//
//        // Get Rule
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/rules/" + ruleId).request(MediaType.APPLICATION_JSON_TYPE).get();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        ruleJSON = new JSONObject(responseStr);
//        assertTrue(ruleJSON.getString("name").equals(modifiedRuleName));
//
//        // Get Condition
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/conditions/" + conditionId).request(MediaType.APPLICATION_JSON_TYPE).get();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        conditionJSON = new JSONObject(responseStr);
//        assertTrue(conditionJSON.getString("name").equals(modifiedConditionName));
//
//        // Get Conditionlets
//
//        response = target.path("/system/conditionlets").request(MediaType.APPLICATION_JSON_TYPE).get();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_OK);
//
//        responseStr = response.readEntity(String.class);
//        JSONObject conditionletJSON = new JSONObject(responseStr);
//
//        assertTrue(conditionletJSON.getString(UsersCountryConditionlet.class.getSimpleName()) != null);
//
//        // Delete Condition Value
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/conditions/" + conditionId + "/conditionValues/" + valueId).request(MediaType.APPLICATION_JSON_TYPE).delete();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//        ConditionValue conditionValue = APILocator.getRulesAPI().getConditionValueById(valueId, user, false);
//        assertNull(conditionValue);
//
//        // Delete Condition
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/conditions/" + conditionId).request(MediaType.APPLICATION_JSON_TYPE).delete();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//        condition = APILocator.getRulesAPI().getConditionById(conditionId, user, false);
//        assertNull(condition);
//
//        // Delete Condition Group
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/rules/" + ruleId + "/conditionGroups/" + groupId).request(MediaType.APPLICATION_JSON_TYPE).delete();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//        group = APILocator.getRulesAPI().getConditionGroupById(conditionId, user, false);
//        assertNull(group);
//
//        // Delete Rule Action
//
//
//        response = target.path("/sites/" + defaultHostIdentifier + "/ruleengine/ruleActions/" + actionId).request(MediaType.APPLICATION_JSON_TYPE).delete();
//
//        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
//
//        action = APILocator.getRulesAPI().getRuleActionById(actionId, user, false);
//        assertNull(action);
//
//        // Delete Rule
//        
//        deleteRule(ruleId);
//    }


   // @After
   // public void deleteRule() throws DotDataException, DotSecurityException {
   //     if (ruleId != null) {
   //         APILocator.getRulesAPI().deleteRule(
   //                 APILocator.getRulesAPI().getRuleById(ruleId, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false);
   //     }
   // }


}

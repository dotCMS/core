package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.TestBase;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import static com.dotcms.repackage.org.junit.Assert.assertTrue;

public class ActionResourceFTest extends TestBase {

    private final FunctionalTestConfig config;
    private final String actionletEndpointUrl;

    public ActionResourceFTest() {
        config = new FunctionalTestConfig();
        actionletEndpointUrl = "/sites/" + config.defaultHost.getIdentifier() + "/ruleengine/actions";
    }

    /**
     * Used to create as many rules as needed for testing, based on simple rule creation
     */
    private String createRule(String ruleName) throws JSONException {
        //setup
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", ruleName);
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ruleJSON.toString()));

        // response
        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule = (String)responseJSON.get("id");

        return rule;
    }

    /**
     * Used to delete as many rules as needed for testing
     */
    private void deleteRule(String ruleID) {
        WebTarget target = config.restBaseTarget();
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + ruleID)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();
    }
    
    
    /**
     * Save Action with required the parameters... return 200
     */
    @Test
    public void saveActionReq() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Save Action Req Parameters");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Action Test REST");
    	actionJSON.put("owningRule", ruleId);
		actionJSON.put("actionlet", "CountRequestsActionlet");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");
    	
    }
    
    /**
     * Save Action with bad parameter Name empty... return 400
     */
    @Test
    public void saveActionNameEmpty() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Save Action Bad Parameter Name");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "");
    	actionJSON.put("owningRule", ruleId);
    	actionJSON.put("actionlet", "CountRequestsActionlet");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    	
    }
    
    /**
     * Save Action with bad parameter Owning Rule non existent... return 400
     */
    @Test
    public void saveActionRuleNonExistent() throws JSONException{
    	//Creation of the Rule
    	String ruleId = "00000000-0000-0000-0000-000000000000";
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Test Action Rest");
    	actionJSON.put("owningRule", ruleId);
    	actionJSON.put("actionlet", "CountRequestsActionlet");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    	
    }

    /**
     * Save Action with bad parameter Actionlet non existent... return 400
     */
    @Test
    public void saveActionActionletNonExistent() throws JSONException {
        //Creation of the Rule
        String ruleId = createRule("Save Action Actionlet Non Existent");

        //Setup
        JSONObject actionJSON = new JSONObject();
        actionJSON.put("owningRule", ruleId);
        actionJSON.put("actionlet", "something");

        // client call
        WebTarget target = config.restBaseTarget();

        //create
        Response response = target
            .path(actionletEndpointUrl)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(actionJSON.toString()));

        assertTrue(String.format("Server should not be able to save a rule that doesn't exist yet (push requires id field). "
                                 + "Response code should be 400, but was %s.", response.getStatus()),
                   response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    }
    
    /**
     * Save Action with missing parameter Owning Rule... return 400
     */
    @Test
    public void saveActionMissingParameter() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Save Action Parameter Missing");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Test Action Rest");
    	actionJSON.put("actionlet", "something");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    	
    }
    
    /**
     * Get Action... return 200
     */
    @Test
    public void getAction() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Get Action");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Action Test REST");
    	actionJSON.put("owningRule", ruleId);
		actionJSON.put("actionlet", "CountRequestsActionlet");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");
    	
    	//get
    	response = target.path("/sites/"+ config.defaultHost.getIdentifier() + "/ruleengine/actions/"+action).request(MediaType.APPLICATION_JSON_TYPE)
    			.get();
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    }

    /**
     * Get Non Existent Action... return 404
     */
    @Test
    public void getNonExistentAction() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Get Non Existent Action");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Action Test REST");
    	actionJSON.put("owningRule", ruleId);
		actionJSON.put("actionlet", "CountRequestsActionlet");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");
    	
    	//get
    	response = target.path("/sites/"+ config.defaultHost.getIdentifier() + "/ruleengine/actions/"+"00000000-0000-0000-0000-000000000000").request(MediaType.APPLICATION_JSON_TYPE)
    			.get();
    	assertTrue(response.getStatus() == HttpStatus.SC_NOT_FOUND);
    	
    }
    
    /**
     * Save Action with all the parameters... return 200
     */
    @Test
    public void saveActionAll() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Save Action All Parameters");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Action Test REST");
    	actionJSON.put("owningRule", ruleId);
    	actionJSON.put("priority", "10");
		actionJSON.put("actionlet", "SetSessionAttributeActionlet");
		//JSON sessionKey
		JSONObject sessionKeyJSON = new JSONObject();
		sessionKeyJSON.put("key", "sessionKey");
		sessionKeyJSON.put("value", "myKey");
		//JSON sessionValue
		JSONObject sessionValueJSON = new JSONObject();
		sessionValueJSON.put("key", "sessionValue");
		sessionValueJSON.put("value", "I am from US");
		//JSON parameters
		JSONObject parametersJSON = new JSONObject();
		parametersJSON.put("sessionKey", sessionKeyJSON);
		parametersJSON.put("sessionValue", sessionValueJSON);
		actionJSON.put("parameters", parametersJSON);

    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");
    	
    }
    
    /**
     * Delete an action... return 204
     */
    @Test
    public void deleteAction() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Delete Action");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Action Test REST");
    	actionJSON.put("owningRule", ruleId);
		actionJSON.put("actionlet", "CountRequestsActionlet");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");
    	
    	//delete
    	response = target.path("/sites/" + config.defaultHostId + "/ruleengine/actions/" + action)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
    }
    
    /**
     * Delete non-existent action... return 404
     */
    @Test
    public void deleteNonExistentAction() {
        WebTarget target = config.restBaseTarget();
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/actions/" + "00000000-0000-0000-0000-000000000000")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        
        assertTrue(response.getStatus() == HttpStatus.SC_NOT_FOUND);
    }
    
    /**
     * Update an action... return 200
     */
    @Test
    public void updateAction() throws JSONException{
    	//Creation of the Rule
    	String ruleId = createRule("Update Action");
    	
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Action Test REST");
    	actionJSON.put("owningRule", ruleId);
		actionJSON.put("actionlet", "CountRequestsActionlet");
    	    	
    	// client call
    	WebTarget target = config.restBaseTarget();
    	
    	//create
    	Response response = target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));
    	
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    	
    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");
    	
        JSONObject updateJSON = new JSONObject();
        updateJSON.put("name", "Updated Name");
        updateJSON.put("owningRule", ruleId);
        updateJSON.put("actionlet", "CountRequestsActionlet");
        
        response = target.path("/sites/"+ config.defaultHost.getIdentifier() + "/ruleengine/actions/" + action)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(updateJSON.toString()));

        // test update
        assertTrue(response.getStatus() == HttpStatus.SC_OK);

    }
}

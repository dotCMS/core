package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.TestBase;
import com.dotcms.repackage.com.google.common.base.Strings;
import com.dotcms.repackage.com.google.common.collect.Lists;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.RuleDataGen;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class ActionResourceFTest extends TestBase {

    private final FunctionalTestConfig config;
    private final String actionletEndpointUrl;
	private final WebTarget target;
	private String ruleId;

	private static final String ACTIONLET="CountRulesActionlet";

	private List<Rule> rulesToRemove = Lists.newArrayList();

    public ActionResourceFTest() {
        config = new FunctionalTestConfig();
        actionletEndpointUrl = "/sites/" + config.defaultHost.getIdentifier() + "/ruleengine/actions/";
		target = config.restBaseTarget();
    }

    /**
     * Save Action with required the parameters... return 200
     */
    @Test
    public void saveValidAction() throws JSONException{
    	//Creation of the Rule
    	ruleId = createRule("Save Valid Action");
		Response response = createAction(ruleId, "MyAction");
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);

    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");
		assertFalse(Strings.isNullOrEmpty(action));

    }

    /**
     * Save Action with bad parameter Owning Rule non existent... return 400
     */
    @Test
    public void saveActionRuleNonExistent() throws JSONException{
    	String nonExistingRuleId = "00000000-0000-0000-0000-000000000000";
    	Response response = createAction(nonExistingRuleId, "MyAction");
    	assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Save Action with bad parameter Actionlet non existent... return 400
     */
    @Test
    public void saveActionNonExistingActionlet() throws JSONException {
        ruleId = createRule("Save Action Non Existing Actionlet");
        Response response = createAction(ruleId, "MyAction", "NonExistingActionlet");
        assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Save Action with missing attribute "owningRule"... return 400
     */
    @Test
    public void saveActionMissingMandatoryAttribute() throws JSONException{
    	//Setup
    	JSONObject actionJSON = new JSONObject();
    	actionJSON.put("name", "Test Action Rest");
    	actionJSON.put("actionlet", "something");

    	//create
    	Response response = target.path(actionletEndpointUrl)
				.request(MediaType.APPLICATION_JSON_TYPE)
    			.post(Entity.json(actionJSON.toString()));

    	assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Get Action... return 200
     */
    @Test
    public void getAction() throws JSONException{
    	//Creation of the Rule
    	ruleId = createRule("Get Action");
		Response response = createAction(ruleId, "MyAction");

    	assertTrue(response.getStatus() == HttpStatus.SC_OK);

    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");

    	//get
    	response = target.path(actionletEndpointUrl + action)
				.request(MediaType.APPLICATION_JSON_TYPE)
				.get();
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);
    }

    /**
     * Get Non Existent Action... return 404
     */
    @Test
    public void getNonExistentAction() throws JSONException{
    	Response response = target.path(actionletEndpointUrl + "00000000-0000-0000-0000-000000000000")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.get();
    	assertTrue(response.getStatus() == HttpStatus.SC_NOT_FOUND);

    }

    /**
     * Delete an action... return 204
     */
    @Test
    public void deleteAction() throws JSONException{
    	ruleId = createRule("Delete Action");
		Response response = createAction(ruleId, "MyAction");
    	assertTrue(response.getStatus() == HttpStatus.SC_OK);

    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");

    	//delete
    	response = target.path(actionletEndpointUrl + action)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

    	assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Delete non-existent action... return 404
     */
    @Test
    public void deleteNonExistentAction() {
        Response response = target.path(actionletEndpointUrl + "00000000-0000-0000-0000-000000000000")
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
    	ruleId = createRule("Update Action");
		Response response = createAction(ruleId, "MyAction");

    	assertTrue(response.getStatus() == HttpStatus.SC_OK);

    	//response
    	String responseStr = response.readEntity(String.class);
    	JSONObject responseJSON = new JSONObject(responseStr);
    	String action = (String)responseJSON.get("id");

        JSONObject updateJSON = new JSONObject();
        updateJSON.put("name", "Updated Name");
        updateJSON.put("owningRule", ruleId);
        updateJSON.put("actionlet", ACTIONLET);

		JSONObject parametersJSON = new JSONObject();

		JSONObject fireOnJSON = new JSONObject();
		fireOnJSON.put("key", "attribute");
		fireOnJSON.put("value", Rule.FireOn.EVERY_PAGE.toString());
		parametersJSON.put("attribute", fireOnJSON);

		updateJSON.put("parameters", parametersJSON);

        response = target.path(actionletEndpointUrl + action)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.json(updateJSON.toString()));

        // test update
        assertTrue(response.getStatus() == HttpStatus.SC_OK);

    }

	/**
	 * Used to create as many rules as needed for testing, based on simple rule creation
	 */
	private String createRule(String ruleName) throws JSONException {
		//setup
		JSONObject ruleJSON = new JSONObject();
		ruleJSON.put("name", ruleName + System.currentTimeMillis());
		ruleJSON.put("enabled", "true");
		ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

		// create
		Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
				.request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.json(ruleJSON.toString()));

		// response
		String responseStr = response.readEntity(String.class);
		JSONObject responseJSON = new JSONObject(responseStr);
		String ruleId = (String)responseJSON.get("id");

		return ruleId;
	}

	private Response createAction(String ruleId, String name) throws JSONException  {
		return createAction(ruleId, name, ACTIONLET);
	}

	private Response createAction(String ruleId, String name, String actionletName) throws JSONException  {
		List<Map<String, String>> parameters = new ArrayList<>();
		HashMap<String, String> parameter = new HashMap<>();
		parameter.put("key", "attribute");
		parameter.put("value", Rule.FireOn.EVERY_PAGE.name());
		parameters.add(parameter);
		return createAction(ruleId, name, actionletName , parameters);
	}

	private Response createAction(String ruleId, String name, String actionletName, List<Map<String, String>> parameters) throws JSONException {
		JSONObject actionJSON = new JSONObject();
		actionJSON.put("name", name);
		actionJSON.put("owningRule", ruleId);
		actionJSON.put("actionlet", actionletName);

		if(parameters!=null) {
			JSONObject parametersJSON = new JSONObject();

			for (Map<String, String> parameter : parameters) {
				JSONObject fireOnJSON = new JSONObject();
				fireOnJSON.put("key", parameter.get("key"));
				fireOnJSON.put("value", parameter.get("value"));
				parametersJSON.put("attribute", fireOnJSON);
			}

			actionJSON.put("parameters", parametersJSON);
		}

		return target.path(actionletEndpointUrl).request(MediaType.APPLICATION_JSON_TYPE)
				.post(Entity.json(actionJSON.toString()));
	}


	@After
	public void deleteRule() throws DotDataException, DotSecurityException {
		if (ruleId != null) {
			APILocator.getRulesAPI().deleteRule(
					APILocator.getRulesAPI().getRuleById(ruleId, APILocator.getUserAPI().getSystemUser(), false), APILocator.getUserAPI().getSystemUser(), false);
		}
	}
}

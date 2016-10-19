package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.TestBase;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotmarketing.portlets.rules.model.LogicalOperator;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class ConditionGroupResourceFTest extends TestBase {

    private final FunctionalTestConfig config;

    public ConditionGroupResourceFTest() {
        config = new FunctionalTestConfig();
    }

    /**
     * Used to create as many rules as needed for testing, based on simple rule creation
     */
    private String createRule(String ruleID) throws JSONException {
        //setup
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", ruleID);
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
     * Testing basic condition group creation... should succeed
     * @throws JSONException
     */
    @Test
    public void testConditionGroup() throws JSONException {
        Response response = null;

        // rules setup
        String rule = createRule("testRuleConditionGroup");

        // condition testing
        JSONObject groupJSON = new JSONObject();
        groupJSON.put("operator", LogicalOperator.AND.name());

        WebTarget target = config.restBaseTarget();
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule + "/conditionGroups")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(groupJSON.toString()));

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String group = (String)responseJSON.get("id");

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule + "/conditionGroups/" + group)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);

        // rules clean up
        deleteRule(rule);
    }

    /**
     * Testing basic condition group creation... should succeed
     * @throws JSONException
     */
    @Test
    public void testConditionGroupWithPriority() throws JSONException {
        Integer originalPriority = 99;
        Integer updatedPriority = 59;

        Response response = null;

        // rules setup
        String rule = createRule("testRuleConditionGroupPriority");

        // condition testing
        JSONObject groupJSON = new JSONObject();
        groupJSON.put("operator", LogicalOperator.AND.name());
        groupJSON.put("priority", originalPriority);

        WebTarget target = config.restBaseTarget();
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule + "/conditionGroups")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(groupJSON.toString()));

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String group = (String)responseJSON.get("id");

        //We sent priority 99 and we should get the same as result.
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule + "/conditionGroups/" + group)
            .request(MediaType.APPLICATION_JSON_TYPE).get();

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        Integer priority = (Integer)responseJSON.get("priority");
        assertEquals(originalPriority, priority);

        //Update priority.
        groupJSON.put("priority", updatedPriority);

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule + "/conditionGroups/" + group)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.json(groupJSON.toString()));

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        //We updated to priority 59 and we should get the same as result.
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule + "/conditionGroups/" + group)
            .request(MediaType.APPLICATION_JSON_TYPE).get();
        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        priority = (Integer)responseJSON.get("priority");
        assertEquals(updatedPriority, priority);

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule + "/conditionGroups/" + group)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);

        // rules clean up
        deleteRule(rule);
    }
}

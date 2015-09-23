package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.TestBase;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import static com.dotcms.repackage.org.junit.Assert.assertTrue;

public class ConditionGroupsRestAPITest extends TestBase {

    private final FunctionalTestConfig config;

    public ConditionGroupsRestAPITest() {
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
        Response response = target.path("/sites/" + config.defaultHostId + "/rules")
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
        Response response = target.path("/sites/" + config.defaultHostId + "/rules/" + ruleID)
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
        groupJSON.put("operator", Condition.Operator.AND.name());

        WebTarget target = config.restBaseTarget();
        response = target.path("/sites/" + config.defaultHostId + "/rules/" + rule + "/conditionGroups")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(groupJSON.toString()));

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String group = (String)responseJSON.get("id");

        response = target.path("/sites/" + config.defaultHostId + "/rules/" + rule + "/conditionGroups/" + group)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);

        // rules clean up
        deleteRule(rule);
    }
}

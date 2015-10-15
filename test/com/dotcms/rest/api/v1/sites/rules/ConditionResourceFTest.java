package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.TestBase;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotmarketing.portlets.rules.conditionlet.UsersCountryConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import static com.dotcms.repackage.org.junit.Assert.assertTrue;

public class ConditionResourceFTest extends TestBase {

    private final FunctionalTestConfig config;

    public ConditionResourceFTest() {
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
     * Creates basic condition group for testing
     */
    private String createConditionGroup(String ruleID) throws JSONException {
        JSONObject groupJSON = new JSONObject();
        groupJSON.put("operator", Condition.Operator.AND.name());

        WebTarget target = config.restBaseTarget();
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + ruleID + "/conditionGroups")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(groupJSON.toString()));

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String group = (String)responseJSON.get("id");

        return group;
    }

    private void deleteConditionGroup(String group, String ruleID) {
        WebTarget target = config.restBaseTarget();
        Response response =
            target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + ruleID + "/conditionGroups/" + group)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
    }

    @Test
    /**
     * Testing basic condition creation... should succeed
     * @throws JSONException
     */
    public void testCondition() throws JSONException {
        // setup
        String rule = createRule("testRuleCondition");
        String group = createConditionGroup(rule);

        // condition
        JSONObject conditionJSON = new JSONObject();
        conditionJSON.put("name", "testCondition");
        conditionJSON.put("conditionlet", UsersCountryConditionlet.class.getSimpleName());
        conditionJSON.put("comparison", "is");
        conditionJSON.put("operator", Condition.Operator.AND.name());
        conditionJSON.put("owningGroup", group);

        JSONObject valueJSON = new JSONObject();
        valueJSON.put("key", "isoCode");
        valueJSON.put("value", "FR");
        valueJSON.put("priority", 0);

        JSONObject valuesJSON = new JSONObject();
        valuesJSON.put("isoCode", valueJSON);

        conditionJSON.put("values", valuesJSON);

        WebTarget target = config.restBaseTarget();
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(conditionJSON.toString()));

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String conditionId = (String)responseJSON.get("id");

        // delete
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions/" + conditionId)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);

        deleteConditionGroup(group, rule);
        deleteRule(rule);
    }

    @Test
    public void testConditionGet() throws JSONException {

        // setup
        String rule = createRule("testRuleConditionGet");
        String group = createConditionGroup(rule);

        // condition
        JSONObject conditionJSON = new JSONObject();
        conditionJSON.put("name", "testConditionGet");
        conditionJSON.put("conditionlet", UsersCountryConditionlet.class.getSimpleName());
        conditionJSON.put("comparison", "is");
        conditionJSON.put("operator", Condition.Operator.AND.name());
        conditionJSON.put("owningGroup", group);

        JSONObject valueJSON = new JSONObject();
        valueJSON.put("key", "isoCode");
        valueJSON.put("value", "FR");
        valueJSON.put("priority", 0);

        JSONObject valuesJSON = new JSONObject();
        valuesJSON.put("isoCode", valueJSON);

        conditionJSON.put("values", valuesJSON);

        WebTarget target = config.restBaseTarget();
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(conditionJSON.toString()));

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String conditionId = (String)responseJSON.get("id");

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions/" + conditionId)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        responseStr = response.readEntity(String.class);
        conditionJSON = new JSONObject(responseStr);
        assertTrue(conditionJSON.getString("name").equals("testConditionGet"));

        // delete
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions/" + conditionId)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        deleteConditionGroup(group, rule);
        deleteRule(rule);
    }
}

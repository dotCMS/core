package com.dotcms.rest.api.v1.sites.rules;

import com.dotcms.TestBase;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import static org.junit.Assert.assertTrue;

import org.junit.Ignore;

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class RuleResourceFTest extends TestBase {

    private final FunctionalTestConfig config;

    public RuleResourceFTest() {
        config = new FunctionalTestConfig();
    }

    /**
     * Testing basic rule creation... should succeed
     */
    @Test
    public void testRule() throws JSONException {
        // setup
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", "testRule" + String.valueOf(System.currentTimeMillis()));

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHost.getIdentifier() + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ruleJSON.toString()));

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        // response
        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule = (String)responseJSON.get("id");

        // delete
        response = target.path("/sites/" + config.defaultHost.getIdentifier() + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Testing empty rule name for rule creation... should fail, name should not be null
     */
    @Test
    public void testRuleNoName() throws JSONException {
        // setup
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", "");
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ruleJSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Testing basic rule name creation... should succeed
     */
    @Test
    public void testRuleName() throws JSONException {
        // setup
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", "Test Rule Large Name");
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ruleJSON.toString()));

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        // response
        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule = (String)responseJSON.get("id");

        // delete
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Testing duplicate rule creation... should succeed, multiple rule name for site.
     */
    @Test
    public void testRuleDuplicate() throws JSONException {
        // setup
        JSONObject rule1JSON = new JSONObject();
        rule1JSON.put("name", "testRuleDup");
        rule1JSON.put("enabled", "true");
        rule1JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        JSONObject rule2JSON = new JSONObject();
        rule2JSON.put("name", "testRuleDup");
        rule2JSON.put("enabled", "true");
        rule2JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule1JSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        // response
        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule = (String)responseJSON.get("id");

        // create second rule
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule2JSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        String rule2 = (String)responseJSON.get("id");

        // delete
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule2)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Testing duplicate rule creation... should succeed, multiple rule name for site.
     */
    //@Test
    public void testRuleDuplicateCaseSensitive() throws JSONException {
        // setup
        JSONObject rule1JSON = new JSONObject();
        rule1JSON.put("name", "testRuleDupCS");
        rule1JSON.put("enabled", "true");
        rule1JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // same as above but lowercase name
        JSONObject rule2JSON = new JSONObject();
        rule2JSON.put("name", "testruledupcs");
        rule2JSON.put("enabled", "true");
        rule2JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule1JSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule = (String)responseJSON.get("id");

        // create second rule
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule2JSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        String rule2 = (String)responseJSON.get("id");

        // delete
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule2)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();

        assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);
    }

    /**
     * Testing bad parameters on rule creation... should fail
     */
    @Test
    public void testRuleBadParameters() throws JSONException {
        // setup

        // bad "enabled" value
        JSONObject rule1JSON = new JSONObject();
        rule1JSON.put("name", "testRule1");
        rule1JSON.put("enabled", "none");
        rule1JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // bad "fireOn" null value
        JSONObject rule2JSON = new JSONObject();
        rule2JSON.put("name", "testRule2");
        rule2JSON.put("enabled", "true");
        rule2JSON.put("fireOn", "");

        // bad "fireOn" value
        JSONObject rule3JSON = new JSONObject();
        rule3JSON.put("name", "testRule3");
        rule3JSON.put("enabled", "true");
        rule3JSON.put("fireOn", "something");

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule1JSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);

        // create
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule2JSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);

        // create
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule3JSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_BAD_REQUEST);
    }

    /**
     * Testing the delete of an non existing rule... should fail
     */
    @Test
    public void testRuleDeleteNoExisting() {
        // client call
        WebTarget target = config.restBaseTarget();
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + "NoRule")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();
        // response
        assertTrue(response.getStatus() == HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void testRuleGet() throws JSONException {
        // rule
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", "testGetRule");
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ruleJSON.toString()));
        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule = (String)responseJSON.get("id");

        // get
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        responseStr = response.readEntity(String.class);
        ruleJSON = new JSONObject(responseStr);
        assertTrue(ruleJSON.getString("name").equals("testGetRule"));
        assertTrue(ruleJSON.getString("enabled").equals("true"));
        assertTrue(ruleJSON.getString("fireOn").equals(Rule.FireOn.EVERY_PAGE.toString()));

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        // get non existing rule
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + "00000000-0000-0000-0000-000000000000")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_NOT_FOUND);
    }

    /**
     * Get rules test
     */
    @Test
    public void testRuleGetRules() throws JSONException {
        // rule 1
        JSONObject rule1JSON = new JSONObject();
        rule1JSON.put("name", "testListRule1");
        rule1JSON.put("enabled", "true");
        rule1JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // rule 2
        JSONObject rule2JSON = new JSONObject();
        rule2JSON.put("name", "testListRule2");
        rule2JSON.put("enabled", "true");
        rule2JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // rule 3
        JSONObject rule3JSON = new JSONObject();
        rule3JSON.put("name", "testListRule3");
        rule3JSON.put("enabled", "false");
        rule3JSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule1JSON.toString()));
        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule1 = (String)responseJSON.get("id");

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule2JSON.toString()));
        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        String rule2 = (String)responseJSON.get("id");

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(rule3JSON.toString()));
        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        String rule3 = (String)responseJSON.get("id");

        response =
            target.path("/sites/" + config.defaultHostId + "/ruleengine/rules").request(MediaType.APPLICATION_JSON_TYPE).get();

        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        JSONObject result1JSON = (JSONObject)responseJSON.get(rule1);
        JSONObject result2JSON = (JSONObject)responseJSON.get(rule2);
        JSONObject result3JSON = (JSONObject)responseJSON.get(rule3);

        assertTrue(result1JSON.getString("name").equals("testListRule1"));
        assertTrue(result2JSON.getString("name").equals("testListRule2"));
        assertTrue(result3JSON.getString("name").equals("testListRule3"));

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule1)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule2)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule3)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();
    }

    @Test
    public void testRuleUpdate() throws JSONException {
        String testRuleRename = "testRuleRename";

        // setup
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", "testRuleUpdate");
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // create
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ruleJSON.toString()));
        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        String rule = (String)responseJSON.get("id");

        JSONObject updateJSON = new JSONObject();
        updateJSON.put("name", testRuleRename);
        updateJSON.put("enabled", "false");

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.json(updateJSON.toString()));

        // test update
        assertTrue(response.getStatus() == HttpStatus.SC_OK);

        // get changed rule
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get();

        responseStr = response.readEntity(String.class);
        ruleJSON = new JSONObject(responseStr);
        assertTrue(ruleJSON.getString("name").equals(testRuleRename));
        assertTrue(ruleJSON.getString("enabled").equals("false"));
        assertTrue(ruleJSON.getString("fireOn").equals(Rule.FireOn.EVERY_PAGE.toString()));

        // delete
        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + rule)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .delete();

        // try to update non existing rule
        JSONObject updateNoRuleJSON = new JSONObject();
        updateNoRuleJSON.put("name", "NoRuleToUpdate");

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + "00000000-0000-0000-0000-000000000000")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .put(Entity.json(updateJSON.toString()));

        // response
        assertTrue(response.getStatus() == HttpStatus.SC_NOT_FOUND);
    }
}

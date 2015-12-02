package com.dotcms.rest.api.v1.system.conditionlet;

import com.dotcms.TestBase;
import com.dotcms.repackage.javax.ws.rs.client.Entity;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.http.HttpStatus;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.RestCondition;
import com.dotcms.rest.api.v1.sites.ruleengine.rules.conditions.RestConditionValue;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.rules.conditionlet.UsersCountryConditionlet;
import com.dotmarketing.portlets.rules.model.Condition;
import com.dotmarketing.portlets.rules.model.Rule;
import com.dotmarketing.util.json.JSONException;
import com.dotmarketing.util.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.dotcms.repackage.org.junit.Assert.assertTrue;
import static com.dotcms.repackage.org.mockito.Mockito.*;

public class UsersCountryConditionletFTest extends TestBase {

    private final FunctionalTestConfig config;

    public UsersCountryConditionletFTest() {
        config = new FunctionalTestConfig();
    }

    @Test
    public void testUsersCountryConditionlet() throws JSONException, DotSecurityException, DotDataException {

        // setup
        final String RULE_NAME = "testUsersCountryRule";
        final String CONDITION_NAME = "testUsersCountryCondition";

        //setup
        JSONObject ruleJSON = new JSONObject();
        ruleJSON.put("name", RULE_NAME);
        ruleJSON.put("enabled", "true");
        ruleJSON.put("fireOn", Rule.FireOn.EVERY_PAGE.toString());

        // client call
        WebTarget target = config.restBaseTarget();

        // POST rule
        Response response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(ruleJSON.toString()));

        String responseStr = response.readEntity(String.class);
        JSONObject responseJSON = new JSONObject(responseStr);
        final String RULE_ID = (String)responseJSON.get("id");

        JSONObject groupJSON = new JSONObject();
        groupJSON.put("operator", Condition.Operator.AND.name());

        // POST condition group

        response = target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + RULE_ID + "/conditionGroups")
            .request(MediaType.APPLICATION_JSON_TYPE)
            .post(Entity.json(groupJSON.toString()));

        responseStr = response.readEntity(String.class);
        responseJSON = new JSONObject(responseStr);
        final String GROUP_ID = (String)responseJSON.get("id");


        try {
            // create valid condition
            RestCondition restCondition = new RestCondition.Builder()
                .name(CONDITION_NAME)
                .conditionlet(UsersCountryConditionlet.class.getSimpleName())
                .comparison("is")
                .operator(Condition.Operator.AND.name())
                .owningGroup(GROUP_ID)
                .build();


            // POST condition
            response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(restCondition));
            assertTrue(response.getStatus() == HttpStatus.SC_OK);

            // get the id of the POSTed condition from the response
            responseStr = response.readEntity(String.class);
            responseJSON = new JSONObject(responseStr);
            final String conditionId = (String) responseJSON.get("id");

            // GET condition by Id
            response = target.path("/sites/" +config.defaultHostId + "/ruleengine/conditions/" + conditionId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
            assertTrue(response.getStatus() == HttpStatus.SC_OK);
            RestCondition returnedCondition = response.readEntity(RestCondition.class);
            assertTrue(returnedCondition.name.equals(CONDITION_NAME));

            // create a condition value
            RestConditionValue value = new RestConditionValue.Builder().key("isoCode").value("VE").priority(0).build();

            // POST condition value
            response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions/" + conditionId + "/conditionValues")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(value));

            assertTrue(response.getStatus() == HttpStatus.SC_OK);


            // evaluate the rule

            Rule myRule = APILocator.getRulesAPI().getRuleById(RULE_ID, config.user, false);

            HttpServletRequest req = mock(HttpServletRequest.class);
            HttpServletResponse res = mock(HttpServletResponse.class);

            // Ip address from Venezuela
            when(req.getHeader("X-Forwarded-For")).thenReturn("186.95.98.200");

            assertTrue(myRule.evaluate(req, res));


            // DELETE condition
            response = target.path("/sites/" + config.defaultHostId + "/ruleengine/conditions/" + conditionId).request(MediaType.APPLICATION_JSON_TYPE).delete();

            assertTrue(response.getStatus() == HttpStatus.SC_NO_CONTENT);

        } finally {

            target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + RULE_ID + "/conditionGroups/" + GROUP_ID)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .delete();

            target.path("/sites/" + config.defaultHostId + "/ruleengine/rules/" + RULE_ID)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .delete();
        }
    }
}

package com.dotcms.rest.api.v1.system.ruleengine;

import com.dotcms.LicenseTestUtil;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.javax.ws.rs.core.Response;
import com.dotcms.repackage.org.apache.commons.httpclient.HttpStatus;
import org.junit.BeforeClass;
import org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;
import com.dotmarketing.util.json.JSONException;


import static org.junit.Assert.assertTrue;

/**
 * Created by Oscar Arrieta on 9/21/15.
 *
 * Use this test class to write tests against Actionlets Rest endpoint.
 */
public class ActionletResourceFTest {

    private final FunctionalTestConfig config;

    public ActionletResourceFTest(){
        config = new FunctionalTestConfig();
    }

    @BeforeClass
    public static void prepare () throws Exception {
        LicenseTestUtil.getLicense();
    }

    /**
     * For now is just testing the endpoint /api/v1/system/ruleengine/actionlets/ exists and returns 200(OK) status.
     * TODO: after create actionlet endpoint is developed, can test creating and retrieving specific actionlet.
     * @throws JSONException
     */
    @Test
    public void testListAllActionlets() throws JSONException {

        WebTarget target = config.restBaseTarget();
        //Response.
        Response response = target.path("/system/ruleengine/actionlets/").request(MediaType.APPLICATION_JSON_TYPE).get();

        //Test if the endpoint exists and returns 200(OK) status.
        assertTrue(response.getStatus() == HttpStatus.SC_OK);
    }
}

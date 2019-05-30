package com.dotcms.rest.api.v1.system.i18n;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import junit.framework.Assert;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.junit.Ignore;
import org.junit.Test;
import com.dotcms.rest.api.FunctionalTestConfig;

/**
 * @author Geoff M. Granum
 */

public class I8NResourceFTest {

    private final FunctionalTestConfig config;

    public I8NResourceFTest() {
        config = new FunctionalTestConfig();
    }

    @Ignore
    @Test
    public void testCanGetResource() throws Exception {

        Client client = ClientBuilder.newClient().register(JacksonFeature.class);

        String resourceBaseUrl = config.restBaseUrl() + "/system/i18n/en-US/message/comment/success";
        String resp = client.target(resourceBaseUrl)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(String.class);

        Assert.assertNotNull(resp);
        Assert.assertTrue("Response contains '\"Optional[Your comment has been saved]\"'",
                          resp.contains("Optional[Your comment has been saved]"));
    }
}
 

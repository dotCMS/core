package com.dotcms.rest.api.v1.system.i18n;

import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.ClientBuilder;
import com.dotcms.repackage.javax.ws.rs.core.MediaType;
import com.dotcms.repackage.junit.framework.Assert;
import com.dotcms.repackage.org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import com.dotcms.repackage.org.glassfish.jersey.jackson.JacksonFeature;
import com.dotcms.repackage.org.junit.Test;
import com.dotcms.rest.RestClientBuilder;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Geoff M. Granum
 */
public class I8NResourceFTest {

    private static final long serialVersionUID = 1L;

    private String serverName;
    private Integer serverPort;
    private User user;
    Host defaultHost;
    Client client;

    public I8NResourceFTest() {
        HttpServletRequest request = ServletTestRunner.localRequest.get();
        serverName = request.getServerName();
        serverPort = request.getServerPort();
        HostAPI hostAPI = APILocator.getHostAPI();

        //Setting the test user
        try {
            user = APILocator.getUserAPI().getSystemUser();
            defaultHost = hostAPI.findDefaultHost(user, false);
        } catch (DotDataException dd) {
            dd.printStackTrace();
        } catch (DotSecurityException ds) {
            ds.printStackTrace();
        }

        client = RestClientBuilder.newClient();
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("admin@dotcms.com", "admin");
        client.register(feature);
    }

    @Test
    public void testCanGetResource() throws Exception {

        // Create Jersey client
        Client client = ClientBuilder.newClient().register(JacksonFeature.class);

        String resourceBaseUrl = "http://localhost:8080/api/v1/system/i18n/en-US/message/comment/success";
        String resp = client.target(resourceBaseUrl)
            .request(MediaType.APPLICATION_JSON_TYPE)
            .get(String.class);

        Assert.assertNotNull(resp);
        Assert.assertTrue("Response contains '\"Optional[Your comment has been saved]\"'", resp.contains("Optional[Your comment has been saved]"));
    }
}
 

package com.dotcms.rest.api;

import com.dotcms.repackage.javax.ws.rs.client.Client;
import com.dotcms.repackage.javax.ws.rs.client.WebTarget;
import com.dotcms.repackage.org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import com.dotcms.rest.RestClientBuilder;
import com.dotmarketing.beans.Host;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.business.HostAPI;
import com.dotmarketing.servlets.test.ServletTestRunner;
import com.liferay.portal.model.User;
import javax.servlet.http.HttpServletRequest;

import org.junit.Ignore;

/**
 * @author Geoff M. Granum
 */

@Ignore("Temporarily ignore this. https://github.com/dotCMS/core/issues/9785")
public class FunctionalTestConfig {

    private static final long serialVersionUID = 1L;

    private final HttpServletRequest request;
    public final String serverName;
    public final Integer serverPort;
    public final User user;
    public final Host defaultHost;
    public final String defaultHostId;
    public final Client client;

    public FunctionalTestConfig() {
        request = ServletTestRunner.localRequest.get();

        serverName = request.getServerName();
        serverPort = request.getServerPort();
        HostAPI hostAPI = APILocator.getHostAPI();

        User user = null;
        Host defaultHost = null;
        //Setting the test user
        try {
            user = APILocator.getUserAPI().getSystemUser();
            defaultHost = hostAPI.findDefaultHost(user, false);
        } catch (DotDataException dd) {
            dd.printStackTrace();
        } catch (DotSecurityException ds) {
            ds.printStackTrace();
        }
        this.user = user;
        this.defaultHost = defaultHost;
        this.defaultHostId = defaultHost.getIdentifier();

        client = RestClientBuilder.newClient();
        HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("admin@dotcms.com", "admin");
        client.register(feature);

    }

    public String restBaseUrl() {
        return request.getScheme() + "://" + serverName + ":" + serverPort + "/api/v1";
    }

    public WebTarget restBaseTarget() {
        return client.target(restBaseUrl());
    }
}
 

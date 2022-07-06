package com.dotcms.restclient;

import com.dotcms.cli.ApplicationContext;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import com.dotcms.model.site.GetSitesRequest;
import com.dotcms.model.site.GetSitesResponse;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SitesRestClientTest {

    @Inject
    ApplicationContext applicationContext;

    @Inject
    @RestClient
    LegacyAuthenticationClient authClient;

    @Inject
    @RestClient
    SitesRestClient sitesRestClient;

    @Test
    public void Test_Get_Sites() {

        final String user = "admin@dotcms.com";
        APITokenResponse resp = authClient.getToken(
                APITokenRequest.builder().user(user).password("admin").expirationDays(1).build());

        applicationContext.setToken(resp.entity().token(), user);

        GetSitesResponse sitesResponse = sitesRestClient.getSites(null,false, true, true,1, 10);
        System.out.println(sitesResponse);
    }


}

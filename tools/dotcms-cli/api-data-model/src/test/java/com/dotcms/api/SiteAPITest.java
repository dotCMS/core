package com.dotcms.api;

import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import com.dotcms.model.site.GetSitesResponse;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SiteAPITest {

    @Inject
    AuthSecurityContext authSecurityContext;

    @Inject
    @RestClient
    AuthenticationAPI authClient;

    @Inject
    @RestClient
    SiteAPI siteAPI;

    @Test
    public void Test_Get_Sites() {

        final String user = "admin@dotcms.com";
        final APITokenResponse resp = authClient.getToken(
                APITokenRequest.builder().user(user).password("admin").expirationDays(1).build());

        authSecurityContext.setToken(resp.entity().token(), user);

        final GetSitesResponse sitesResponse = siteAPI.getSites(null, false, true, true, 1,
                10);
        Assertions.assertNotNull(sitesResponse);
    }


}

package com.dotcms.api;

import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import com.dotcms.model.site.Site;
import io.quarkus.test.junit.QuarkusTest;
import java.util.List;
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
        final ResponseEntityView<TokenEntity> response = authClient.getToken(
                APITokenRequest.builder().user(user).password("admin12345678").expirationDays(1).build());

        authSecurityContext.setToken(response.entity().token(), user);

        final ResponseEntityView<List<Site>> sitesResponse = siteAPI.getSites(null, false, true, true, 1,
                10);
        Assertions.assertNotNull(sitesResponse);
    }


}

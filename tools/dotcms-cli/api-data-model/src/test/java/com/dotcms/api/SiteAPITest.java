package com.dotcms.api;

import com.dotcms.model.ResponseEntityView;
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
    AuthenticationContext authenticationContext;

    @Inject
    @RestClient
    SiteAPI siteAPI;

    @Test
    public void Test_Get_Sites() {
        final String user = "admin@dotcms.com";
        final String passwd= "admin";
        authenticationContext.login(user, passwd);
        final ResponseEntityView<List<Site>> sitesResponse = siteAPI.getSites(null, false, true, true, 1,
                10);
        Assertions.assertNotNull(sitesResponse);
    }


}

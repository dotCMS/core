package com.dotcms.api;

import com.dotcms.api.client.RestClientFactory;
import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.config.ServiceBean;
import com.dotcms.model.site.Site;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SiteAPITest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    RestClientFactory clientFactory;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    @Test
    public void Test_Get_Sites() throws IOException {
        final String user = "admin@dotcms.com";
        final char[] passwd= "admin".toCharArray();
        authenticationContext.login(user, passwd);

        final ResponseEntityView<List<Site>> sitesResponse = clientFactory.getClient(SiteAPI.class).getSites(null, false, true, true, 1, 10);
        Assertions.assertNotNull(sitesResponse);
    }


}

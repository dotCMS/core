package com.dotcms.restclient;


import com.dotcms.cli.ApplicationContext;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import com.dotcms.model.contenttype.GetContentTypesResponse;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ContentTypeRestClientTest {

    @Inject
    ApplicationContext applicationContext;

    @Inject
    @RestClient
    ContentTypeRestClient client;

    @Inject
    @RestClient
    LegacyAuthenticationClient authClient;

    @Test
    public void Test_Content_Type() {

        final String user = "admin@dotcms.com";
        APITokenResponse resp = authClient.getToken(
                APITokenRequest.builder().user(user).password("admin").expirationDays(1).build());

        applicationContext.setToken(resp.entity().token(), user);

        final GetContentTypesResponse response = client.getContentTypes(null, null, null, null, null, null, null );
        System.out.println(response);
    }

}

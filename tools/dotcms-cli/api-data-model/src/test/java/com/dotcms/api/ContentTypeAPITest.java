package com.dotcms.api;


import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import com.dotcms.model.contenttype.GetContentTypesResponse;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ContentTypeAPITest {

    @Inject
    AuthSecurityContext authSecurityContext;

    @Inject
    @RestClient
    ContentTypeAPI client;

    @Inject
    @RestClient
    AuthenticationAPI authClient;

    @Test
    public void Test_Content_Type() {

        final String user = "admin@dotcms.com";
        APITokenResponse resp = authClient.getToken(
                APITokenRequest.builder().user(user).password("admin").expirationDays(1).build());

        authSecurityContext.setToken(resp.entity().token(), user);

        final GetContentTypesResponse response = client.getContentTypes(null, null, null, null, null, null, null );
        Assertions.assertNotNull(response);
    }

}

package com.dotcms.restclient;


import com.dotcms.cli.ApplicationContext;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;


@QuarkusTest
class LegacyUsersClientTest {
    @Inject
    @RestClient
    LegacyUsersClient client;

    @Inject
    @RestClient
    LegacyAuthenticationClient authClient;

    @Inject
    ApplicationContext authCtx;


    @Test
    public void testGetCurrent() {

        final String user = "admin@dotcms.com";
        APITokenResponse resp = authClient.getToken(
                APITokenRequest.builder().user(user).password("admin").expirationDays(1).build());

        authCtx.setToken(resp.entity().token(), user);
        /*
        Assertions.assertTrue(authCtx.getCurrentUser().isPresent());
        Assertions.assertEquals(authCtx.getCurrentUser().get(),user);

        final User userResponse = client.getCurrent();
        System.out.println("User response="+userResponse);
        */
    }

}
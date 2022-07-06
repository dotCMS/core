package com.dotcms.restclient;


import com.dotcms.cli.ApplicationContext;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.APITokenResponse;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

@QuarkusTest
//@QuarkusTestResource(WireMockExtensionsResource.class)
public class LegacyAuthenticationClientTest {

    @Inject
    @RestClient
    LegacyAuthenticationClient client;

    @Inject
    ApplicationContext applicationContext;

    @Test
    public void testTokenApi() {
        APITokenResponse tokenResponse = client.getToken(
                APITokenRequest.builder()
                        .user("admin@dotcms.com")
                        .password("admin")
                        .expirationDays(1).build());

        System.out.println("Token response="+tokenResponse);
        applicationContext.setToken(tokenResponse.toString(),"admin@dotCMS.com");

    }
}
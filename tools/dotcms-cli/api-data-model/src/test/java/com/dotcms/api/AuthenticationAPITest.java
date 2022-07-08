package com.dotcms.api;


import com.dotcms.model.ResponseEntityView;
import com.dotcms.model.authentication.APITokenRequest;
import com.dotcms.model.authentication.TokenEntity;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Optional;
import javax.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AuthenticationAPITest {

    @Inject
    @RestClient
    AuthenticationAPI client;

    @Inject
    AuthSecurityContext authSecurityContext;

    @Test
    public void testTokenApi() {

        final String userString = "admin@dotCMS.com";
        final String passwordString = "admin";

        final ResponseEntityView<TokenEntity> tokenResponse = client.getToken(
                APITokenRequest.builder()
                        .user(userString)
                        .password(passwordString)
                        .expirationDays(1).build());

        Assertions.assertNotNull(tokenResponse);
        authSecurityContext.setToken(tokenResponse.toString(), userString);
        final Optional<String> user = authSecurityContext.getUser();
        Assertions.assertTrue(user.isPresent());
        final Optional<String> token = authSecurityContext.getToken();
        Assertions.assertTrue(token.isPresent());
        Assertions.assertEquals(token.get(),tokenResponse.toString());
    }
}
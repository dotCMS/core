package com.dotcms.api;

import io.quarkus.test.junit.QuarkusTest;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AuthAuthenticationContextTest {


    @Inject
    AuthSecurityContext authSecurityContext;

    @Test
    public void Test_App_Context() {
        final String userString = "admin@dotCMS.com";
        final String tokenString = "lol";
        authSecurityContext.setToken(tokenString,userString);
        final Optional<String> user = authSecurityContext.getUser();
        Assertions.assertTrue(user.isPresent());
        Assertions.assertEquals(userString, user.get());
        final Optional<String> token = authSecurityContext.getToken();
        Assertions.assertTrue(token.isPresent());
        Assertions.assertEquals(tokenString, token.get());
    }

}

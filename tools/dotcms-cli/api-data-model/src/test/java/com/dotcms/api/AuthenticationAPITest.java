package com.dotcms.api;


import io.quarkus.test.junit.QuarkusTest;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AuthenticationAPITest {

    @Inject
    AuthenticationContext authenticationContext;

    @Test
    public void Test_Get_Token() {
        final String userString = "admin@dotCMS.com";
        final String passwordString = "admin";
        authenticationContext.login(userString, passwordString);
        final Optional<String> user = authenticationContext.getUser();
        Assertions.assertTrue(user.isPresent());
        final Optional<String> token = authenticationContext.getToken();
        Assertions.assertTrue(token.isPresent());
    }
}
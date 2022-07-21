package com.dotcms.api;

import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AuthenticationContextTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Test
    public void Test_Login_Default() {
        final String userString = "admin@dotCMS.com";
        final String pwdString = "admin";
        authenticationContext.login(userString, pwdString);
    }

}

package com.dotcms.api;


import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Optional;

@QuarkusTest
public class AuthenticationAPITest {

    @Inject
    ServiceManager  serviceManager;

    @Inject
    AuthenticationContext authenticationContext;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    @Test
    public void Test_Get_Token() {
        final String userString = "admin@dotCMS.com";
        final char[] passwordString = "admin".toCharArray();
        authenticationContext.login(userString, passwordString);
        final Optional<String> user = authenticationContext.getUser();
        Assertions.assertTrue(user.isPresent());
        final Optional<char[]> token = authenticationContext.getToken();
        Assertions.assertTrue(token.isPresent());
    }
}
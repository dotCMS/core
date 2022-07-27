package com.dotcms.api;


import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class AuthenticationAPITest {

    @Inject
    ServiceManager  serviceManager;

    @Inject
    AuthenticationContext authenticationContext;

    @Test
    public void Test_Get_Token() throws IOException {

        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());

        final String userString = "admin@dotCMS.com";
        final char[] passwordString = "admin".toCharArray();
        authenticationContext.login(userString, passwordString);
        final Optional<String> user = authenticationContext.getUser();
        Assertions.assertTrue(user.isPresent());
        final Optional<String> token = authenticationContext.getToken();
        Assertions.assertTrue(token.isPresent());
    }
}
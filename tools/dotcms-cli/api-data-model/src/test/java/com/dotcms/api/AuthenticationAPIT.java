package com.dotcms.api;


import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import java.util.Optional;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
public class AuthenticationAPIT {

    @Inject
    ServiceManager  serviceManager;

    @Inject
    AuthenticationContext authenticationContext;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").url(new URL("http://localhost:8080")).active(true).build());
    }

    @Test
    public void Test_Get_Token() throws IOException {
        final String userString = "admin@dotCMS.com";
        final char[] passwordString = "admin".toCharArray();
        authenticationContext.login(userString, passwordString);
        final Optional<String> user = authenticationContext.getUser();
        Assertions.assertTrue(user.isPresent());
        final Optional<char[]> token = authenticationContext.getToken();
        Assertions.assertTrue(token.isPresent());
    }
}

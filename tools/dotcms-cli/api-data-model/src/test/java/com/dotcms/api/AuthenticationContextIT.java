package com.dotcms.api;

import com.dotcms.DotCMSITProfile;
import com.dotcms.api.client.model.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.io.IOException;
import java.net.URL;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class AuthenticationContextIT {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").url(new URL("http://localhost:8080")).active(true).build());
    }

    @Test
    public void Test_Login_Default() throws IOException {
        final String userString = "admin@dotCMS.com";
        final char[] pwdString = "admin".toCharArray();
        authenticationContext.login(userString, pwdString);

    }

}

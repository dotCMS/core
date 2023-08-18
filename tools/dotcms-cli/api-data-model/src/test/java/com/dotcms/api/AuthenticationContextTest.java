package com.dotcms.api;

import com.dotcms.api.client.ServiceManager;
import com.dotcms.model.config.ServiceBean;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class AuthenticationContextTest {

    @Inject
    AuthenticationContext authenticationContext;

    @Inject
    ServiceManager serviceManager;

    @BeforeEach
    public void setupTest() throws IOException {
        serviceManager.removeAll().persist(ServiceBean.builder().name("default").active(true).build());
    }

    @Test
    public void Test_Login_Default() throws IOException {
        final String userString = "admin@dotCMS.com";
        final char[] pwdString = "admin".toCharArray();
        authenticationContext.login(userString, pwdString);


    }

}

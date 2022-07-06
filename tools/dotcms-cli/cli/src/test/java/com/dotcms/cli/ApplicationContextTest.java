package com.dotcms.cli;

import com.dotcms.cli.ApplicationContext;
import io.quarkus.test.junit.QuarkusTest;
import java.util.Optional;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ApplicationContextTest {


    @Inject
    ApplicationContext applicationContext;

    @Test
    public void Test_App_Context() {
        final String userString = "admin@dotCMS.com";
        final String tokenString = "lol";
        applicationContext.setToken(tokenString,userString);
        final Optional<String> user = applicationContext.getUser();
        Assertions.assertTrue(user.isPresent());
        Assertions.assertEquals(userString, user.get());
        final Optional<String> token = applicationContext.getToken();
        Assertions.assertTrue(token.isPresent());
        Assertions.assertEquals(tokenString, token.get());
    }

}

package com.dotcms.cli;

import com.dotcms.cli.ApplicationContext;
import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ApplicationContextTest {


    @Inject
    ApplicationContext applicationContext;

    @Test
    public void Test_App_Context() {

    }

}

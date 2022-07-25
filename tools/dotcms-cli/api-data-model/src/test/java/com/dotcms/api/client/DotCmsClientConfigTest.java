package com.dotcms.api.client;

import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DotCmsClientConfigTest {

    @Inject
    DotCmsClientConfig config;

    @Test
    public void testDefault() {
       Assertions.assertEquals("https://demo.dotcms.com/api",config.servers().get("default").toString());
    }
}
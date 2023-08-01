package com.dotcms.api.client;

import io.quarkus.test.junit.QuarkusTest;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class DotCmsClientConfigIT {

    @Inject
    DotCmsClientConfig config;

    @Test
    public void testDefault() {
       Assertions.assertEquals("http://localhost:8080/api",config.servers().get("default").toString());
    }
}
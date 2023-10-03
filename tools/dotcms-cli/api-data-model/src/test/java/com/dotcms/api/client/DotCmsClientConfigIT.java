package com.dotcms.api.client;

import com.dotcms.ContainerResource;
import com.dotcms.DotCMSITProfile;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import javax.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class DotCmsClientConfigIT {

    @Inject
    DotCmsClientConfig config;

    @Test
    public void testDefault() {
       Assertions.assertEquals("http://localhost:8080/api",config.servers().get("default").toString());
    }
}

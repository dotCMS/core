package com.dotcms.observability.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
class SystemStateServiceTest {

    @Inject
    SystemStateService systemStateService;

    private static final Logger LOG = Logger.getLogger(SystemStateServiceTest.class);

    @Test
    void testGetState() {
        Assertions.assertNotNull(systemStateService);
        Assertions.assertNotNull(systemStateService.getState("subsystemName"));
    }

}

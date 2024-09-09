package com.dotcms.observability.service;

import com.dotcms.observability.state.State;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Optional;
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
        final Optional<State> subsystemName = systemStateService.getState("subsystemName");
        Assertions.assertTrue(subsystemName.isEmpty());
    }

}

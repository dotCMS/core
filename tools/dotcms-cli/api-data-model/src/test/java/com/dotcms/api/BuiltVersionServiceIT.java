package com.dotcms.api;

import com.dotcms.DotCMSITProfile;
import com.dotcms.model.asset.BuildVersion;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import java.util.Optional;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@QuarkusTest
@TestProfile(DotCMSITProfile.class)
class BuiltVersionServiceIT {

    @Inject
    BuiltVersionService builtVersionService;

    /**
     * Given scenario: We simply want to test the version of the build
     * Expected result: the version of the build is returned
     */
    @Test
     void testVersion() {
         Optional<BuildVersion> version = builtVersionService.version();
         Assertions.assertTrue(version.isPresent());
         Assertions.assertNotNull(version.get().name());
         Assertions.assertNotNull(version.get().version());
         Assertions.assertTrue(version.get().timestamp() > 0);
         Assertions.assertNotNull(version.get().revision());
     }
}
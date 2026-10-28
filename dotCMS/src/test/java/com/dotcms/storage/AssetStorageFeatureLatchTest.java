package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import com.dotcms.storage.binary.BinaryAssetCleanupProcessor;
import com.dotcms.storage.binary.BinaryFieldCleanupProcessor;
import com.dotmarketing.util.Config;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AssetStorageFeatureLatchTest {
    private static final List<String> S3_QUEUES = List.of(BinaryAssetCleanupProcessor.QUEUE,
            BinaryFieldCleanupProcessor.QUEUE);

    private String previous;

    @BeforeEach
    void remember() {
        previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
    }

    @AfterEach
    void restore() {
        Config.setProperty(AssetStorageFeature.FLAG, previous);
    }

    @Test
    void runtimeConfigurationChangesAreIgnoredUntilReset() {
        Config.setProperty(AssetStorageFeature.FLAG, true);
        assertTrue(AssetStorageFeature.isEnabled());
        try (MockedStatic<Config> config = mockStatic(Config.class)) {
            config.when(() -> Config.getBooleanProperty(AssetStorageFeature.FLAG, false)).thenReturn(false);
            assertTrue(AssetStorageFeature.isEnabled(), "A running node must keep the mode it started with");
            AssetStorageFeature.reset();
            assertFalse(AssetStorageFeature.isEnabled());
        } finally {
            AssetStorageFeature.reset();
        }
    }

    @Test
    void inMemoryOverridesSwitchTheMode() {
        Config.setProperty(AssetStorageFeature.FLAG, true);
        assertTrue(AssetStorageFeature.isEnabled());
        Config.setProperty(AssetStorageFeature.FLAG, false);
        assertFalse(AssetStorageFeature.isEnabled());
    }

    @Test
    void s3JobProcessorsRegisterOnlyWhenEnabled() {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        S3_QUEUES.forEach(q -> assertFalse(AssetStorageFeature.allowsJobQueue(q), q));
        assertTrue(AssetStorageFeature.allowsJobQueue("importContentlets"), "Other queues are unaffected");

        Config.setProperty(AssetStorageFeature.FLAG, true);
        S3_QUEUES.forEach(q -> assertTrue(AssetStorageFeature.allowsJobQueue(q), q));
    }

    @Test
    void overrideCallbacksRunOnlyForTheirKeyAfterTheValueIsSet() {
        final String key = "ASSET_STORAGE_LATCH_TEST_HOOKED_KEY";
        final String otherKey = "ASSET_STORAGE_LATCH_TEST_OTHER_KEY";
        final List<String> seen = new ArrayList<>();
        Config.onOverride(key, () -> seen.add(Config.getStringProperty(key, null)));
        try {
            Config.setProperty(otherKey, "ignored");
            assertTrue(seen.isEmpty(), "Overriding another key must not run the callback");
            Config.setProperty(key, "updated");
            assertEquals(List.of("updated"), seen, "The callback runs once and sees the new value");
        } finally {
            Config.setProperty(key, null);
            Config.setProperty(otherKey, null);
        }
    }
}

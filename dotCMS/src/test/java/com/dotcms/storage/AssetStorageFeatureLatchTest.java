package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

import com.dotcms.publishing.output.BundleArchiveCleanupProcessor;
import com.dotcms.storage.binary.BinaryAssetBackfillProcessor;
import com.dotcms.storage.binary.BinaryAssetCleanupProcessor;
import com.dotcms.storage.binary.BinaryFieldCleanupProcessor;
import com.dotmarketing.util.Config;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AssetStorageFeatureLatchTest {
    private static final List<Class<?>> S3_PROCESSORS = List.of(BinaryAssetBackfillProcessor.class,
            BinaryAssetCleanupProcessor.class, BinaryFieldCleanupProcessor.class,
            BundleArchiveCleanupProcessor.class);

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
        S3_PROCESSORS.forEach(p -> assertFalse(AssetStorageFeature.allowsJobProcessor(p), p.getName()));
        assertTrue(AssetStorageFeature.allowsJobProcessor(String.class), "Other processors are unaffected");

        Config.setProperty(AssetStorageFeature.FLAG, true);
        S3_PROCESSORS.forEach(p -> assertTrue(AssetStorageFeature.allowsJobProcessor(p), p.getName()));
    }
}

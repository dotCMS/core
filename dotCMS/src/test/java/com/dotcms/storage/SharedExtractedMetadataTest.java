package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.dotmarketing.util.Config;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SharedExtractedMetadataTest {
    @TempDir Path root;
    private String previousFlag;

    @BeforeEach
    void enable() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
    }

    @AfterEach
    void restore() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    @Test
    void unknownExtractorVersionExtractsWithoutTheSharedCache() throws Exception {
        assertTrue(AssetStorageFeature.isEnabled());
        final File source = Files.writeString(root.resolve("asset.txt"), "same bytes").toFile();
        final StoragePersistenceAPI storage = mock(StoragePersistenceAPI.class);
        final List<File> extractedFrom = new ArrayList<>();
        final Map<String, Serializable> extracted = Map.of("content", "same bytes");

        final Map<String, Serializable> result = new SharedExtractedMetadata(storage)
                .get(source, null, 1, 100, file -> {
                    extractedFrom.add(file);
                    return extracted;
                });

        assertEquals(extracted, result);
        assertEquals(List.of(source), extractedFrom, "Extraction must run on the source, not a cached copy");
        verifyNoInteractions(storage);
    }
}

package com.dotcms.storage.binary;

import com.dotcms.storage.AssetStorageFeature;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BinaryAssetReferenceTest {
    @TempDir Path root;

    @Test void inventoryIncludesOnlyOwnedLegacyFilesAndPropagatesListingFailures() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final var binaries = mock(BinaryAssetStorageAPI.class);
        final String json = """
                {"fields":{
                  "oldImage":{"type":"Image","value":"MixedCase.JPG"},
                  "oldFile":{"type":"File","value":"Document.Txt"},
                  "linked":{"type":"Image","value":"other-content-identifier"},
                  "external":{"type":"File","value":"https://example.com/asset"},
                  "text":{"type":"Text","value":"Unrelated.Txt"},
                  "retired":{"type":"Binary","value":"Retired.Txt"}
                }}
                """;
        try (var locator = mockStatic(com.dotmarketing.business.APILocator.class);
             var paths = mockStatic(ConfigUtils.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            locator.when(com.dotmarketing.business.APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
            when(binaries.listBinaryPaths("abc123")).thenReturn(java.util.List.of(
                    "a/b/abc123/oldImage/MixedCase.JPG", "a/b/abc123/oldFile/Document.Txt",
                    "a/b/abc123/text/Unrelated.Txt"));
            final var references = BinaryAssetReference.fromContentJson(json, "abc123");
            assertEquals(java.util.Set.of("oldImage", "oldFile", "retired"), references.keySet());
            assertEquals("MixedCase.JPG", references.get("oldImage").fileName());
            verify(binaries).listBinaryPaths("abc123");
            reset(binaries);
            when(binaries.listBinaryPaths("abc123")).thenThrow(new com.dotmarketing.exception.DotDataException("S3 unavailable"));
            assertThrows(com.dotmarketing.exception.DotDataException.class,
                    () -> BinaryAssetReference.fromContentJson(json, "abc123"));
            Config.setProperty(AssetStorageFeature.FLAG, false);
            clearInvocations(binaries);
            assertThrows(IllegalStateException.class, () -> BinaryAssetReference.fromContentJson(json, "abc123"));
            verifyNoInteractions(binaries);
        } finally { Config.setProperty(AssetStorageFeature.FLAG, previous); }
    }

    @Test void revisionReferencesCannotEscapeTheirOwner() {
        String key = "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG";
        assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.localFile("abc123", "OtherField", key));
        assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.localFile("abc123", "HeroImage", key + "/../../outside"));
        assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.localFile("../outside", "HeroImage", key));
    }
}

package com.dotcms.storage.binary;

import com.dotcms.content.model.type.system.AbstractBinaryFieldType;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
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

    @Test void metadataFileUsesLegacyFileXmlWithoutLosingTheSourceReference() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var paths = mockStatic(ConfigUtils.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            final File original = new File(root.toFile(), "a/b/abc123/HeroImage/Mixed.GIF");
            final String metadataKey = BinaryAssetReference.newMetadataKey(original, "abc123", "HeroImage");
            final File snapshot = BinaryAssetReference.withMetadata(original, "abc123", "HeroImage", metadataKey);
            final var serializer = com.dotcms.util.xstream.XStreamHandler.newXStreamInstance();
            final var input = new HashMap<String, Object>();
            input.put("asset", snapshot);
            final String xml = serializer.toXML(input);
            assertFalse(xml.contains("MetadataFile"));
            final Map<?, ?> restored = (Map<?, ?>) serializer.fromXML(xml);
            assertEquals(File.class, restored.get("asset").getClass());
            assertEquals(original, restored.get("asset"));
            assertEquals(metadataKey, BinaryAssetReference.metadataKeyOf(snapshot));
            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertEquals(serializer.toXML(new HashMap<>(Map.of("asset", original))), xml,
                    "Enabled wrappers must keep the original File wire format");
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }

    @Test void immutableFieldReferenceSerializesWithoutReadingFileAndFlagOffRetainsLegacyJson() throws Exception {
        String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var paths = mockStatic(ConfigUtils.class)) {
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            Config.setProperty(AssetStorageFeature.FLAG, true);
            String key = "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG";
            File file = BinaryAssetReference.localFile("abc123", "HeroImage", key);
            assertFalse(file.exists());
            assertEquals(key, BinaryAssetReference.keyOf(file, "abc123", "HeroImage"));
            assertNull(BinaryAssetReference.keyOf(file, "def456", "HeroImage"), "A new content version must copy the bytes before publishing its own reference");
            assertNull(BinaryAssetReference.keyOf(file, "abc123", "OtherImage"));
            BinaryField field = mock(BinaryField.class, CALLS_REAL_METHODS);
            var value = (AbstractBinaryFieldType) field.fieldValue(file).orElseThrow().build();
            assertEquals("Friday.PNG", value.value());
            assertEquals(key, value.storageKey());
            String json = new ObjectMapper().writeValueAsString(value);
            assertTrue(json.contains(key));
            assertEquals(key, ((AbstractBinaryFieldType) new ObjectMapper().readValue(json,
                    com.dotcms.content.model.FieldValue.class)).storageKey());
            assertFalse(file.exists());

            Config.setProperty(AssetStorageFeature.FLAG, false);
            var legacy = (AbstractBinaryFieldType) field.fieldValue(file).orElseThrow().build();
            assertNull(legacy.storageKey());
            assertFalse(new ObjectMapper().writeValueAsString(legacy).contains("storageKey"));
            var provider = mock(StoragePersistenceAPI.class);
            var api = new BinaryAssetStorageAPIImpl(provider);
            assertThrows(IllegalStateException.class, () -> api.storeRevision("abc123", "HeroImage", "Friday.PNG", file));
            assertThrows(IllegalStateException.class, () -> api.getRevisionFile("abc123", "HeroImage", key));
            verifyNoInteractions(provider);
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }

    @Test void replacementRenditionsHaveDistinctKeysAndStayInTheAssetShard() throws Exception {
        String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var paths = mockStatic(ConfigUtils.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            paths.when(ConfigUtils::getDotGeneratedPath).thenReturn(root.resolve("generated").toString());
            File first = BinaryAssetReference.localFile("abc123", "HeroImage", "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG");
            File second = BinaryAssetReference.localFile("abc123", "HeroImage", "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG");
            Map<String, String[]> params = new HashMap<>();
            params.put("resize_w", new String[]{"32"});
            params.put("fieldVarName", new String[]{"HeroImage"});
            params.put("assetInodeOrIdentifier", new String[]{"abc123"});
            var filter = new com.dotmarketing.image.filter.ResizeImageFilter();
            File oldRendition = filter.getResultsFile(first, params);
            File newRendition = filter.getResultsFile(second, params);
            assertNotEquals(oldRendition, newRendition);
            assertEquals(root.resolve("generated/a/b/abc123").toFile().getCanonicalFile(), newRendition.getParentFile());
            assertEquals(newRendition, filter.getResultsFile(second, params));
            assertEquals(newRendition.getParentFile(), filter.getResultsFile(newRendition, params).getParentFile());
            File uploadedName = BinaryAssetReference.localFile("abc123", "HeroImage",
                    "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/dotGenerated_upload.PNG");
            assertEquals(newRendition.getParentFile(), filter.getResultsFile(uploadedName, params).getParentFile(),
                    "An uploaded filename alone must not be mistaken for a generated cache file");
            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertEquals(root.resolve("generated/a/b").toFile().getCanonicalFile(),
                    filter.getResultsFile(root.resolve("Original.PNG").toFile(), params).getParentFile());
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }

    @Test void metadataEditsHaveIndependentReferencesAndSurviveJsonAndColdRestoration() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var paths = mockStatic(ConfigUtils.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            final String key = "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG";
            final File binary = BinaryAssetReference.localFile("abc123", "HeroImage", key);
            final String firstKey = BinaryAssetReference.newMetadataKey(binary, "abc123", "HeroImage");
            final File first = BinaryAssetReference.withMetadata(binary, "abc123", "HeroImage", firstKey);
            final String secondKey = BinaryAssetReference.newMetadataKey(first, "abc123", "HeroImage");
            assertNotEquals(firstKey, secondKey);
            assertEquals(key, BinaryAssetReference.keyOf(first));
            assertEquals(firstKey, BinaryAssetReference.metadataKeyOf(
                    BinaryAssetReference.preserveMetadata(binary, first)));
            assertNull(BinaryAssetReference.metadataKeyOf(first, "def456", "HeroImage"));
            assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.withMetadata(
                    binary, "abc123", "OtherField", firstKey));
            assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.withMetadata(
                    binary, "abc123", "HeroImage", firstKey + "/../outside"));
            final BinaryField field = mock(BinaryField.class, CALLS_REAL_METHODS);
            final var value = (AbstractBinaryFieldType) field.fieldValue(first).orElseThrow().build();
            final ObjectMapper mapper = new ObjectMapper();
            final String json = mapper.writeValueAsString(value);
            assertEquals(firstKey, ((AbstractBinaryFieldType) mapper.readValue(json,
                    com.dotcms.content.model.FieldValue.class)).metadataStorageKey());
            final File hydrated = BinaryAssetReference.fromJson(mapper.readTree(json), "abc123", "HeroImage")
                    .localFile("abc123", "HeroImage");
            assertEquals(firstKey, BinaryAssetReference.metadataKeyOf(hydrated));
            assertFalse(hydrated.exists(), "JSON reconstruction must not require stored bytes");
            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertFalse(mapper.writeValueAsString(field.fieldValue(first).orElseThrow().build())
                    .contains("metadataStorageKey"));
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }

    @Test void revisionReferencesCannotEscapeTheirOwner() {
        String key = "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG";
        assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.localFile("abc123", "OtherField", key));
        assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.localFile("abc123", "HeroImage", key + "/../../outside"));
        assertThrows(IllegalArgumentException.class, () -> BinaryAssetReference.localFile("../outside", "HeroImage", key));
    }

    @Test void metadataIdentityFollowsTheSnapshotWithoutFilesystemAccess() {
        String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var paths = mockStatic(ConfigUtils.class)) {
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            var metadata = mock(com.dotcms.storage.FileMetadataAPI.class, CALLS_REAL_METHODS);
            var content = new com.dotmarketing.portlets.contentlet.model.Contentlet();
            content.setInode("abc123");
            String firstKey = "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG";
            File first = BinaryAssetReference.localFile("abc123", "HeroImage", firstKey);
            content.getMap().put("HeroImage", first);
            Config.setProperty(AssetStorageFeature.FLAG, true);
            String firstMetadata = metadata.getFileName(content, "HeroImage");
            assertEquals("/" + firstKey + "-metadata.json", firstMetadata);
            assertEquals(firstMetadata, metadata.getMetadataCacheKey(content, "HeroImage"));
            File second = BinaryAssetReference.localFile("abc123", "HeroImage",
                    "a/b/abc123/HeroImage/.revisions/" + UUID.randomUUID() + "/Friday.PNG");
            content.getMap().put("HeroImage", second);
            assertNotEquals(firstMetadata, metadata.getFileName(content, "HeroImage"));
            assertNotEquals(firstMetadata, metadata.getMetadataCacheKey(content, "HeroImage"));
            assertFalse(first.exists());
            assertFalse(second.exists());

            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertEquals("/a/b/abc123/HeroImage-metadata.json", metadata.getFileName(content, "HeroImage"));
            assertEquals("abc123:HeroImage", metadata.getMetadataCacheKey(content, "HeroImage"));
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }
}

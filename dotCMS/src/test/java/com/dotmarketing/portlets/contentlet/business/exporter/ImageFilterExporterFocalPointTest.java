package com.dotmarketing.portlets.contentlet.business.exporter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.storage.FileMetadataAPI;
import com.dotcms.storage.binary.BinaryAssetReference;
import com.dotcms.storage.model.Metadata;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.image.filter.ImageFilter;
import com.dotmarketing.image.focalpoint.FocalPointAPI;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ImageFilterExporterFocalPointTest {
    @TempDir Path root;

    @Test
    void legacyAndRevisionCropsKeepTheirSourceMetadataSnapshot() throws Exception {
        final var metadata = mock(FileMetadataAPI.class);
        final var currentContent = mock(ContentletAPI.class);
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class)) {
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            locator.when(APILocator::getContentletAPI).thenReturn(currentContent);
            final var exporter = new ImageFilterExporter();
            for (String tail : new String[]{"Photo.PNG",
                    ".revisions/11111111-1111-1111-1111-111111111111/Photo.PNG"}) {
                final File original = root.resolve("a/b/abc123/HeroImage/" + tail).toFile();
                final String oldKey = BinaryAssetReference.newMetadataKey(original, "abc123", "HeroImage");
                final String newKey = BinaryAssetReference.newMetadataKey(original, "abc123", "HeroImage");
                final File earlier = BinaryAssetReference.withMetadata(original, "abc123", "HeroImage", oldKey);
                final File later = BinaryAssetReference.withMetadata(original, "abc123", "HeroImage", newKey);
                when(metadata.getMetadata(any(Contentlet.class), eq("HeroImage"))).thenAnswer(call -> {
                    Contentlet snapshot = call.getArgument(0);
                    assertEquals("abc123", snapshot.getInode());
                    File source = (File) snapshot.getMap().get("HeroImage");
                    String key = BinaryAssetReference.metadataKeyOf(source);
                    if (key == null) {
                        return null; // An earlier legacy snapshot also pins the absence of an edit.
                    }
                    assertTrue(key.equals(oldKey) || key.equals(newKey));
                    return new Metadata("HeroImage", Map.of(Metadata.CUSTOM_PROP_PREFIX + FocalPointAPI.FOCAL_POINT,
                            key.equals(oldKey) ? "0.25,0.5" : "0.75,0.5"));
                });
                // Files are deliberately absent locally: resolving metadata cannot depend on a warm cache.
                for (File source : new File[]{later, earlier, original, earlier}) {
                    final var parameters = parameters();
                    exporter.resolveCropFocalPoint(source, parameters);
                    final String expected = source == later ? "0.75,0.5" : source == earlier ? "0.25,0.5" : "";
                    assertArrayEquals(new String[]{expected}, parameters.get(ImageFilter.RESOLVED_CROP_FOCAL_POINT));
                }
            }
            verifyNoInteractions(currentContent);
        }
    }

    @Test
    void explicitPointOverridesMetadataAndStorageFailuresAreNotCachedAsAbsence() throws Exception {
        final var metadata = mock(FileMetadataAPI.class);
        try (var paths = mockStatic(ConfigUtils.class); var locator = mockStatic(APILocator.class)) {
            paths.when(ConfigUtils::getAssetPath).thenReturn(root.toString());
            locator.when(APILocator::getFileMetadataAPI).thenReturn(metadata);
            final var exporter = new ImageFilterExporter();
            final File source = root.resolve("a/b/abc123/HeroImage/Photo.PNG").toFile();
            final var parameters = parameters();
            parameters.put("fp", new String[]{"0.1,0.2"});
            exporter.resolveCropFocalPoint(source, parameters);
            assertArrayEquals(new String[]{"0.1,0.2"}, parameters.get(ImageFilter.RESOLVED_CROP_FOCAL_POINT));
            verifyNoInteractions(metadata);

            when(metadata.getMetadata(any(Contentlet.class), eq("HeroImage")))
                    .thenThrow(new DotDataException("metadata unavailable"));
            final var failed = parameters();
            assertThrows(DotDataException.class, () -> exporter.resolveCropFocalPoint(source, failed));
            assertFalse(failed.containsKey(ImageFilter.RESOLVED_CROP_FOCAL_POINT));
        }
    }

    private static Map<String, String[]> parameters() {
        final Map<String, String[]> parameters = new HashMap<>();
        parameters.put("assetInodeOrIdentifier", new String[]{"abc123"});
        parameters.put("fieldVarName", new String[]{"HeroImage"});
        return parameters;
    }
}

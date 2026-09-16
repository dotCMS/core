package com.dotcms.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TemporaryAssetStorageTest {
    @TempDir Path root;

    /** Also called by the real MinIO test, using two independent local cache roots. */
    static void assertRoundTrip(final StoragePersistenceAPI remote, final Path root) throws Exception {
        final String flag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final String ttl = Config.getStringProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", null);
        try (var locator = mockStatic(APILocator.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            Config.setProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 1800);
            final var metadata = mock(FileStorageAPI.class);
            final var binaries = mock(BinaryAssetStorageAPI.class);
            locator.when(APILocator::getFileStorageAPI).thenReturn(metadata);
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(binaries);
            final String id = "temp_Mixed-Case";
            final var nodeA = new TemporaryAssetStorage(remote, root.resolve("node-a"));
            final var nodeB = new TemporaryAssetStorage(remote, root.resolve("node-b"));
            final File source = nodeA.file(id, "Nested/Hero.GIF");
            Files.createDirectories(source.toPath().getParent());
            Files.writeString(source.toPath(), "complete upload bytes");
            Files.writeString(source.toPath().resolveSibling(TemporaryAssetStorage.PERMISSIONS_FILE), "[\"owner\"]");
            nodeA.store(id, source);
            final var receipt = nodeB.receipt(id).orElseThrow();
            assertTrue(nodeB.exists(receipt));
            assertTrue(nodeB.retrieve(receipt, List.of("stranger")).isEmpty());
            assertFalse(Files.exists(root.resolve("node-b")), "Denied access must not materialize a file");
            Files.delete(source.toPath());
            final File restored = nodeB.retrieve(receipt, List.of("owner")).orElseThrow();
            assertEquals("Nested/Hero.GIF", root.resolve("node-b").resolve(id).toRealPath().relativize(restored.toPath()).toString());
            assertEquals("complete upload bytes", Files.readString(restored.toPath()));
            assertEquals(receipt.modifiedAt(), restored.lastModified(), "Restoration must not renew expiry");
            assertEquals("complete upload bytes", Files.readString(nodeA.retrieve(receipt, List.of("owner")).orElseThrow().toPath()));
            Config.setProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", 0);
            assertFalse(nodeB.exists(receipt));
            assertTrue(nodeB.retrieve(receipt, List.of("owner")).isEmpty());

            doThrow(new DotDataException("metadata unavailable")).when(metadata).removeMetaData(any());
            assertThrows(DotDataException.class, nodeB::cleanupExpired);
            assertTrue(remote.existsObject(TemporaryAssetStorage.GROUP, receipt.key()), "Failed cleanup must retain its retry record and payload");
            assertTrue(nodeB.receipt(id).isPresent());
            doReturn(true).when(metadata).removeMetaData(any());
            nodeB.cleanupExpired();
            assertFalse(remote.existsObject(TemporaryAssetStorage.GROUP, receipt.key()));
            assertTrue(nodeB.receipt(id).isEmpty());
            assertFalse(restored.exists());
            verify(binaries).deleteGeneratedFiles(id);
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, flag);
            Config.setProperty("TEMP_RESOURCE_MAX_AGE_SECONDS", ttl);
        }
    }

    @Test
    void coldRoundTripAuthorizationAndExpiry() throws Exception {
        final var remote = memoryRemote();
        assertRoundTrip(remote, root);
        verify(remote, times(2)).pullFile(anyString(), anyString());
        verify(remote, times(2)).releaseRetrievedFile(any());
    }

    @Test
    void rejectedUploadRetainsLocalBytesAndDisabledStorageDoesNoRemoteWork() throws Exception {
        final String flag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try {
            final var remote = memoryRemote();
            final var store = new TemporaryAssetStorage(remote, root);
            Config.setProperty(AssetStorageFeature.FLAG, true);
            final String id = "temp_Failed";
            final File source = store.file(id, "Image.GIF");
            Files.createDirectories(source.toPath().getParent());
            Files.writeString(source.toPath(), "retain me");
            Files.writeString(source.toPath().resolveSibling(TemporaryAssetStorage.PERMISSIONS_FILE), "[\"owner\"]");
            doReturn(false).when(remote).backfillFile(anyString(), anyString(), any());
            assertThrows(DotDataException.class, () -> store.store(id, source));
            assertEquals("retain me", Files.readString(source.toPath()));
            assertTrue(store.isManagedLocally(id));
            assertTrue(store.receipt(id).isPresent(), "Interrupted uploads must remain discoverable for expiry");
            assertFalse(store.exists(store.receipt(id).orElseThrow()));
            for (String name : List.of("../other/file", "/outside", "whoCanUse.tmp", ".s3-upload", "x.meta.tmp", "a\\b")) {
                assertThrows(IllegalArgumentException.class, () -> store.file(id, name));
            }
            Files.createSymbolicLink(root.resolve(id).resolve("alias"), root.resolve("another"));
            assertThrows(IllegalArgumentException.class, () -> store.file(id, "alias/file"));
            clearInvocations(remote);
            Config.setProperty(AssetStorageFeature.FLAG, false);
            store.store(id, source);
            store.cleanupExpired();
            assertTrue(store.receipt(id).isEmpty());
            assertTrue(store.retrieve(new TemporaryAssetStorage.Receipt(id, "Image.GIF", System.currentTimeMillis(), List.of("owner")), List.of("owner")).isEmpty());
            verifyNoInteractions(remote);
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, flag);
        }
    }

    private StoragePersistenceAPI memoryRemote() throws Exception {
        final var remote = mock(StoragePersistenceAPI.class);
        final Map<String, Object> objects = new HashMap<>();
        when(remote.backfillObject(anyString(), anyString(), any(), any(), any())).thenAnswer(call -> {
            final String key = call.getArgument(1);
            final Object value = call.getArgument(4);
            return objects.putIfAbsent(key, value) == null || objects.get(key).equals(value);
        });
        when(remote.backfillFile(anyString(), anyString(), any())).thenAnswer(call -> {
            final String key = call.getArgument(1);
            final byte[] bytes = Files.readAllBytes(call.<File>getArgument(2).toPath());
            return objects.putIfAbsent(key, bytes) == null || java.util.Arrays.equals((byte[]) objects.get(key), bytes);
        });
        when(remote.pullObject(anyString(), anyString(), any())).thenAnswer(call -> objects.get(call.<String>getArgument(1)));
        when(remote.existsObject(anyString(), anyString())).thenAnswer(call -> objects.containsKey(call.<String>getArgument(1)));
        when(remote.pullFile(anyString(), anyString())).thenAnswer(call -> {
            final byte[] bytes = (byte[]) objects.get(call.<String>getArgument(1));
            return bytes == null ? null : Files.write(Files.createTempFile(root, "download-", ".tmp"), bytes).toFile();
        });
        doAnswer(call -> { Files.delete(call.<File>getArgument(0).toPath()); return null; }).when(remote).releaseRetrievedFile(any());
        when(remote.listObjectPaths(anyString(), anyString())).thenAnswer(call ->
                objects.keySet().stream().filter(key -> key.startsWith(call.<String>getArgument(1))).toList());
        when(remote.deleteObjectAndReferences(anyString(), anyString())).thenAnswer(call -> {
            final String prefix = call.getArgument(1);
            objects.remove(prefix);
            return true;
        });
        return remote;
    }
}

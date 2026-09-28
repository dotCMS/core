package com.dotmarketing.webdav;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.binary.BinaryAssetStorageAPI;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.fileassets.business.FileAsset;
import com.dotmarketing.portlets.fileassets.business.FileAssetAPI;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WebdavAssetStorageTest {
    @Test
    void uploadsWithTheSameNameHaveIndependentStagingFiles() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var locator = mockStatic(APILocator.class)) {
            final var files = mock(FileAssetAPI.class);
            when(files.getRealAssetPathTmpBinary()).thenReturn(root.toString());
            locator.when(APILocator::getFileAssetAPI).thenReturn(files);
            final var helper = mock(DotWebdavHelper.class, CALLS_REAL_METHODS);
            final var field = mock(com.dotmarketing.portlets.structure.model.Field.class);
            when(field.getFieldContentlet()).thenReturn("asset");
            Config.setProperty(AssetStorageFeature.FLAG, true);
            final var first = helper.createFileInTemporalFolder(field, "same-user", "Mixed-Name.TXT");
            Files.writeString(first.toPath(), "first request");
            final var second = helper.createFileInTemporalFolder(field, "same-user", "Mixed-Name.TXT");
            Files.writeString(second.toPath(), "second request");
            assertNotEquals(first, second);
            assertEquals(first.getName(), second.getName());
            assertEquals("first request", Files.readString(first.toPath()));
            assertEquals("second request", Files.readString(second.toPath()));
            assertThrows(IOException.class, () -> helper.createFileInTemporalFolder(field, "user", "../escape"));

            Config.setProperty(AssetStorageFeature.FLAG, false);
            final var legacy = helper.createFileInTemporalFolder(field, "same-user", "Mixed-Name.TXT");
            Files.writeString(legacy.toPath(), "legacy staging");
            assertEquals(legacy, helper.createFileInTemporalFolder(field, "same-user", "Mixed-Name.TXT"));
            assertFalse(legacy.exists(), "Disabled mode retains the existing staging behavior");
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }

    @Test
    void incompleteTemporaryUploadsDoNotReplaceCompleteFiles() throws Exception {
        try (var config = mockStatic(com.dotmarketing.util.ConfigUtils.class)) {
            config.when(com.dotmarketing.util.ConfigUtils::getAssetTempPath).thenReturn(root.toString());
            final var target = root.resolve("example.com/(Staging)/Mixed-Name.TXT");
            DotWebdavHelper.writeCompletedTempFile(target.toFile(), new ByteArrayInputStream("complete".getBytes()));
            final var interrupted = new java.io.InputStream() {
                private boolean first = true;
                @Override public int read() throws IOException {
                    if (first) { first = false; return 'x'; }
                    throw new IOException("interrupted upload");
                }
            };
            assertThrows(IOException.class, () -> DotWebdavHelper.writeCompletedTempFile(target.toFile(), interrupted));
            assertEquals("complete", Files.readString(target));
            final var absent = target.resolveSibling("Absent.TXT");
            assertThrows(IOException.class, () -> DotWebdavHelper.writeCompletedTempFile(absent.toFile(), interrupted));
            assertFalse(Files.exists(absent));
            try (var entries = Files.list(root)) {
                assertFalse(entries.anyMatch(p -> p.getFileName().toString().startsWith(".webdav-write-")));
            }
            assertThrows(IOException.class, () -> DotWebdavHelper.writeCompletedTempFile(
                    root.resolve("../outside.txt").toFile(), new ByteArrayInputStream(new byte[0])));
            final var link = root.resolve("link.txt");
            Files.createSymbolicLink(link, target);
            assertThrows(IOException.class, () -> DotWebdavHelper.writeCompletedTempFile(
                    link.toFile(), new ByteArrayInputStream(new byte[0])));
            assertEquals("complete", Files.readString(target));
        }
    }

    @TempDir Path root;

    @Test
    void fileAssetSnapshotDoesNotInvokeItsMetadataGeneratingMap() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        final var asset = new FileAsset() {
            @Override public Map<String, Object> getMap() {
                throw new IllegalStateException("FileAsset map calculates metadata");
            }
        };
        asset.setInode("abc123");
        try {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            assertEquals("abc123", new Contentlet(asset).getInode());
            Config.setProperty(AssetStorageFeature.FLAG, false);
            assertThrows(IllegalStateException.class, () -> new Contentlet(asset));
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }

    @Test
    void streamsAndHistoricalSizeChecksResolveUnderEvictionLease() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var locator = mockStatic(APILocator.class); var caches = mockStatic(CacheLocator.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, true);
            final var storage = mock(BinaryAssetStorageAPI.class);
            final var held = new AtomicInteger();
            when(storage.acquireCacheLease()).thenAnswer(call -> {
                held.incrementAndGet();
                return (BinaryAssetStorageAPI.CacheLease) held::decrementAndGet;
            });
            locator.when(APILocator::getBinaryAssetStorageAPI).thenReturn(storage);
            final var language = mock(LanguageAPI.class);
            when(language.getDefaultLanguage()).thenReturn(new Language());
            locator.when(APILocator::getLanguageAPI).thenReturn(language);
            locator.when(APILocator::getFileAssetAPI).thenReturn(mock(FileAssetAPI.class));
            final Path binary = Files.writeString(root.resolve("Hero-Mixed.GIF"), "original bytes");
            final FileAsset asset = mock(FileAsset.class);
            when(asset.getBinary(FileAssetAPI.BINARY_FIELD)).thenAnswer(call -> {
                assertTrue(held.get() > 0, "Resolve and open/stat must share the eviction lease");
                return binary.toFile();
            });
            doCallRealMethod().when(asset).getBinaryStream(FileAssetAPI.BINARY_FIELD);
            try (var input = asset.getBinaryStream(FileAssetAPI.BINARY_FIELD)) {
                assertEquals(0, held.get(), "The open descriptor can be consumed after releasing the lease");
                Files.delete(binary);
                assertEquals("original bytes", new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
            Files.writeString(binary, "restored bytes");
            final var resource = new FileResourceImpl(asset, "/webdav/working/1/example.com/Hero-Mixed.GIF");
            final var output = new ByteArrayOutputStream();
            resource.sendContent(output, null, Map.of(), null);
            assertEquals("restored bytes", output.toString(java.nio.charset.StandardCharsets.UTF_8));
            assertEquals(Files.size(binary), resource.getContentLength());
            assertTrue(DotWebdavHelper.storedBinaryLength(asset) > 0);
            Files.write(binary, new byte[0]);
            assertEquals(0, DotWebdavHelper.storedBinaryLength(asset));
            Files.delete(binary);
            assertThrows(IOException.class, () -> DotWebdavHelper.storedBinaryLength(asset));
            assertThrows(DotRuntimeException.class, resource::getContentLength);
            assertEquals(0, held.get(), "Failures must also release the eviction lease");
            doReturn(null).when(asset).getBinary(FileAssetAPI.BINARY_FIELD);
            assertEquals(-1, DotWebdavHelper.storedBinaryLength(asset), "Missing storage cannot justify deleting a version");
            assertThrows(IOException.class, () -> asset.getBinaryStream(FileAssetAPI.BINARY_FIELD));

            final var stream = spy(new ByteArrayInputStream(new byte[] {1, 2, 3}));
            doReturn(stream).when(asset).getBinaryStream(FileAssetAPI.BINARY_FIELD);
            assertThrows(IOException.class, () -> resource.sendContent(new OutputStream() {
                @Override public void write(int value) throws IOException { throw new IOException("disconnected"); }
            }, null, Map.of(), null));
            verify(stream).close();
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }

    @Test
    void disabledBinaryStreamRetainsFilesystemBehaviorWithoutStorageInitialization() throws Exception {
        final String previous = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        try (var locator = mockStatic(APILocator.class)) {
            Config.setProperty(AssetStorageFeature.FLAG, false);
            final var content = new Contentlet();
            final Path source = Files.writeString(root.resolve("NFS-Mixed.TXT"), "shared filesystem");
            content.getMap().put(FileAssetAPI.BINARY_FIELD, source.toFile());
            try (var input = content.getBinaryStream(FileAssetAPI.BINARY_FIELD)) {
                assertEquals("shared filesystem", new String(input.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8));
            }
            locator.verifyNoInteractions();
        } finally {
            Config.setProperty(AssetStorageFeature.FLAG, previous);
        }
    }
}

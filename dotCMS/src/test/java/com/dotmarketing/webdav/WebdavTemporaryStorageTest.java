package com.dotmarketing.webdav;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bradmcevoy.http.Resource;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.ObjectSnapshot;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotcms.storage.WebdavTemporaryStorage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.portlets.languagesmanager.business.LanguageAPI;
import com.dotmarketing.portlets.languagesmanager.model.Language;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Runs the S3-backed WebDAV temporary-file storage against an in-memory remote that models
 * conditional writes, so its listing behavior is covered without MinIO.
 */
class WebdavTemporaryStorageTest {
    @TempDir Path root;
    private String previousFlag;
    private final Map<String, ObjectSnapshot> objects = new TreeMap<>();
    private final List<String> reads = new ArrayList<>();

    @BeforeEach
    void enableFlag() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
    }

    @AfterEach
    void restoreFlag() {
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    /** Listing a folder reads each direct child's own record, and descendants only for an implicit folder. */
    @Test
    void childrenReadsOnlyTheRecordsThatDecideEachDirectChild() throws Exception {
        final var remote = memoryRemote();
        final var storage = new WebdavTemporaryStorage(remote, root);
        storage.mkdir(file("example.com/folder/.Trash"));
        storage.store(file("example.com/folder/.Trash/a.txt"), payload("a"));
        storage.store(file("example.com/folder/.Trash/deep/b.txt"), payload("b"));
        storage.store(file("example.com/folder/._Hero.GIF"), payload("hero"));
        storage.store(file("example.com/folder/(implicit)/c.txt"), payload("c"));
        storage.store(file("example.com/folder/._Gone"), payload("gone"));
        storage.delete(file("example.com/folder/._Gone"));
        storage.store(file("example.com/other/._Unrelated"), payload("other"));
        reads.clear();

        final var children = storage.children(file("example.com/folder"));

        assertEquals(List.of("example.com/folder/(implicit)", "example.com/folder/.Trash", "example.com/folder/._Hero.GIF"),
                children.stream().map(WebdavTemporaryStorage.Entry::path).toList());
        assertEquals(List.of(true, true, false), children.stream().map(WebdavTemporaryStorage.Entry::directory).toList());
        assertEquals(4, children.get(2).size());
        // .Trash, ._Hero.GIF and the ._Gone tombstone have their own records. The implicit folder
        // has none, so one live descendant is read to show it exists.
        assertEquals(4, reads.size(), "Unexpected record reads: " + reads);
        assertTrue(reads.stream().noneMatch(key -> key.contains("Trash/a") || key.contains("Trash/deep")),
                "Records below a live folder must not be read: " + reads);
        assertTrue(reads.stream().noneMatch(key -> key.contains("other")), "Unrelated records must not be read");
    }

    /** A temporary file deleted mid-listing, or an S3 failure, must not fail the containing folder listing. */
    @Test
    void temporaryChildrenSurviveVanishingFilesAndLeaveOutUnreadableOnes() throws Exception {
        final var remote = memoryRemote();
        final var storage = new WebdavTemporaryStorage(remote, root);
        storage.store(file("example.com/folder/._Hero.GIF"), payload("hero"));
        storage.store(file("example.com/folder/._Vanished"), payload("vanished"));
        // ._Vanished is deleted between the listing and its read. ._Hero.GIF is deleted right after
        // its first read, which is when a second lookup per file used to fail the whole listing.
        reads.clear();
        doAnswer(call -> {
            final String key = call.getArgument(1);
            final boolean readBefore = reads.contains(key);
            reads.add(key);
            return key.contains("Vanished") || (key.contains("Hero") && readBefore) ? null : objects.get(key);
        }).when(remote).readObjectSnapshot(anyString(), anyString(), any());

        try (var locator = mockStatic(APILocator.class); var caches = mockStatic(CacheLocator.class);
             var paths = mockStatic(ConfigUtils.class, CALLS_REAL_METHODS);
             var singleton = mockStatic(WebdavTemporaryStorage.class)) {
            final var language = mock(LanguageAPI.class);
            when(language.getDefaultLanguage()).thenReturn(new Language());
            locator.when(APILocator::getLanguageAPI).thenReturn(language);
            paths.when(ConfigUtils::getAssetTempPath).thenReturn(root.toString());
            singleton.when(WebdavTemporaryStorage::getInstance).thenReturn(storage);
            final var helper = new DotWebdavHelper();

            final List<Resource> listed = helper.temporaryChildren("/example.com/folder", false);
            assertEquals(List.of("._Hero.GIF"), listed.stream().map(Resource::getName).toList());
            assertEquals(4L, ((TempFileResourceImpl) listed.get(0)).getContentLength());

            doThrow(new DotDataException("S3 unavailable")).when(remote).listObjectPaths(anyString(), anyString());
            assertTrue(helper.temporaryChildren("/example.com/folder", false).isEmpty(),
                    "An S3 failure leaves out only the temporary children");
        }
    }

    private File file(final String key) {
        return root.resolve(key).toFile();
    }

    private File payload(final String content) throws Exception {
        return Files.writeString(Files.createTempFile(root, "payload-", ".tmp"), content).toFile();
    }

    /** A remote whose records carry versions, so conditional writes and tombstones behave as on S3. */
    private StoragePersistenceAPI memoryRemote() throws Exception {
        final var remote = mock(StoragePersistenceAPI.class);
        when(remote.listObjectPaths(anyString(), anyString())).thenAnswer(call -> objects.keySet().stream()
                .filter(key -> key.startsWith(call.<String>getArgument(1))).toList());
        when(remote.readObjectSnapshot(anyString(), anyString(), any())).thenAnswer(call -> {
            reads.add(call.getArgument(1));
            return objects.get(call.<String>getArgument(1));
        });
        when(remote.writeObjectIfMatch(anyString(), anyString(), any(), any())).thenAnswer(call -> {
            final String key = call.getArgument(1);
            final ObjectSnapshot current = objects.get(key);
            if (!Objects.equals(current == null ? null : current.version(), call.getArgument(3))) return null;
            final String version = UUID.randomUUID().toString();
            objects.put(key, new ObjectSnapshot(key, call.getArgument(2), version, System.currentTimeMillis()));
            return version;
        });
        when(remote.backfillFile(anyString(), anyString(), any())).thenAnswer(call -> {
            final String key = call.getArgument(1);
            objects.put(key, new ObjectSnapshot(key, Files.readString(call.<File>getArgument(2).toPath()),
                    UUID.randomUUID().toString(), System.currentTimeMillis()));
            return true;
        });
        when(remote.deleteObjectAndReferences(anyString(), anyString())).thenAnswer(call ->
                objects.remove(call.<String>getArgument(1)) != null);
        return remote;
    }
}

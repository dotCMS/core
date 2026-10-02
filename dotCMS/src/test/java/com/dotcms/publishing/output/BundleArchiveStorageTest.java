package com.dotcms.publishing.output;

import com.dotcms.jobs.business.job.Job;
import com.dotcms.storage.AssetStorageFeature;
import com.dotcms.storage.ObjectSnapshot;
import com.dotcms.storage.StoragePersistenceAPI;
import com.dotmarketing.common.db.DotConnect;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.ConfigUtils;
import com.google.common.collect.ImmutableMap;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the flag-on behavior of {@link BundleArchiveStorage} and
 * {@link BundleArchiveCleanupProcessor}, using a mocked storage chain instead of S3.
 */
class BundleArchiveStorageTest {

    @TempDir Path root;
    private String previousFlag;
    private MockedStatic<ConfigUtils> paths;
    private StoragePersistenceAPI provider;
    private BundleArchiveStorage archives;

    @BeforeEach
    void enable() {
        previousFlag = Config.getStringProperty(AssetStorageFeature.FLAG, null);
        Config.setProperty(AssetStorageFeature.FLAG, true);
        paths = mockStatic(ConfigUtils.class);
        paths.when(ConfigUtils::getBundlePath).thenReturn(root.toString());
        provider = mock(StoragePersistenceAPI.class);
        archives = new BundleArchiveStorage(provider);
    }

    @AfterEach
    void restore() {
        paths.close();
        Config.setProperty(AssetStorageFeature.FLAG, previousFlag);
    }

    /** A received file is stored under the id the receivers derive: the name before the first ".tar.gz". */
    @Test
    void receiveStoresUnderTheIdTheReadersUse() throws Exception {
        archives.receive("release.tar.gz-v2.tar.gz", new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)));
        verify(provider).pushFile(eq(BundleArchiveStorage.GROUP), eq("release.tar.gz"), any(File.class), anyMap());

        archives.receive("x.tar.gzip", new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8)));
        verify(provider).pushFile(eq(BundleArchiveStorage.GROUP), eq("x.tar.gz"), any(File.class), anyMap());

        assertThrows(IllegalArgumentException.class,
                () -> archives.receive("bundle.zip", new ByteArrayInputStream(new byte[0])));
    }

    /** The page check reports an S3 failure as a missing archive; the publishing check still fails. */
    @Test
    void displayCheckToleratesStorageFailure() throws Exception {
        when(provider.existsObject(anyString(), anyString())).thenThrow(new DotDataException("S3 unavailable"));
        assertThrows(RuntimeException.class, () -> archives.exists("Some-Bundle"));
        assertFalse(archives.existsForDisplay("Some-Bundle"));
        assertFalse(archives.existsForDisplay("../outside"), "An invalid id is shown as missing, not thrown");
    }

    /** Only archives stored strictly before the cutoff are expired, and one failure does not stop the rest. */
    @Test
    void expiresOnlyArchivesOlderThanTheCutoff() throws Exception {
        final long cutoff = Instant.parse("2026-09-01T00:00:00Z").toEpochMilli();
        when(provider.listObjectSnapshots(BundleArchiveStorage.GROUP, "")).thenReturn(List.of(
                new ObjectSnapshot("Failing-Bundle.tar.gz", null, "e1", cutoff - 10),
                new ObjectSnapshot("Old-Bundle.tar.gz", null, "e2", cutoff - 1),
                new ObjectSnapshot("Exact-Bundle.tar.gz", null, "e3", cutoff),
                new ObjectSnapshot("New-Bundle.tar.gz", null, "e4", cutoff + 1000),
                new ObjectSnapshot("notes.txt", null, "e5", cutoff - 1),
                new ObjectSnapshot("nested/Inner.tar.gz", null, "e6", cutoff - 1)));
        when(provider.deleteObjectAndReferences(BundleArchiveStorage.GROUP, "Failing-Bundle.tar.gz"))
                .thenThrow(new DotDataException("S3 unavailable"));

        assertEquals(1, archives.expireOlderThan(Instant.ofEpochMilli(cutoff)));

        verify(provider).deleteObjectAndReferences(BundleArchiveStorage.GROUP, "Failing-Bundle.tar.gz");
        verify(provider).deleteObjectAndReferences(BundleArchiveStorage.GROUP, "Old-Bundle.tar.gz");
        verify(provider, never()).deleteObjectAndReferences(BundleArchiveStorage.GROUP, "Exact-Bundle.tar.gz");
        verify(provider, never()).deleteObjectAndReferences(BundleArchiveStorage.GROUP, "New-Bundle.tar.gz");
        verify(provider, never()).deleteObjectAndReferences(BundleArchiveStorage.GROUP, "notes.txt");
        verify(provider, never()).deleteObjectAndReferences(BundleArchiveStorage.GROUP, "nested/Inner.tar.gz");
    }

    /** With the flag off, expiry touches no storage at all. */
    @Test
    void disabledFlagExpiresNothing() throws Exception {
        Config.setProperty(AssetStorageFeature.FLAG, false);
        assertEquals(0, archives.expireOlderThan(Instant.now()));
        verifyNoInteractions(provider);
    }

    /** Storing an archive waits while the cleanup job holds the bundle's archive lock. */
    @Test
    void storeWaitsForTheArchiveLock() throws Exception {
        final File source = root.resolve("source.tmp").toFile();
        assertTrue(source.createNewFile());
        final CountDownLatch locked = new CountDownLatch(1);
        final AtomicBoolean pushedWhileLocked = new AtomicBoolean(true);
        // The holder runs on its own thread, because the static ConfigUtils mock only applies to this one.
        final CompletableFuture<Void> holder = CompletableFuture.runAsync(() -> {
            final Lock lock = archives.archiveLock("Locked-Bundle");
            lock.lock();
            try {
                locked.countDown();
                Thread.sleep(300);
                pushedWhileLocked.set(mockingDetails(provider).getInvocations().stream()
                        .anyMatch(call -> call.getMethod().getName().equals("pushFile")));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        });
        assertTrue(locked.await(10, TimeUnit.SECONDS));
        archives.store("Locked-Bundle", source);
        holder.get(10, TimeUnit.SECONDS);
        assertFalse(pushedWhileLocked.get(), "Store must not run while the lock is held");
        verify(provider).pushFile(eq(BundleArchiveStorage.GROUP), eq("Locked-Bundle.tar.gz"), eq(source), anyMap());
    }

    /** A bundle that exists again keeps its archive, and the job completes instead of failing. */
    @Test
    void cleanupKeepsTheArchiveOfABundleThatExistsAgain() throws Exception {
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of(Map.of("id", "Again"))));
             var instance = mockStatic(BundleArchiveStorage.class, CALLS_REAL_METHODS)) {
            instance.when(BundleArchiveStorage::getInstance).thenReturn(archives);
            assertDoesNotThrow(() -> new BundleArchiveCleanupProcessor().process(job("Again")));
        }
        verify(provider, never()).deleteObjectAndReferences(anyString(), anyString());
    }

    /** A bundle that is gone has its archive deleted. */
    @Test
    void cleanupDeletesTheArchiveOfADeletedBundle() throws Exception {
        try (var queries = mockConstruction(DotConnect.class, withSettings().defaultAnswer(RETURNS_SELF),
                     (query, context) -> when(query.loadObjectResults()).thenReturn(List.of()));
             var instance = mockStatic(BundleArchiveStorage.class, CALLS_REAL_METHODS)) {
            instance.when(BundleArchiveStorage::getInstance).thenReturn(archives);
            new BundleArchiveCleanupProcessor().process(job("Gone"));
        }
        verify(provider).deleteObjectAndReferences(BundleArchiveStorage.GROUP, "Gone.tar.gz");
    }

    private Job job(final String bundleId) {
        final Job job = mock(Job.class);
        when(job.id()).thenReturn("bundle-cleanup-test");
        when(job.parameters()).thenReturn(ImmutableMap.of("bundleId", bundleId));
        return job;
    }
}

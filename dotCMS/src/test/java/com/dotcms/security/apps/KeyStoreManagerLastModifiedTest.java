package com.dotcms.security.apps;

import com.dotmarketing.util.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Specialized test class focusing specifically on KeyStoreManager's lastModified timestamp behavior.
 * This test ensures that the auto-reload mechanism works correctly in various scenarios.
 */
class KeyStoreManagerLastModifiedTest {

    @TempDir
    Path tempDir;

    private KeyStoreManager keyStoreManager;
    private File testKeyStoreFile;
    private final char[] testPassword = "testPassword123".toCharArray();

    @BeforeEach
    void setUp() throws Exception {
        testKeyStoreFile = tempDir.resolve("testKeyStore.p12").toFile();

        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class)) {
            configMock.when(() -> Config.getStringProperty(anyString(), anyString()))
                     .thenReturn(testKeyStoreFile.getAbsolutePath());

            createKeyStoreFile(testKeyStoreFile, testPassword);
            keyStoreManager = new KeyStoreManager();
        }
    }

    @Test
    void testLastModifiedTrackingAccuracy() throws Exception {
        // Record initial file timestamp
        long initialTimestamp = testKeyStoreFile.lastModified();
        assertTrue(initialTimestamp > 0, "Initial timestamp should be valid");

        // First load - should cache with correct timestamp
        KeyStore ks1 = keyStoreManager.getKeyStore();
        assertNotNull(ks1);

        // Verify cache hit (same timestamp)
        KeyStore ks1Cache = keyStoreManager.getKeyStore();
        assertSame(ks1, ks1Cache, "Should return cached instance for same timestamp");

        // Wait and modify file
        Thread.sleep(1100); // Ensure different timestamp (some filesystems have 1-second resolution)
        touchFile(testKeyStoreFile);

        long newTimestamp = testKeyStoreFile.lastModified();
        assertTrue(newTimestamp > initialTimestamp,
                  String.format("New timestamp (%d) should be greater than initial (%d)",
                               newTimestamp, initialTimestamp));

        // Load should detect change and reload
        KeyStore ks2 = keyStoreManager.getKeyStore();
        assertNotNull(ks2);
        assertNotSame(ks1, ks2, "Should create new instance when timestamp changes");

        // Subsequent loads should use cache again
        KeyStore ks2Cache = keyStoreManager.getKeyStore();
        assertSame(ks2, ks2Cache, "Should return cached instance after reload");
    }

    @Test
    void testTimestampComparisonEdgeCases() throws Exception {
        // Load initial KeyStore
        KeyStore ks1 = keyStoreManager.getKeyStore();

        // Test case 1: File deleted (timestamp becomes 0)
        Files.delete(testKeyStoreFile.toPath());
        KeyStore ks2 = keyStoreManager.getKeyStore();
        assertNotSame(ks1, ks2, "Should reload when file is deleted");

        // Test case 2: File recreated with new timestamp
        createKeyStoreFile(testKeyStoreFile, testPassword);
        Thread.sleep(1100);
        KeyStore ks3 = keyStoreManager.getKeyStore();
        assertNotSame(ks2, ks3, "Should reload when file is recreated");

        // Test case 3: Timestamp set to past (should still trigger reload)
        long oldTimestamp = System.currentTimeMillis() - 10000; // 10 seconds ago
        testKeyStoreFile.setLastModified(oldTimestamp);
        KeyStore ks4 = keyStoreManager.getKeyStore();
        assertNotSame(ks3, ks4, "Should reload even when timestamp goes backwards");
    }

    @Test
    void testConcurrentAccessDuringReload() throws Exception {
        // Initial load
        KeyStore initialKs = keyStoreManager.getKeyStore();

        // Create multiple threads that will access KeyStore
        int numThreads = 5;
        List<CompletableFuture<KeyStore>> futures = new ArrayList<>();

        // Modify file to trigger reload
        Thread.sleep(1100);
        touchFile(testKeyStoreFile);

        // Launch concurrent access attempts
        for (int i = 0; i < numThreads; i++) {
            CompletableFuture<KeyStore> future = CompletableFuture.supplyAsync(() -> {
                return keyStoreManager.getKeyStore();
            });
            futures.add(future);
        }

        // Wait for all to complete
        List<KeyStore> results = new ArrayList<>();
        for (CompletableFuture<KeyStore> future : futures) {
            KeyStore ks = future.get(5, TimeUnit.SECONDS);
            assertNotNull(ks);
            results.add(ks);
        }

        // All threads should get the same reloaded instance
        KeyStore reloadedKs = results.get(0);
        assertNotSame(initialKs, reloadedKs, "Should be different from initial KeyStore");

        for (KeyStore ks : results) {
            assertSame(reloadedKs, ks, "All concurrent accesses should get same reloaded instance");
        }
    }

    @Test
    void testReloadCountOptimization() throws Exception {
        // Track how many times KeyStore is actually loaded vs cached
        int loadOperations = 10;
        long initialTimestamp = testKeyStoreFile.lastModified();

        // Multiple accesses without file modification should use cache
        KeyStore firstLoad = keyStoreManager.getKeyStore();
        for (int i = 0; i < loadOperations - 1; i++) {
            KeyStore cached = keyStoreManager.getKeyStore();
            assertSame(firstLoad, cached,
                      "Access " + (i + 2) + " should return cached instance");
        }

        // Now modify file and verify reload happens only once
        Thread.sleep(1100);
        touchFile(testKeyStoreFile);
        assertTrue(testKeyStoreFile.lastModified() > initialTimestamp);

        KeyStore reloaded = keyStoreManager.getKeyStore();
        assertNotSame(firstLoad, reloaded, "Should reload after file modification");

        // Subsequent accesses should use the reloaded cache
        for (int i = 0; i < loadOperations; i++) {
            KeyStore cached = keyStoreManager.getKeyStore();
            assertSame(reloaded, cached,
                      "Post-reload access " + (i + 1) + " should return cached instance");
        }
    }

    @Test
    void testSaveOperationUpdatesTimestampTracking() throws Exception {
        // Initial load
        KeyStore ks1 = keyStoreManager.getKeyStore();
        long timestamp1 = testKeyStoreFile.lastModified();

        // Wait and save KeyStore (this will update file and timestamp)
        Thread.sleep(1100);
        keyStoreManager.saveKeyStore(ks1);

        long timestamp2 = testKeyStoreFile.lastModified();
        assertTrue(timestamp2 > timestamp1, "Save operation should update file timestamp");

        // Access should NOT trigger reload since KeyStoreManager saved it
        KeyStore ks2 = keyStoreManager.getKeyStore();
        assertSame(ks1, ks2, "Should return same instance after own save operation");

        // External modification should still trigger reload
        Thread.sleep(1100);
        touchFile(testKeyStoreFile);

        KeyStore ks3 = keyStoreManager.getKeyStore();
        assertNotSame(ks1, ks3, "Should reload after external file modification");
    }

    @Test
    void testCacheInvalidationResetsTimestampTracking() throws Exception {
        // Load KeyStore
        KeyStore ks1 = keyStoreManager.getKeyStore();
        long originalTimestamp = testKeyStoreFile.lastModified();

        // Invalidate cache
        keyStoreManager.invalidateCache();

        // Next access should reload even though file hasn't changed
        KeyStore ks2 = keyStoreManager.getKeyStore();
        assertNotSame(ks1, ks2, "Should reload after cache invalidation");

        // Verify it still tracks timestamp correctly after invalidation
        KeyStore ks2Cache = keyStoreManager.getKeyStore();
        assertSame(ks2, ks2Cache, "Should cache after reload post-invalidation");

        // File modification should still work correctly
        Thread.sleep(1100);
        touchFile(testKeyStoreFile);

        KeyStore ks3 = keyStoreManager.getKeyStore();
        assertNotSame(ks2, ks3, "Should still detect file changes after cache invalidation cycle");
    }

    // Helper methods
    private void createKeyStoreFile(File file, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password);

        file.getParentFile().mkdirs();
        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            keyStore.store(fos, password);
        }
    }

    private void touchFile(File file) throws Exception {
        // Update the file's last modified time by writing the same content back
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var inputStream = Files.newInputStream(file.toPath())) {
            keyStore.load(inputStream, testPassword);
        }

        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            keyStore.store(fos, testPassword);
        }
    }
}
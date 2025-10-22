package com.dotcms.security.apps;

import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Test class for KeyStoreManager focusing on lastModified functionality and auto-reload behavior.
 */
class KeyStoreManagerTest {

    @TempDir
    Path tempDir;

    private KeyStoreManager keyStoreManager;
    private File testKeyStoreFile;
    private char[] testPassword = "testPassword123".toCharArray();

    @BeforeEach
    void setUp() throws Exception {
        // Create test keystore file
        testKeyStoreFile = tempDir.resolve("testKeyStore.p12").toFile();

        // Mock Config to return our test path
        try (MockedStatic<Config> configMock = Mockito.mockStatic(Config.class)) {
            configMock.when(() -> Config.getStringProperty(anyString(), anyString()))
                     .thenReturn(testKeyStoreFile.getAbsolutePath());

            // Create initial keystore file
            createInitialKeyStore(testKeyStoreFile, testPassword);

            // Initialize KeyStoreManager
            keyStoreManager = new KeyStoreManager();
        }
    }

    @Test
    void testInitialLoad() {
        // Test that initial load works
        KeyStore keyStore = keyStoreManager.getKeyStore();
        assertNotNull(keyStore);
        assertEquals("PKCS12", keyStore.getType());
    }

    @Test
    void testCacheHitWhenFileUnchanged() {
        // First load
        KeyStore keyStore1 = keyStoreManager.getKeyStore();
        assertNotNull(keyStore1);

        // Second load should return the same instance (cache hit)
        KeyStore keyStore2 = keyStoreManager.getKeyStore();
        assertSame(keyStore1, keyStore2, "Should return same cached instance when file unchanged");
    }

    @Test
    void testAutoReloadOnFileModification() throws Exception {
        // First load
        KeyStore keyStore1 = keyStoreManager.getKeyStore();
        assertNotNull(keyStore1);

        // Wait a bit to ensure different timestamp
        Thread.sleep(1000);

        // Modify the keystore file (simulate external change)
        modifyKeyStoreFile(testKeyStoreFile, testPassword, "newTestEntry", "newTestValue");

        // Next load should detect file change and reload
        KeyStore keyStore2 = keyStoreManager.getKeyStore();
        assertNotNull(keyStore2);

        // Should be a different instance due to reload
        assertNotSame(keyStore1, keyStore2, "Should reload and return new instance when file modified");

        // Verify the new entry is present
        assertTrue(keyStore2.containsAlias("newTestEntry"), "New entry should be present after reload");
    }

    @Test
    void testConcurrentAccessWithFileModification() throws Exception {
        final int numThreads = 10;
        final ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishLatch = new CountDownLatch(numThreads);
        final AtomicInteger loadCount = new AtomicInteger(0);
        final AtomicReference<Exception> exception = new AtomicReference<>();

        // Start multiple threads that will access KeyStore
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Each thread accesses KeyStore multiple times
                    for (int j = 0; j < 5; j++) {
                        KeyStore ks = keyStoreManager.getKeyStore();
                        assertNotNull(ks);
                        loadCount.incrementAndGet();
                        Thread.sleep(10);
                    }
                } catch (Exception e) {
                    exception.set(e);
                } finally {
                    finishLatch.countDown();
                }
            });
        }

        // Modify file while threads are accessing
        Thread.sleep(100);
        modifyKeyStoreFile(testKeyStoreFile, testPassword, "concurrentTestEntry", "concurrentTestValue");

        // Release all threads
        startLatch.countDown();

        // Wait for completion
        assertTrue(finishLatch.await(10, TimeUnit.SECONDS));
        executor.shutdown();

        // Verify no exceptions occurred
        assertNull(exception.get(), "No exceptions should occur during concurrent access");

        // Verify total load count
        assertEquals(numThreads * 5, loadCount.get(), "All loads should complete successfully");

        // Verify final state contains the concurrent modification
        KeyStore finalKs = keyStoreManager.getKeyStore();
        assertTrue(finalKs.containsAlias("concurrentTestEntry"),
                  "Final KeyStore should contain entry added during concurrent access");
    }

    @Test
    void testCacheInvalidation() {
        // Load initial KeyStore
        KeyStore keyStore1 = keyStoreManager.getKeyStore();
        assertNotNull(keyStore1);

        // Invalidate cache
        keyStoreManager.invalidateCache();

        // Next load should create new instance
        KeyStore keyStore2 = keyStoreManager.getKeyStore();
        assertNotNull(keyStore2);
        assertNotSame(keyStore1, keyStore2, "Should create new instance after cache invalidation");
    }

    @Test
    void testSaveKeyStoreUpdatesCache() throws Exception {
        // Load initial KeyStore
        KeyStore originalKs = keyStoreManager.getKeyStore();
        int originalSize = originalKs.size();

        // Create a new KeyStore with additional entry
        KeyStore modifiedKs = KeyStore.getInstance("PKCS12");
        modifiedKs.load(null, testPassword);

        // Add an entry
        javax.crypto.spec.PBEKeySpec keySpec = new javax.crypto.spec.PBEKeySpec("testSecret".toCharArray());
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBE");
        javax.crypto.SecretKey secretKey = factory.generateSecret(keySpec);

        KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(testPassword);
        modifiedKs.setEntry("testEntry", new KeyStore.SecretKeyEntry(secretKey), protection);

        // Save using KeyStoreManager
        keyStoreManager.saveKeyStore(modifiedKs);

        // Verify cache was updated (should return the saved KeyStore)
        KeyStore cachedKs = keyStoreManager.getKeyStore();
        assertTrue(cachedKs.containsAlias("testEntry"), "Saved KeyStore should be in cache");
        assertEquals(originalSize + 1, cachedKs.size(), "Cache should reflect the saved changes");
    }

    @Test
    void testFileTimestampDetection() throws Exception {
        // Get initial timestamp
        long initialTimestamp = testKeyStoreFile.lastModified();
        KeyStore ks1 = keyStoreManager.getKeyStore();

        // Wait to ensure different timestamp
        Thread.sleep(1000);

        // Touch file to change timestamp without changing content
        assertTrue(testKeyStoreFile.setLastModified(System.currentTimeMillis()));
        long newTimestamp = testKeyStoreFile.lastModified();
        assertTrue(newTimestamp > initialTimestamp, "File timestamp should be updated");

        // Access should trigger reload due to timestamp change
        KeyStore ks2 = keyStoreManager.getKeyStore();
        assertNotSame(ks1, ks2, "Should reload due to timestamp change even with same content");
    }

    @Test
    void testNonExistentFileHandling() throws IOException {
        // Delete the test file
        Files.delete(testKeyStoreFile.toPath());
        assertFalse(testKeyStoreFile.exists());

        // KeyStoreManager should handle this gracefully and create new KeyStore
        assertDoesNotThrow(() -> {
            KeyStore ks = keyStoreManager.getKeyStore();
            assertNotNull(ks);
        });

        // File should be recreated
        assertTrue(testKeyStoreFile.exists(), "KeyStore file should be recreated");
    }

    @Test
    void testDestroyFunctionality() {
        // Ensure file exists
        assertTrue(testKeyStoreFile.exists());

        // Load KeyStore first
        KeyStore ks1 = keyStoreManager.getKeyStore();
        assertNotNull(ks1);

        // Destroy
        keyStoreManager.destroy();

        // File should be deleted and cache cleared
        assertFalse(testKeyStoreFile.exists(), "KeyStore file should be deleted");

        // Next access should recreate file and KeyStore
        KeyStore ks2 = keyStoreManager.getKeyStore();
        assertNotNull(ks2);
        assertNotSame(ks1, ks2, "Should create new instance after destroy");
        assertTrue(testKeyStoreFile.exists(), "KeyStore file should be recreated");
    }

    // Helper method to create initial KeyStore file
    private void createInitialKeyStore(File file, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password);

        // Ensure parent directory exists
        file.getParentFile().mkdirs();

        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            keyStore.store(fos, password);
        }
    }

    // Helper method to modify KeyStore file
    private void modifyKeyStoreFile(File file, char[] password, String entryName, String entryValue) throws Exception {
        // Load existing KeyStore
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (var inputStream = Files.newInputStream(file.toPath())) {
            keyStore.load(inputStream, password);
        }

        // Add new entry
        javax.crypto.spec.PBEKeySpec keySpec = new javax.crypto.spec.PBEKeySpec(entryValue.toCharArray());
        javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance("PBE");
        javax.crypto.SecretKey secretKey = factory.generateSecret(keySpec);

        KeyStore.PasswordProtection protection = new KeyStore.PasswordProtection(password);
        keyStore.setEntry(entryName, new KeyStore.SecretKeyEntry(secretKey), protection);

        // Save back to file
        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            keyStore.store(fos, password);
        }
    }
}
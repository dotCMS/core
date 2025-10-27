package com.dotcms.security.apps;

import com.dotcms.cdi.CDIUtils;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Integration test demonstrating the complete workflow:
 * SecretsKeyStoreHelper -> CDIUtils -> KeyStoreManager -> Auto-reload functionality
 */
public class KeyStoreManagerIntegrationTest {

    @TempDir
    Path tempDir;

    private File testKeyStoreFile;
    private final char[] testPassword = "integrationTestPassword".toCharArray();

    @BeforeEach
    void setUp() throws Exception {
        testKeyStoreFile = tempDir.resolve("integrationKeyStore.p12").toFile();
        createInitialKeyStore(testKeyStoreFile, testPassword);
    }

    @Test
    void testCompleteWorkflowWithAutoReload() throws Exception {
        KeyStoreManager mockManager = Mockito.spy(new TestKeyStoreManager(testKeyStoreFile.getAbsolutePath()));

        try (MockedStatic<CDIUtils> cdiMock = Mockito.mockStatic(CDIUtils.class);
             MockedStatic<Config> configMock = Mockito.mockStatic(Config.class)) {

            // Mock CDI to return our test manager
            cdiMock.when(() -> CDIUtils.getBeanThrows(KeyStoreManager.class))
                   .thenReturn(mockManager);

            // Mock Config for password
            configMock.when(() -> Config.getStringProperty(anyString(), anyString()))
                     .thenAnswer(invocation -> {
                         String key = invocation.getArgument(0);
                         if (key.equals(SecretsKeyStoreHelper.SECRETS_KEYSTORE_PASSWORD_KEY)) {
                             return new String(testPassword);
                         }
                         return invocation.getArgument(1);
                     });

            // Create SecretsKeyStoreHelper (this will use CDI to get KeyStoreManager)
            SecretsKeyStoreHelper secretsHelper = new SecretsKeyStoreHelper();

            // Test 1: Initial operations
            secretsHelper.saveValue("testSecret1", "testValue1".toCharArray());
            char[] retrievedValue1 = secretsHelper.getValue("testSecret1");
            assertNotEquals(0, retrievedValue1.length, "Should retrieve saved secret");

            // Verify KeyStoreManager was called
            Mockito.verify(mockManager, Mockito.atLeastOnce()).getKeyStore();

            // Test 2: Simulate external file modification
            Thread.sleep(1100); // Ensure different timestamp
            modifyKeyStoreFileExternally(testKeyStoreFile, testPassword, "externalSecret", "externalValue");

            // Reset mock to track new calls
            Mockito.reset(mockManager);
            Mockito.when(mockManager.getKeyStore()).thenCallRealMethod();

            // Next operation should trigger auto-reload
            secretsHelper.saveValue("testSecret2", "testValue2".toCharArray());

            // Verify that getKeyStore was called (indicating reload happened)
            Mockito.verify(mockManager, Mockito.atLeastOnce()).getKeyStore();

            // Test 3: Verify both original and external data are accessible
            char[] retrievedValue2 = secretsHelper.getValue("testSecret2");
            assertNotEquals(0, retrievedValue2.length, "Should retrieve newly saved secret");

            // Verify external modification was detected and preserved
            KeyStore currentKs = mockManager.getKeyStore();
            assertTrue(currentKs.containsAlias("externalSecret"),
                      "KeyStore should contain externally added secret after auto-reload");

            // Test 4: Test size reflects all changes
            int finalSize = secretsHelper.size();
            assertTrue(finalSize >= 2, "KeyStore should contain at least the secrets we added");

            System.out.println("Integration test completed successfully:");
            System.out.println("- Auto-reload triggered by external file modification");
            System.out.println("- CDI integration working correctly");
            System.out.println("- All secrets accessible through SecretsKeyStoreHelper");
            System.out.println("- Final KeyStore size: " + finalSize);
        }
    }

    @Test
    void testConcurrentOperationsWithReload() throws Exception {
        KeyStoreManager mockManager = Mockito.spy(new TestKeyStoreManager(testKeyStoreFile.getAbsolutePath()));

        try (MockedStatic<CDIUtils> cdiMock = Mockito.mockStatic(CDIUtils.class);
             MockedStatic<Config> configMock = Mockito.mockStatic(Config.class)) {

            cdiMock.when(() -> CDIUtils.getBeanThrows(KeyStoreManager.class))
                   .thenReturn(mockManager);

            configMock.when(() -> Config.getStringProperty(anyString(), anyString()))
                     .thenAnswer(invocation -> {
                         String key = invocation.getArgument(0);
                         if (key.equals(SecretsKeyStoreHelper.SECRETS_KEYSTORE_PASSWORD_KEY)) {
                             return new String(testPassword);
                         }
                         return invocation.getArgument(1);
                     });

            // Create multiple SecretsKeyStoreHelper instances (simulating different components)
            SecretsKeyStoreHelper helper1 = new SecretsKeyStoreHelper();
            SecretsKeyStoreHelper helper2 = new SecretsKeyStoreHelper();

            // Concurrent operations
            helper1.saveValue("concurrent1", "value1".toCharArray());
            helper2.saveValue("concurrent2", "value2".toCharArray());

            // Modify file externally while operations are ongoing
            Thread.sleep(1100);
            modifyKeyStoreFileExternally(testKeyStoreFile, testPassword, "externalConcurrent", "externalValue");

            // Both helpers should see all changes after reload
            Thread.sleep(100); // Allow for reload

            char[] value1 = helper1.getValue("concurrent1");
            char[] value2 = helper2.getValue("concurrent2");

            assertNotEquals(0, value1.length, "Helper1 should see its own changes");
            assertNotEquals(0, value2.length, "Helper2 should see its own changes");

            // Both should see external changes due to auto-reload
            KeyStore ks1 = helper1.getSecretsStore();
            KeyStore ks2 = helper2.getSecretsStore();

            // They should be the same instance due to CDI singleton
            assertSame(ks1, ks2, "Both helpers should use the same CDI KeyStore instance");
            assertTrue(ks1.containsAlias("externalConcurrent"),
                      "Both helpers should see external changes");
        }
    }

    // Helper class for testing
    private static class TestKeyStoreManager extends KeyStoreManager {
        private final String keyStorePath;

        public TestKeyStoreManager(String keyStorePath) {
            super(null,null, null);
            this.keyStorePath = keyStorePath;
        }

        @Override
        public String getKeyStorePath() {
            return keyStorePath;
        }
    }

    private void createInitialKeyStore(File file, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password);

        file.getParentFile().mkdirs();
        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            keyStore.store(fos, password);
        }
    }

    private void modifyKeyStoreFileExternally(File file, char[] password, String entryName, String entryValue) throws Exception {
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

        // Save back to file with new timestamp
        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            keyStore.store(fos, password);
        }

        System.out.println("External modification: Added '" + entryName + "' to KeyStore file");
        System.out.println("New file timestamp: " + file.lastModified());
    }
}
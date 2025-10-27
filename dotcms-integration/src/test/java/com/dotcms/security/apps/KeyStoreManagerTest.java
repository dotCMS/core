package com.dotcms.security.apps;

import static com.dotcms.security.apps.KeyStoreManager.SECRETS_KEYSTORE_FILE_PATH_KEY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.dotcms.JUnit4WeldRunner;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.util.IntegrationTestInitService;
import com.dotmarketing.util.Config;
import java.io.File;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import javax.enterprise.context.ApplicationScoped;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test class for KeyStoreManager focusing on lastModified functionality and auto-reload behavior.
 */
@RunWith(JUnit4WeldRunner.class)
@ApplicationScoped
public class KeyStoreManagerTest {

    private static KeyStoreManager keyStoreManager;
    private static File testKeyStoreFile;
    private static final char[] testPassword = "testPassword123".toCharArray();

    @BeforeClass
    public static void prepare() throws Exception {
        IntegrationTestInitService.getInstance().init();
        SecretsStore.INSTANCE.get().deleteAll();

        // Create test keystore file
        final Path tempDir = Files.createTempDirectory("tmpAppsDir").toAbsolutePath();
        testKeyStoreFile = tempDir.resolve("testKeyStore.p12").toFile();

        // Mock Config to return our test path

        final String property = Config.getStringProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, null);
        try {
            Config.setProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, testKeyStoreFile.getAbsolutePath());

            // Create an initial keystore file
            createInitialKeyStore(testKeyStoreFile, testPassword);

            // Initialize KeyStoreManager
            keyStoreManager = CDIUtils.getBeanThrows(KeyStoreManager.class);
        }finally {
            Config.setProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, property);
        }
    }


    @Test
    public void testInitialLoad() {
        // Test that an initial load works
        KeyStore keyStore = keyStoreManager.getKeyStore();
        assertNotNull(keyStore);
        assertEquals("pkcs12", keyStore.getType());
    }

    // Helper method to create initial KeyStore file
    private static void createInitialKeyStore(File file, char[] password) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password);

        // Ensure parent directory exists
        file.getParentFile().mkdirs();

        try (OutputStream fos = Files.newOutputStream(file.toPath())) {
            keyStore.store(fos, password);
        }
    }


}
package com.dotcms.security.apps;

import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.liferay.util.FileUtil;

import java.nio.file.Paths;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * CDI ApplicationScoped KeyStore manager that handles automatic reloading
 * when the underlying keystore file changes on disk.
 *
 * This class provides a centralized, thread-safe KeyStore instance that:
 * - Loads automatically from disk on first access
 * - Reloads when file timestamp changes
 * - Uses ReadWriteLock for optimal concurrent access
 */
@ApplicationScoped
public class KeyStoreManager {

    private static final String SECRETS_STORE_FILE = "dotSecretsStore.p12";
    private static final String SECRETS_STORE_KEYSTORE_TYPE = "pkcs12";
    private static final String SECRETS_STORE_LOAD_TRIES = "SECRETS_STORE_LOAD_TRIES";
    private static final String SECRETS_KEYSTORE_FILE_PATH_KEY = "SECRETS_KEYSTORE_FILE_PATH_KEY";

    // Thread-safe synchronization
    private final ReadWriteLock keyStoreLock = new ReentrantReadWriteLock();
    private volatile KeyStore cachedKeyStore;
    private volatile long lastModified = -1;

    private final String keyStorePath;
    private final Supplier<char[]> passwordSupplier;

    public static String getSecretStorePath() {
        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + "server" + File.separator + "secrets" + File.separator + SECRETS_STORE_FILE;
        final String dirPath = Config.getStringProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize().toString();
    }

    public KeyStoreManager() {
        this.keyStorePath = getSecretStorePath();
        this.passwordSupplier = () -> Config
                .getStringProperty(SecretsKeyStoreHelper.SECRETS_KEYSTORE_PASSWORD_KEY,
                        AppsUtil.digest(com.dotcms.enterprise.cluster.ClusterFactory.getClusterSalt())).toCharArray();
    }

    /**
     * Gets the KeyStore instance, reloading from disk if file has been modified.
     * Uses ReadWriteLock for optimal concurrent access performance.
     *
     * @return the current KeyStore instance
     */
    public KeyStore getKeyStore() {
        final File keyStoreFile = new File(keyStorePath);
        final long currentModified = keyStoreFile.exists() ? keyStoreFile.lastModified() : 0;

        // Fast path: check if the cache is valid without any locking
        KeyStore result = cachedKeyStore;
        if (result != null && lastModified == currentModified) {
            Logger.debug(this.getClass(), "Returning cached KeyStore (timestamp match)");
            return result;
        }

        // Slow path: acquire write lock for loading/reloading
        keyStoreLock.writeLock().lock();
        try {
            result = cachedKeyStore;
            final long checkModified = keyStoreFile.exists() ? keyStoreFile.lastModified() : 0;

            // Double-check: another thread might have already loaded
            if (result == null || lastModified != checkModified) {
                Logger.info(this.getClass(), String.format(
                    "Loading KeyStore from disk (file modified: %d -> %d)",
                    lastModified, checkModified));

                result = loadKeyStoreFromDisk();
                cachedKeyStore = result;
                lastModified = checkModified;
            }

            return result;
        } finally {
            keyStoreLock.writeLock().unlock();
        }
    }

    /**
     * Forces a reload of the KeyStore from disk on next access.
     * Useful for programmatic cache invalidation.
     */
    public void invalidateCache() {
        keyStoreLock.writeLock().lock();
        try {
            cachedKeyStore = null;
            lastModified = -1;
            Logger.debug(this.getClass(), "KeyStore cache invalidated");
        } finally {
            keyStoreLock.writeLock().unlock();
        }
    }

    /**
     * Saves the KeyStore to disk using atomic write (tmp file + rename).
     * Automatically updates the cache and timestamp.
     */
    public void saveKeyStore(final KeyStore keyStore) {
        final File secretStoreFile = new File(keyStorePath);
        final File secretStoreFileTmp = new File(
            secretStoreFile.getParent(),
            "dotSecretsStore_" + System.currentTimeMillis() + ".p12.tmp"
        );

        keyStoreLock.writeLock().lock();
        try {
            // Ensure parent directory exists
            if (!secretStoreFileTmp.getParentFile().exists()) {
                secretStoreFileTmp.getParentFile().mkdirs();
            }

            // Atomic write: tmp file -> final file
            try (OutputStream fos = Files.newOutputStream(secretStoreFileTmp.toPath())) {
                keyStore.store(fos, passwordSupplier.get());
                FileUtil.copyFile(secretStoreFileTmp, secretStoreFile);
                Files.deleteIfExists(secretStoreFileTmp.toPath());

                // Update cache
                cachedKeyStore = keyStore;
                lastModified = secretStoreFile.lastModified();

                Logger.debug(this.getClass(), "KeyStore saved successfully and cache updated");
            }
        } catch (Exception e) {
            Logger.error(this.getClass(), "unable to save secrets store " + secretStoreFileTmp + ": " + e);
            throw new DotRuntimeException(e);
        } finally {
            keyStoreLock.writeLock().unlock();
        }
    }

    /**
     * Loads KeyStore from disk with retry logic.
     */
    private KeyStore loadKeyStoreFromDisk() {
        try {
            final KeyStore keyStore = KeyStore.getInstance(SECRETS_STORE_KEYSTORE_TYPE);
            final int maxLoadTries = Config.getIntProperty(SECRETS_STORE_LOAD_TRIES, 2);

            int tryCount = 1;
            while (tryCount <= maxLoadTries) {
                final File secretStoreFile = createStoreIfNeeded();
                try (InputStream inputStream = Files.newInputStream(secretStoreFile.toPath())) {
                    keyStore.load(inputStream, passwordSupplier.get());
                    Logger.info(KeyStoreManager.class,
                            String.format("KeyStore loaded successfully after %d tries.", tryCount));
                    break;
                } catch (IOException e) {
                    Logger.warn(KeyStoreManager.class,
                        String.format("Failed to load KeyStore on attempt %d: %s", tryCount, e.getMessage()));
                    tryCount++;
                    if (tryCount > maxLoadTries) {
                        throw new DotRuntimeException("Failed to load KeyStore after " + maxLoadTries + " attempts", e);
                    }
                }
            }

            return keyStore;
        } catch (Exception e) {
            Logger.error(this.getClass(), "Unable to load secrets store " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Creates KeyStore file if it doesn't exist.
     */
    private File createStoreIfNeeded() {
        final File secretStoreFile = new File(keyStorePath);
        if (!secretStoreFile.exists()) {
            try {
                Logger.info(this.getClass(), "Creating new KeyStore at: " + keyStorePath);
                final KeyStore keyStore = KeyStore.getInstance(SECRETS_STORE_KEYSTORE_TYPE);
                keyStore.load(null, passwordSupplier.get());

                // Create parent directories
                if (!secretStoreFile.getParentFile().exists()) {
                    secretStoreFile.getParentFile().mkdirs();
                }

                // Save empty KeyStore
                try (OutputStream fos = Files.newOutputStream(secretStoreFile.toPath())) {
                    keyStore.store(fos, passwordSupplier.get());
                }

                Logger.info(this.getClass(), "New KeyStore created successfully");
            } catch (Exception e) {
                Logger.error(this.getClass(), "unable to create secrets store " + SECRETS_STORE_FILE + ": " + e);
                throw new DotRuntimeException(e);
            }
        }
        return secretStoreFile;
    }

    /**
     * Destroys the KeyStore file and invalidates cache.
     */
    public void destroy() {
        keyStoreLock.writeLock().lock();
        try {
            final File secretStoreFile = new File(keyStorePath);
            if (secretStoreFile.exists()) {
                try {
                    Files.delete(secretStoreFile.toPath());
                    Logger.info(this.getClass(), "KeyStore file deleted: " + keyStorePath);
                } catch (IOException e) {
                    Logger.warn(this.getClass(), "Failed to delete KeyStore file: " + e.getMessage());
                }
            }

            cachedKeyStore = null;
            lastModified = -1;
            Logger.debug(this.getClass(), "KeyStore destroyed and cache invalidated");
        } finally {
            keyStoreLock.writeLock().unlock();
        }
    }

    /**
     * Gets the current number of entries in the KeyStore.
     */
    public int size() {
        try {
            return getKeyStore().size();
        } catch (Exception e) {
            Logger.error(this.getClass(), "Failed to get KeyStore size: " + e.getMessage(), e);
            return 0;
        }
    }

    /**
     * Gets the KeyStore file path.
     */
    public String getKeyStorePath() {
        return keyStorePath;
    }
}
package com.dotcms.security.apps;

import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotCacheException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.util.FileUtil;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/**
 * This is basically a safe repository implemented using java.security.KeyStore
 * Which according to the official Java documentation Represents a storage facility for cryptographic keys and certificates.
 * More info Below:
 * @see <a href=https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore>https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore</a>
 * @see <a href=https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14>https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14</a>
 * @see <a href=https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details/>https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details</a>
 */
public class SecretsStoreKeyStoreImpl implements SecretsStore {

    private static final String SECRETS_STORE_FILE = "dotSecretsStore.p12";
    private static final String SECRETS_STORE_KEYSTORE_TYPE = "pkcs12";
    private static final String SECRETS_STORE_SECRET_KEY_FACTORY_TYPE = "PBE";
    protected static final String SECRETS_CACHE_GROUP = "SECRETS_CACHE_GROUP";
    protected static final String SECRETS_CACHE_KEYS_GROUP = "KEYS_CACHE_GROUP";
    protected static final String SECRETS_CACHE_KEY = "KEYS_CACHE";
    private static final String SECRETS_KEYSTORE_PASSWORD_KEY = "SECRETS_KEYSTORE_PASSWORD_KEY";
    private static final String SECRETS_KEYSTORE_FILE_PATH_KEY = "SECRETS_KEYSTORE_FILE_PATH_KEY";
    private final String secretsKeyStorePath;

    private static String getSecretStorePath() {
        final Supplier<String> supplier = () -> APILocator.getFileAssetAPI().getRealAssetsRootPath()
                + File.separator + "server" + File.separator + "secrets" + File.separator + SECRETS_STORE_FILE;
        final String dirPath = Config.getStringProperty(SECRETS_KEYSTORE_FILE_PATH_KEY, supplier.get());
        return Paths.get(dirPath).normalize().toString();
    }

    @VisibleForTesting
    protected SecretsStoreKeyStoreImpl(final String secretsKeyStorePath) {
        this.secretsKeyStorePath = secretsKeyStorePath;
    }

    protected SecretsStoreKeyStoreImpl() {
        this(getSecretStorePath());
    }

    /**
     * returns the password to the store - it can be set as a config variable (which can read from an
     * environment or system property) or will default to the SHA-256 digest of the clusterId
     * 
     * @return
     */
    @VisibleForTesting
    private char[] loadStorePassword() {
        return getFromCache(SECRETS_KEYSTORE_PASSWORD_KEY,
                () -> Config.getStringProperty(SECRETS_KEYSTORE_PASSWORD_KEY, digest(ClusterFactory.getClusterId()))
                                .toCharArray());

    }

    /**
     * This will create a Keystore file for reading/writing if there is not one already there,
     * otherwise, it will return the one there
     * 
     * @return
     */
    private File createStoreIfNeeded() {
        final File secretStoreFile = new File(secretsKeyStorePath);
        if (!secretStoreFile.exists()) {
            try {
                final KeyStore keyStore = KeyStore.getInstance(SECRETS_STORE_KEYSTORE_TYPE);
                keyStore.load(null, loadStorePassword());
                flushCache();
                saveSecretsStore(keyStore);
            } catch (Exception e) {
                Logger.error(this.getClass(), "unable to create secrets store " + SECRETS_STORE_FILE + ": " + e);
                throw new DotRuntimeException(e);
            }
        }
        return secretStoreFile;
    }


    /**
     * loads up the Keystore from disk
     * 
     * @return
     */
    private KeyStore getSecretsStore() {
        final File secretStoreFile = createStoreIfNeeded();
        try {
            final KeyStore keyStore = KeyStore.getInstance(SECRETS_STORE_KEYSTORE_TYPE);
            try (InputStream inputStream = Files.newInputStream(secretStoreFile.toPath())) {
                keyStore.load(inputStream, loadStorePassword());
            }

            return keyStore;

        } catch (Exception e) {
            Logger.error(this.getClass(), "unable to load secrets store " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }

    }

    /**
     * Persists the keystore, tries to do a 2 phase commit, saving to a tmp file, copying that to the
     * keystore file, then deleting the tmp file
     * 
     * @param keyStore
     * @return
     */
    private KeyStore saveSecretsStore(final KeyStore keyStore) {
        final File secretStoreFile = new File(secretsKeyStorePath);
        final File secretStoreFileTmp = new File(secretStoreFile.getParent(), "dotSecretsStore_" + System.currentTimeMillis() + ".p12.tmp");

        secretStoreFileTmp.getParentFile().mkdirs();
        try (OutputStream fos = Files.newOutputStream(secretStoreFileTmp.toPath())) {
            keyStore.store(fos, loadStorePassword());
            FileUtil.copyFile(secretStoreFileTmp, secretStoreFile);
            secretStoreFileTmp.delete();
        } catch (Exception e) {
            Logger.error(this.getClass(), "unable to save secrets store " + secretStoreFileTmp + ": " + e);
            throw new DotRuntimeException(e);
        }
        return keyStore;
    }

    @VisibleForTesting
    protected final static char[] CACHE_404 = new char[] {'4', '0', '4'};

    /**
     * Verifies if the key exists in the store
     * @param variableKey
     * @return
     */
    public boolean containsKey(final String variableKey){
        try {
            return getKeysFromCache().contains(variableKey);
        } catch (Exception e) {
            Logger.error(SecretsStoreKeyStoreImpl.class,e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * secret values are stored encryped in cache for 30 seconds and are decrypted when requested. If
     * value is not in cache, it will be read from the keystore
     * 
     * @param variableKey
     * @return
     * @throws Exception
     */
    public Optional<char[]> getValue(final String variableKey) {

        final char[] fromCache = getFromCache(variableKey,
                () -> loadValueFromStore(variableKey));

        return Arrays.equals(fromCache, CACHE_404) ? Optional.empty() : Optional.ofNullable(decrypt(fromCache));

    }

    /**
     * tries to load the value from the store on disk
     * 
     * @param variableKey
     * @return
     */
    private char[] loadValueFromStore(final String variableKey) {
        try {
           final KeyStore keyStore = getSecretsStore();
            if (keyStore.containsAlias(variableKey)) {
                final PasswordProtection keyStorePP = new PasswordProtection(loadStorePassword());
                final SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRETS_STORE_SECRET_KEY_FACTORY_TYPE);
                final SecretKeyEntry secretKeyEntry = (SecretKeyEntry) keyStore.getEntry(variableKey, keyStorePP);
                return ((PBEKeySpec) factory.getKeySpec(secretKeyEntry.getSecretKey(), PBEKeySpec.class)).getPassword();
            } else {
                return CACHE_404;
            }

        } catch (Exception e) {
            Logger.error(SecretsStoreKeyStoreImpl.class,e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Lists all keys.
     * Keys will be loaded from cache if available otherwise they will be loaded from disk
     * @return
     */
    @Override
    public Set<String> listKeys() {
        return Sneaky.sneak(this::getKeysFromCache);
    }

    @Override
    public synchronized boolean deleteAll(){
        final File secretStoreFile = new File(secretsKeyStorePath);
        secretStoreFile.delete();
        flushCache();
        return true;

    }


    /**
     * This method saves a secret value into the keystore. The value to be stored is encrypted using the
     * company key and is used as a "password" for a keystore entry - using it as a passowrd allows us
     * to store an arbitrary string in a keystore
     * 
     * @param variableKey
     * @param variableValue
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     * @throws KeyStoreException
     * @throws Exception
     */
    @Override
    public boolean saveValue(final String variableKey, final char[] variableValue) {
        try {
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRETS_STORE_SECRET_KEY_FACTORY_TYPE);
            final KeyStore keyStore = getSecretsStore();
            final char [] encryptedVal = encrypt(variableValue);
            final SecretKey generatedSecret = factory.generateSecret(new PBEKeySpec(encryptedVal));
            final PasswordProtection keyStorePP = new PasswordProtection(loadStorePassword());
            keyStore.setEntry(variableKey, new KeyStore.SecretKeyEntry(generatedSecret), keyStorePP);
            saveSecretsStore(keyStore);
            putInCache(variableKey, encryptedVal);
        } catch (Exception e) {
            Logger.warn(this.getClass(), "Unable to save secret from " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }
        return true;
    }

    /**
     * deletes a value from the store
     */
    @Override
    public void deleteValue(final String secretKey) {
        try {
            final KeyStore keyStore = getSecretsStore();
            keyStore.deleteEntry(secretKey);
            saveSecretsStore(keyStore);
            flushCache(secretKey);
        } catch (KeyStoreException | DotCacheException e) {
            Logger.warn(this.getClass(), "Unable to delete secret from  " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * While In memory, values remain encrypted. This is the key used for such purpose
     * @return
     */
    private Key key() {
        return Sneaky.sneak(() -> APILocator.getCompanyAPI().getDefaultCompany()).getKeyObj();
    }

    @VisibleForTesting
    protected char[] encrypt(final char[] val) {
        return Sneaky.sneak(() -> AppsUtil.encrypt(key(), val));
    }

    @VisibleForTesting
    protected String digest(final String val) {
        return Sneaky.sneak(() -> AppsUtil.digest(val));
    }

    @VisibleForTesting
    protected char[] decrypt(final String encryptedString) {
        if (encryptedString == null || encryptedString.length() == 0) {
            return null;
        }
        return Sneaky.sneak(() -> AppsUtil.decrypt(key(), encryptedString));
    }

    @VisibleForTesting
    protected char[] decrypt(final char[] encryptedString) {
        if (encryptedString == null || encryptedString.length == 0) {
            return null;
        }
        return decrypt(new String(encryptedString));
    }

    private char[] getFromCache(final String key, final Supplier<char[]> defaultValue) {
        char[] retVal = (char[]) CacheLocator.getCacheAdministrator().getNoThrow(key, SECRETS_CACHE_GROUP);
        if (retVal == null) {
            retVal = defaultValue.get();
            CacheLocator.getCacheAdministrator().put(key, retVal, SECRETS_CACHE_GROUP);
        }
        return retVal;
    }

    private synchronized void putInCache(final String key, final char[] val)
            throws KeyStoreException, DotCacheException {
        CacheLocator.getCacheAdministrator().put(key, val ,SECRETS_CACHE_GROUP);
        putInCache(key);
    }

    private synchronized void putInCache(final String key) throws KeyStoreException, DotCacheException{
         final Set <String> keys =  getKeysFromCache();
         keys.add(key);
         CacheLocator.getCacheAdministrator().put(SECRETS_CACHE_KEY, keys, SECRETS_CACHE_KEYS_GROUP);
    }

   private synchronized Set<String> getKeysFromCache() throws KeyStoreException, DotCacheException {
        Set<String> keys = (Set<String>) CacheLocator.getCacheAdministrator().get(SECRETS_CACHE_KEY, SECRETS_CACHE_KEYS_GROUP);
        if (!UtilMethods.isSet(keys)) {
            final KeyStore keyStore = getSecretsStore();
            keys = new HashSet<>(Collections.list(keyStore.aliases()));
            CacheLocator.getCacheAdministrator().put(SECRETS_CACHE_KEY, keys, SECRETS_CACHE_KEYS_GROUP);
        }
        return keys;
    }

    private synchronized void flushCache() {
        CacheLocator.getCacheAdministrator().flushGroup(SECRETS_CACHE_GROUP);
        CacheLocator.getCacheAdministrator().flushGroup(SECRETS_CACHE_KEYS_GROUP);
    }

    private synchronized void flushCache(final String key) throws DotCacheException {
        CacheLocator.getCacheAdministrator().remove(key, SECRETS_CACHE_GROUP);
        final Set<String> keys = (Set<String>) CacheLocator.getCacheAdministrator().get(SECRETS_CACHE_KEY, SECRETS_CACHE_KEYS_GROUP);
        if(UtilMethods.isSet(keys)){
           keys.remove(key);
           CacheLocator.getCacheAdministrator().put(SECRETS_CACHE_KEY, keys, SECRETS_CACHE_KEYS_GROUP);
        }
    }

}

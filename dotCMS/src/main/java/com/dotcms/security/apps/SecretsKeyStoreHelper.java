package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsCache.CACHE_404;
import static com.dotcms.security.apps.AppsUtil.digest;

import com.dotcms.auth.providers.jwt.factories.SigningKeyFactory;
import com.dotcms.cdi.CDIUtils;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.annotations.VisibleForTesting;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.io.IOException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/**
 * This is basically a safe repository implemented using java.security.KeyStore,
 * Which, according to the official Java documentation, Represents a storage facility for cryptographic keys and certificates.
 * More info Below:
 * @see <a href=https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore>https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore</a>
 * @see <a href=https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14>https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14</a>
 * @see <a href=https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details/>https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details</a>
 * This class does not offer any caching for that purpose you need to consume this class through {@link SecretCachedKeyStoreImpl}
 */
public class SecretsKeyStoreHelper {

    static final String SECRETS_KEYSTORE_PASSWORD_KEY = "SECRETS_KEYSTORE_PASSWORD_KEY";
    private static final String SECRETS_STORE_FILE = "dotSecretsStore.p12";
    private static final String SECRETS_STORE_SECRET_KEY_FACTORY_TYPE = "PBE";
    private static final String APPS_KEY_PROVIDER_CLASS = "APPS_KEY_PROVIDER_CLASS";
    private final Supplier<char[]> passwordSupplier;

    @VisibleForTesting
    public static String getSecretStorePath() {
        return KeyStoreManager.getSecretStorePath();
    }

    public SecretsKeyStoreHelper(
            final Supplier<char[]> passwordSupplier) {
                this.passwordSupplier = passwordSupplier;
    }

    public SecretsKeyStoreHelper() {
       this(() -> Config
               .getStringProperty(SECRETS_KEYSTORE_PASSWORD_KEY,
                       digest(ClusterFactory.getClusterSalt())).toCharArray());
    }

   /**
    * Gets the KeyStore using CDI ApplicationScoped KeyStoreManager.
    * This provides automatic reloading when the file timestamp changes.
    */
    KeyStore getSecretsStore() {
            final KeyStoreManager keyStoreManager = CDIUtils.getBeanThrows(KeyStoreManager.class);
            return keyStoreManager.getKeyStore();
    }

    /**
     * Persists the keystore using CDI KeyStoreManager. Provides atomic write and automatic cache
     * update.
     *
     * @param keyStore the KeyStore to save
     */
    private void saveSecretsStore(final KeyStore keyStore) {
        try {
            final KeyStoreManager keyStoreManager = CDIUtils.getBeanThrows(KeyStoreManager.class);
            keyStoreManager.saveKeyStore(keyStore);
        } catch (Exception e) {
            Logger.error(this.getClass(), "Failed to save KeyStore using CDI KeyStoreManager: " + e.getMessage(), e);
            throw new DotRuntimeException("Unable to save KeyStore", e);
        }
    }

    /**
     * secret values are stored encrypted in a cache for 30 seconds and are decrypted when requested. If
     *  a value is not in the cache, it will be read from the keystore
     * 
     * @param variableKey
     * @return
     * @throws Exception
     */
    public char[] getValue(final String variableKey) {
        final Optional<char[]> chars = loadValueFromStore(variableKey);
        return chars.orElse(CACHE_404);
    }

    /**
     * tries to load the value from the store on disk
     * 
     * @param variableKey
     * @return
     */
    private Optional<char[]> loadValueFromStore(final String variableKey) {
        try {
           final KeyStore keyStore = getSecretsStore();
            if (keyStore.containsAlias(variableKey)) {
                final PasswordProtection keyStorePP = new PasswordProtection(passwordSupplier.get());
                final SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRETS_STORE_SECRET_KEY_FACTORY_TYPE);
                final SecretKeyEntry secretKeyEntry = (SecretKeyEntry) keyStore.getEntry(variableKey, keyStorePP);
                final char[] chars = ((PBEKeySpec) factory.getKeySpec(secretKeyEntry.getSecretKey(), PBEKeySpec.class)).getPassword();
                return Optional.of(chars);
            } else {
                return Optional.empty();
            }
        } catch (Exception e) {
            Logger.error(SecretsKeyStoreHelper.class,e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * Use this to destroy the secrets repo
     */
    public  void destroy(){
        final KeyStoreManager keyStoreManager = CDIUtils.getBeanThrows(KeyStoreManager.class);
        keyStoreManager.destroy();
    }

    /**
     * Number of secrets stored
     * @return store size
     */
    public int size(){
       return Sneaky.sneaked(() -> getSecretsStore().size()).get();
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
    public char [] saveValue(final String variableKey, final char[] variableValue) {
        try {
            final SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRETS_STORE_SECRET_KEY_FACTORY_TYPE);
            final KeyStore keyStore = getSecretsStore();
            final char [] encryptedVal = encrypt(variableValue);
            final SecretKey generatedSecret = factory.generateSecret(new PBEKeySpec(encryptedVal));
            final PasswordProtection keyStorePP = new PasswordProtection(passwordSupplier.get());
            keyStore.setEntry(variableKey, new KeyStore.SecretKeyEntry(generatedSecret), keyStorePP);
            saveSecretsStore(keyStore);
            return encryptedVal;
        } catch (Exception e) {
            Logger.warn(this.getClass(), "Unable to save secret from " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * deletes a value from the store.
     */
    void deleteValue(final String secretKey) {
        try {
            final KeyStore keyStore = getSecretsStore();
            keyStore.deleteEntry(secretKey);
            saveSecretsStore(keyStore);
        } catch (KeyStoreException  e) {
            Logger.warn(this.getClass(), "Unable to delete secret from  " + SECRETS_STORE_FILE + ": " + e);
            throw new DotRuntimeException(e);
        }
    }

    /**
     * While In memory, values remain encrypted. This is the key used for such a purpose
     * @return Security Key
     */
    private Key key() {
        final String providerClassName = getCustomKeyProvider();
        if(UtilMethods.isSet(providerClassName)){
            try {
                @SuppressWarnings("unchecked")
                final SigningKeyFactory customKeyProvider = ((Class<SigningKeyFactory>) Class
                        .forName(providerClassName)).getDeclaredConstructor().newInstance();
                return customKeyProvider.getKey();
            } catch (Exception e) {
                Logger.error(this.getClass(), " Fail to get Security Key from Custom Key Provider Will fallback to default key provider. ", e);
            }
        }
        return Sneaky.sneak(() -> AppsKeyDefaultProvider.INSTANCE.get().getKey());
    }

    /**
     * brings the possibility to load a custom class to override the default Key provider thought an implementation of <code>SigningKeyFactory</code>
     * @return String
     */
    private String getCustomKeyProvider() {
        return Config
                .getStringProperty(APPS_KEY_PROVIDER_CLASS, null);
    }

    /**
     * encryption function
     * @param val
     * @return
     */
    @VisibleForTesting
    char[] encrypt(final char[] val) {
        return Sneaky.sneak(() -> AppsUtil.encrypt(key(), val));
    }

    /**
     * decryption function
     * @param encryptedString - encrypted string
     * @return - decrypted string
     */
    @VisibleForTesting
    char[] decrypt(final String encryptedString) {
        if (encryptedString == null || encryptedString.isEmpty()) {
            return null;
        }
        return Sneaky.sneak(() -> AppsUtil.decrypt(key(), encryptedString));
    }

    /**
     * @param encryptedString
     * @return char array
     */
    @VisibleForTesting
    char[] decrypt(final char[] encryptedString) {
        if (encryptedString == null || encryptedString.length == 0) {
            return null;
        }
        return decrypt(new String(encryptedString));
    }

    /**
     * {@inheritDoc}
     */
    void backupAndRemoveKeyStore() throws IOException {
        final KeyStoreManager keyStoreManager = CDIUtils.getBeanThrows(KeyStoreManager.class);
        keyStoreManager.backupAndRemoveKeyStore();
    }
}

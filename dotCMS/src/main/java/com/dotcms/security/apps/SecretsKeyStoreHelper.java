package com.dotcms.security.apps;

import static com.dotcms.security.apps.AppsCache.CACHE_404;
import static com.dotcms.security.apps.AppsUtil.digest;

import com.dotcms.api.system.event.Visibility;
import com.dotcms.auth.providers.jwt.factories.SigningKeyFactory;
import com.dotcms.enterprise.cluster.ClusterFactory;
import com.dotcms.notifications.bean.NotificationLevel;
import com.dotcms.notifications.bean.NotificationType;
import com.dotcms.notifications.business.NotificationAPI;
import com.dotcms.util.I18NMessage;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.Role;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.util.Config;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.cdi.CDIUtils;
import com.google.common.annotations.VisibleForTesting;
import com.liferay.portal.model.User;
import com.rainerhahnekamp.sneakythrow.Sneaky;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.apache.commons.lang.time.FastDateFormat;


/**
 * This is basically a safe repository implemented using java.security.KeyStore
 * Which according to the official Java documentation Represents a storage facility for cryptographic keys and certificates.
 * More info Below:
 * @see <a href=https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore>https://stackoverflow.com/questions/6243446/how-to-store-a-simple-key-string-inside-java-keystore</a>
 * @see <a href=https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14>https://medium.com/@danojadias/aes-256bit-encryption-decryption-and-storing-in-the-database-using-java-2ada3f2a0b14</a>
 * @see <a href=https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details/>https://neilmadden.blog/2017/11/17/java-keystores-the-gory-details</a>
 * This class does not offer any caching for that purpose you need to consume this class through {@link SecretCachedKeyStoreImpl}
 */
public class SecretsKeyStoreHelper {

    static final String SECRETS_KEYSTORE_PASSWORD_KEY = "SECRETS_KEYSTORE_PASSWORD_KEY";
    private static final String SECRETS_STORE_FILE = "dotSecretsStore.p12";
    private static final String SECRETS_STORE_KEYSTORE_TYPE = "pkcs12";
    private static final String SECRETS_STORE_SECRET_KEY_FACTORY_TYPE = "PBE";
    private static final String SECRETS_STORE_LOAD_TRIES = "SECRETS_STORE_LOAD_TRIES";
    private static final String SECRETS_KEYSTORE_FILE_PATH_KEY = "SECRETS_KEYSTORE_FILE_PATH_KEY";
    private static final String APPS_KEY_PROVIDER_CLASS = "APPS_KEY_PROVIDER_CLASS";
    private final String secretsKeyStorePath;
    private final List<StoreCreatedListener> storeCreatedListeners;
    private final Supplier<char[]> passwordSupplier;

    @VisibleForTesting
    public static String getSecretStorePath() {
        return KeyStoreManager.getSecretStorePath();
    }

    private SecretsKeyStoreHelper(final String secretsKeyStorePath,
            final Supplier<char[]> passwordSupplier,
            final List<StoreCreatedListener> storeCreatedListeners) {
        this.secretsKeyStorePath = secretsKeyStorePath;
        this.passwordSupplier = passwordSupplier;
        this.storeCreatedListeners = storeCreatedListeners;
    }


    SecretsKeyStoreHelper( final Supplier<char[]> passwordSupplier, final List<StoreCreatedListener> storeCreatedListeners) {
        this(getSecretStorePath(), passwordSupplier, storeCreatedListeners);
    }

   public SecretsKeyStoreHelper() {
       this(getSecretStorePath(),() -> Config
               .getStringProperty(SECRETS_KEYSTORE_PASSWORD_KEY,
                       digest(ClusterFactory.getClusterSalt())).toCharArray(), List.of());
    }


   /**
    * Gets the KeyStore using CDI ApplicationScoped KeyStoreManager.
    * This provides automatic reloading when the file timestamp changes.
    */
    @VisibleForTesting
    KeyStore getSecretsStore() {
        try {
            final KeyStoreManager keyStoreManager = CDIUtils.getBeanThrows(KeyStoreManager.class);
            return keyStoreManager.getKeyStore();
        } catch (Exception e) {
            Logger.error(this.getClass(), "Failed to get KeyStore from CDI KeyStoreManager: " + e.getMessage(), e);
            throw new DotRuntimeException("Unable to access KeyStore", e);
        }
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
     * @return
     */
    
    public synchronized void destroy(){
        final File secretStoreFile = new File(secretsKeyStorePath);
        secretStoreFile.delete();
    }

    /**
     * Number of secrets stored
     * @return
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
     * While In memory, values remain encrypted. This is the key used for such purpose
     * @return
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
     * @return
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
     * @param encryptedString
     * @return
     */
    @VisibleForTesting
    char[] decrypt(final String encryptedString) {
        if (encryptedString == null || encryptedString.length() == 0) {
            return null;
        }
        return Sneaky.sneak(() -> AppsUtil.decrypt(key(), encryptedString));
    }

    /**
     *
     * @param encryptedString
     * @return
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
        final File secretStoreFile = new File(secretsKeyStorePath);
        if (!secretStoreFile.exists()) {
            Logger.warn(SecretsKeyStoreHelper.class, String.format("KeyStore file `%s` does NOT exist therefore it can not be backed-up. ",secretsKeyStorePath));
            return;
        }
        final FastDateFormat datetimeFormat = FastDateFormat.getInstance("yyyyMMddHHmmss");
        final String name = secretStoreFile.getName();
        final File secretStoreFileBak = new File(secretStoreFile.getParent(), datetimeFormat.format(new Date()) + "-" + name );
        Files.copy(secretStoreFile.toPath(), secretStoreFileBak.toPath());
        secretStoreFile.delete();

        Logger.info(SecretsKeyStoreHelper.class, ()->String.format("KeyStore `%s` has been removed a backup has been created.", secretsKeyStorePath));
    }

    /**
     * broad cast system-wide a notification in case of a keystore load exception
     * @throws DotDataException
     */
    private void sendFailureNotification()
            throws DotDataException {

        final NotificationAPI notificationAPI = APILocator.getNotificationAPI();
        // Search for the CMS Admin role and System User
        final Role cmsAdminRole = APILocator.getRoleAPI().loadCMSAdminRole();
        final User systemUser = APILocator.systemUser();

        notificationAPI.generateNotification(new I18NMessage("apps.fail.recover.secrets.title"),
                new I18NMessage("apps.fail.recover.secrets.notification", null), null, // no actions
                NotificationLevel.WARNING, NotificationType.GENERIC, Visibility.ROLE, cmsAdminRole.getId(), systemUser.getUserId(),
                systemUser.getLocale());
    }

    /**
     * handles the any security exception when loading the keyStore
     * on failure the p12 store is back-up and then gets removed.
     * @param gse
     * @throws DotDataException
     * @throws IOException
     */
    private boolean handleStorageLoadException(final IOException gse) throws DotDataException, IOException {
        Logger.warn(SecretsKeyStoreHelper.class,
                "Failed to recover secrets from key/store. The keyStore will be backup and then removed. A new empty store will be generated.",
                gse);
        backupAndRemoveKeyStore();
        sendFailureNotification();
        return true;
    }

    @FunctionalInterface
    public interface StoreCreatedListener {

        void onStoreCreated();

    }
}

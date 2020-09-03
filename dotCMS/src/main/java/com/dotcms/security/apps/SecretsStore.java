package com.dotcms.security.apps;

import java.io.IOException;
import java.util.Optional;
import com.dotmarketing.util.Config;
import io.vavr.control.Try;
import java.util.Set;

/**
 * This is basically a safe repository implemented using java.security.KeyStore
 */
public interface SecretsStore {

    /**
     * verifies if the key is present
     * @param variableKey
     * @return
     */
     boolean containsKey(final String variableKey);

    /**
     * Optionally returns the secrets value if there is one, empty if not
     * 
     * @param variableKey
     * @return
     */
    Optional<char[]> getValue(final String variableKey);

    /**
     * Lists the keys - keys can be case insensitive (forced lowercase) based on the implementation
     * 
     * @return
     */
    Set<String> listKeys();

    /**
     * deletes all the entries in the secretsStore
     * 
     * @return
     * @throws Exception
     */
    boolean deleteAll() throws Exception;

    /**
     * saves a single key and a value in the SecretsStore
     * 
     * @return
     * @throws Exception
     */
    boolean saveValue(String variableKey, char[] variableValue);

    /**
     * deletes a single value out of the SecretsStore
     * 
     * @param secretKey
     * @throws Exception
     */
    void deleteValue(String secretKey);

    /**
     * Creates a backup copy of the p12 keystore file.
     * Then removes the original file.
     * @throws IOException
     */
    void backupAndRemoveKeyStore() throws IOException;

    /**
     * 
     * Singleton Holder Enum
     *
     */
    enum INSTANCE {
        INSTANCE;
        private final SecretsStore secretsStore = loadSecretsApi();

        public static SecretsStore get() {
            return INSTANCE.secretsStore;
        }

        private static SecretsStore loadSecretsApi() {
            return (SecretsStore) Try.of(() -> Class
                            .forName(Config.getStringProperty("SECRETS_STORE_IMPL", SecretsStoreKeyStoreImpl.class.getCanonicalName()))
                            .newInstance()).getOrNull();

        }
    }

}

package com.dotcms.security.secret;

import java.util.Collection;
import java.util.Optional;
import com.dotmarketing.util.Config;
import io.vavr.control.Try;

public interface SecretsStore {

    /**
     * Optionally returns the secrets value if there is one, empty if not
     * 
     * @param variableKey
     * @return
     */
    public Optional<char[]> getValue(final String variableKey);

    /**
     * Lists the keys - keys can be case insensitive (forced lowercase) based on the implementation
     * 
     * @return
     */
    public Collection<String> listKeys();

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

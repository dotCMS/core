package com.dotcms.security.secret;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ServiceIntegrationAPI {

    /**
     * Given an individual host a list with the associated services is returned.
     * @param host
     * @param user
     * @return
     */
    List<String> listServiceKeys(Host host, User user) throws DotDataException, DotSecurityException;

    /**
     * Conforms a map where the Elements are lists of service unique names, organized by host as key.
     * @return
     */
    Map<String, List<String>> serviceKeysByHost();

    /**
     * This returns json object read from the secret store that contains the service integration configuration and secret
     * @param serviceKey
     * @param host
     * @return
     */
     Optional<ServiceSecrets> getSecretsForService(String serviceKey,
             Host host, User user) throws DotDataException, DotSecurityException;

    /**
     * Lookup for an individual secret/property then updates the single entry.
     * @param serviceKey Service unique id.
     * @param keyAndSecret Tuple value Pair with the definition of the secret and name.
     * @param host The host owning the secret.
     * @param user logged-in user
     */
     void saveSecret(String serviceKey, Tuple2<String,Secret> keyAndSecret, Host host, User user)
             throws DotDataException, DotSecurityException;

    /**
     * Creates or replaces an existing service set of secrets.
     * When calling this the Whole secret gets replaced.
     * @param secrets Secrets info bean.
     * @param host The host owning the secret.
     */
    void saveSecrets(ServiceSecrets secrets, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * Lookup for an individual secret/property then removes the single entry.
     * @param serviceKey Service unique id.
     * @param propSecretName Individual secret or property name.
     * @param host The host owning the secret.
     * @param user logged-in user
     */
    void deleteSecret(String serviceKey, String propSecretName, Host host, User user)
    throws DotDataException, DotSecurityException;

    /**
     * Deletes all secretes associated with the serviceKey and Host.
     * @param serviceKey service unique id.
     * @param host The host owning the secret.
     */
    void deleteSecrets(String serviceKey, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * This method should read the yml file service definition
     * @return
     */
    List<ServiceDescriptor> getAvailableServiceDescriptors(User user)
            throws DotDataException, DotSecurityException;

    /**
     *
     * @param serviceKey
     * @param user
     * @return
     * @throws DotDataException
     * @throws DotSecurityException
     */
    Optional<ServiceDescriptor> getServiceDescriptor(final String serviceKey, final User user)
            throws DotDataException, DotSecurityException;

    void createServiceDescriptor(final String serviceKey, final FileInputStream inputStream,
            User user) throws IOException, DotDataException, DotSecurityException;

    enum INSTANCE {
        INSTANCE;
        private final ServiceIntegrationAPI secretsStore = loadSecretsApi();

        public static ServiceIntegrationAPI get() {
            return INSTANCE.secretsStore;
        }

        private static ServiceIntegrationAPI loadSecretsApi() {
            return (ServiceIntegrationAPI) Try.of(() -> Class
                    .forName(Config.getStringProperty("SERVICE_INTEGRATION_API_IMPL",
                            ServiceIntegrationAPIImpl.class.getCanonicalName()))
                    .newInstance()).getOrNull();

        }
    }

}

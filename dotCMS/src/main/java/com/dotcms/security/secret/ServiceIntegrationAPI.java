package com.dotcms.security.secret;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.Tuple2;
import io.vavr.control.Try;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ServiceIntegrationAPI {

    /**
     *
     * @param host
     * @return
     */
    List<String> listServiceKeys(Host host);

    /**
     *
     * @return
     */
    Map<String, List<String>> serviceKeysByHost();

    /**
     * This returns json object read from the secret store that contains the service integration configuration and secret
     * @param serviceKey
     * @param host
     * @return
     */
     Optional<ServiceSecrets> getSecretForService(String serviceKey,
             Host host, User user) throws DotDataException;

    /**
     *
     * @param serviceKey
     * @param propSecretName
     * @param host
     * @param user
     */
     void deleteSecret(String serviceKey, String propSecretName, Host host, User user);

    /**
     *
     * @param serviceKey
     * @param keyAndSecret
     * @param host
     * @param user
     */
     void saveSecret(String serviceKey, Tuple2<String,Secret> keyAndSecret, Host host, User user);

    /**
     *
     * @param bean
     * @param user
     * @throws DotDataException
     * @throws DotSecurityException
     */
    void saveServiceSecrets(ServiceSecrets bean, User user)
            throws DotDataException, DotSecurityException;

    /**
     *
     * @param bean
     * @param host
     */
    void saveServiceSecrets(ServiceSecrets bean, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * @param serviceKey
     * @param host
     */
    void deleteServiceSecrets(String serviceKey, Host host, User user)
            throws DotDataException, DotSecurityException;

    /**
     * This method should read the yml file service definition
     * @return
     */
    List<?> getAvailableServiceDescriptors(Host host, User user);


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

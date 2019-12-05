package com.dotcms.security.secret;

import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.util.Config;
import com.liferay.portal.model.User;
import io.vavr.control.Try;
import java.util.List;
import java.util.Optional;

public interface ServiceIntegrationAPI {

    /**
     * This returns json object read from the secret store that contains the service integration configuration and secret
     * @param serviceKey
     * @return
     */
     Optional<ServiceIntegrationBean> getIntegrationForService(String serviceKey, User user) throws DotDataException;

    /**
     *
     * @param bean
     */
    void registerServiceIntegration(final ServiceIntegrationBean bean, User user)
            throws DotDataException, DotSecurityException;

    /**
     *
     * @param serviceKey
     */
    void deleteServiceIntegration(String serviceKey, User user)
            throws DotDataException, DotSecurityException;

    /**
     * This method should read the yml file service definition
     * @return
     */
    List<ServiceIntegrationBean> getAvailableServiceDescriptors(User user);


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

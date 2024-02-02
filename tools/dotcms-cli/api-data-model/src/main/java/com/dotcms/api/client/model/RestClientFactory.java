package com.dotcms.api.client.model;

import com.dotcms.api.exception.APIConfigurationException;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.jboss.logging.Logger;

/**
 * @author Steve Bolton
 * This serve as a mechanish to instantiate RestClients dynimaically configured
 * from preploaded properties using Quarkus config hierarchy.
 * The idea is to be able to swithc configuration at runtime
 */
@ApplicationScoped
public class RestClientFactory {

    @Inject
    Logger logger;


    @Inject
    ServiceManager serviceManager;

    AtomicReference<ServiceBean> value = new AtomicReference<>();

    private final Map<String, APIEndpoints> registry = new ConcurrentHashMap<>();

    /**
     * Given the selected profile this will return or instantiate a Rest Client
     * @param profileName config profile
     * @param clazz Rest Client class
     * @return Client Instance
     * @param <T> Rest Client class Type
     */
    <T> T getClient(final String profileName, final Class<T> clazz) {
        if (registry.containsKey(profileName)) {
            return registry.get(profileName).getClient(clazz);
        } else {
            try {
                final Optional<ServiceBean> service = serviceManager.get(profileName);
                service.ifPresentOrElse(
                        serviceBean -> {
                            registry.put(profileName, new APIEndpoints(serviceBean.uri()));
                        },
                        () -> {
                            throw new APIConfigurationException(String.format(
                                    "No Service configuration found for profile named [%s]",
                                    profileName));
                        }
                );
            }catch (IOException e) {
                throw new APIConfigurationException(
                        String.format("Unable to get service configuration for profile [%s]", profileName), e);
            }
        }
        return registry.get(profileName).getClient(clazz);
    }

        /**
         * Given the selected profile this will return or instantiate a Rest Client
         * @param clazz
         * @return
         * @param <T>
         */
        public <T > T getClient( final Class<T> clazz){

            Optional<ServiceBean> profile;
            try {
                profile = getService();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to get current selected profile ", e);
            }
            if (profile.isEmpty()) {
                throw new IllegalStateException(
                        String.format("No dotCMS instance has been activated check your [%s] file.",
                                YAMLFactoryServiceManagerImpl.DOT_SERVICE_YML)
                );
            }
            return getClient(profile.get().name(), clazz);
        }

    public Optional<ServiceBean> getService() throws IOException {
        ServiceBean val = value.get();
        if (val == null) {
            val = serviceManager.selected().orElseThrow();
            // Set the value only if it's currently null
            if (!value.compareAndSet(null, val)) {
                // If compareAndSet fails, another thread has already set the value
                val = value.get();
            }
        }
        return Optional.ofNullable(val);
    }

}

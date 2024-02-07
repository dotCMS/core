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

    public static final String NONE = "none";

    @Inject
    Logger logger;

    /**
     * Thread local variable containing API profile name
     */
    private final AtomicReference<String> instanceProfile = new AtomicReference<>(NONE);

    @Inject
    DotCmsClientConfig clientConfig;

    @Inject
    ServiceManager serviceManager;

    private final Map<String, APIEndpoints> registry = new ConcurrentHashMap<>();

    /**
     * Get or instantiate a Rest Client depending on its existence on the registry
     * @param profileName config profile
     * @param uri API URI
     * @param clazz RestClient Class
     * @return RestClient Instance
     * @param <T> Rest Client class Type
     */
    <T> T getClient(final String profileName, final URI uri, final Class<T> clazz) {
        return registry.computeIfAbsent(profileName, c ->
                new APIEndpoints(uri)
        ).getClient(clazz);
    }

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
            if (clientConfig.servers().containsKey(profileName)) {
                final URI server = clientConfig.servers().get(profileName);
                return getClient(profileName, server, clazz);
            } else {
                throw new APIConfigurationException(profileName);
            }
        }
    }

    /**
     * Given the selected profile this will return or instantiate a Rest Client
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> T getClient(final Class<T> clazz)  {

        final Optional<String> profile;
        try {
            profile = currentSelectedProfile();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to get current selected profile ",e);
        }
        if(profile.isEmpty()){
            throw new IllegalStateException(
                   String.format("No dotCMS instance has been activated check your %s file.", YAMLFactoryServiceManagerImpl.DOT_SERVICE_YML)
            );
        }
        return getClient(profile.get(), clazz);
    }

    public Optional<String> currentSelectedProfile() throws IOException {
        instanceProfile.compareAndSet(NONE, service());
        return NONE.equals(instanceProfile.get()) ? Optional.empty() : Optional.of(instanceProfile.get());
    }

    final String service() throws IOException {
        final Optional<ServiceBean> selected = serviceManager.selected();
        return selected.map(ServiceBean::name).orElse(NONE);
    };


}

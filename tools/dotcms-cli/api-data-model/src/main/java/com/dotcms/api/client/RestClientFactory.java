package com.dotcms.api.client;

import com.dotcms.api.exception.ClientConfigNotFoundException;
import com.dotcms.model.config.ServiceBean;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
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
     * Thread local variable containing each thread's ID
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
                throw new ClientConfigNotFoundException(profileName);
            }
        }
    }

    /**
     * Given the selected profile this will return or instantiate a Rest Client
     * @param clazz
     * @return
     * @param <T>
     */
    public <T> T getClient(final Class<T> clazz) {
        return getClient(currentSelectedProfile(), clazz);
    }

    public String currentSelectedProfile() {
        instanceProfile.compareAndSet(NONE, serviceSupplier.get());
        return instanceProfile.get();
    }

    final Supplier<String> serviceSupplier = () -> {
        final Optional<ServiceBean> selected = serviceManager.selected();
        return selected.map(ServiceBean::name).orElse(NONE);
    };


}

package com.dotcms.api.client;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * @author Steve Bolton
 * This serve as a mechanish to instantiate RestClients dynimaically configured
 * from preploaded properties using Quarkus config hierarchy.
 * The idea is to be able to swithc configuration at runtime
 */
@ApplicationScoped
public class RestClientFactory {

    public static final String DEFAULT = "default";

    /**
     * Thread local variable containing each thread's ID
     */
    private final ThreadLocal<String> dotCMSInstanceProfile =
            ThreadLocal.withInitial(() -> DEFAULT);

    @Inject
    DotCmsClientConfig clientConfig;

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
        return getClient(dotCMSInstanceProfile.get(), clazz);
    }

    /**
     * We should be able to tell this class what profile we're currently looking at.
     * @param profile
     */
    public void setProfile(final String profile) {
        dotCMSInstanceProfile.set(profile);
    }

}

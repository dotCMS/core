package com.dotcms.api.client;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
public class RestClientFactory {

    public static final String DEFAULT = "default";

    // Thread local variable containing each thread's ID
    private final ThreadLocal<String> dotCMSInstanceProfile =
            ThreadLocal.withInitial(() -> DEFAULT);

    @Inject
    DotCmsClientConfig clientConfig;

    private final Map<String, APIEndpoints> registry = new ConcurrentHashMap<>();

    <T> T getClient(final String name, final URI uri, final Class<T> clazz) {
        return registry.computeIfAbsent(name, c ->
                new APIEndpoints(uri)
        ).getClient(clazz);
    }

    <T> T getClient(final String configName, final Class<T> clazz) {
        if (registry.containsKey(configName)) {
            return registry.get(configName).getClient(clazz);
        } else {
            if (clientConfig.servers().containsKey(configName)) {
                final URI server = clientConfig.servers().get(configName);
                return getClient(configName, server, clazz);
            } else {
                throw new ClientConfigNotFoundException(configName);
            }
        }
    }

    public <T> T getClient(final Class<T> clazz) {
        return getClient(dotCMSInstanceProfile.get(), clazz);
    }

    void setProfile(final String profile) {
        dotCMSInstanceProfile.set(profile);
    }

}

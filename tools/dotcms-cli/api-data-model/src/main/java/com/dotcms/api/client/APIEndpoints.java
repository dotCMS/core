package com.dotcms.api.client;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.microprofile.rest.client.RestClientBuilder;

/**
 * This class is meant to store all the API-Clients under a given base API URI
 */
public class APIEndpoints {

    private final URI apiBaseUri;
    private final Map<Class<?>,Object> registry = new ConcurrentHashMap<>();

    public APIEndpoints(final URI apiBaseUri){
        this.apiBaseUri = apiBaseUri;
    }

    @SuppressWarnings("unchecked")
    <T> T getClient(final Class<T> clazz) {
        return (T) registry.computeIfAbsent(clazz, c ->
                 RestClientBuilder.newBuilder()
                        .baseUri(apiBaseUri)
                        .build(c)
                );
    }

}

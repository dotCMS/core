package com.dotcms.api.client.model;

import com.dotcms.api.provider.ClientObjectMapper;
import com.dotcms.model.config.ServiceBean;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.RestClientBuilder;
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

    @Inject
    RemoteURLParam remoteURLParam;

    // Stores a reference to the current selected profile, so we don't have to get it from disk every time
    AtomicReference<ServiceBean> profile = new AtomicReference<>();

    /**
     * Given the selected profile this will return or instantiate a Rest Client
     * @param clazz
         * @return
     * @param <T>
     */
    public <T> T getClient(final Class<T> clazz) {

        URI uri = getAPIURI(clazz);

        return newRestClient(clazz, uri);
    }

    /**
     * Retrieves the API URI, if the dotCMS URL is present in the command line arguments, it will
     * use it otherwise it will use the one from the selected profile.
     *
     * @param clazz The class for which to retrieve the API URI.
     * @param <T>   The type of the class.
     * @return The API URI.
     * @throws IllegalStateException If there is an error building the API URI.
     */
    private <T> URI getAPIURI(final Class<T> clazz) {

        URL remoteURL;

        //This injects the dotCMS URL from the command line if present
        final Optional<URL> paramRemoteURL = remoteURLParam.getURL();
        if (paramRemoteURL.isPresent()) {
            remoteURL = paramRemoteURL.get();
        } else {

            Optional<ServiceBean> optional;
            try {
                optional = getServiceProfile();
            } catch (IOException e) {
                throw new IllegalStateException("Unable to get current selected profile ", e);
            }
            if (optional.isEmpty()) {
                throw new IllegalStateException(
                        String.format("No dotCMS instance has been activated check your [%s] file.",
                                YAMLFactoryServiceManagerImpl.DOT_SERVICE_YML)
                );
            }

            remoteURL = optional.get().url();
        }

        URI uri;
        try {
            uri = toApiURI(remoteURL);
        } catch (URISyntaxException | IOException e) {
            throw new IllegalStateException(
                    String.format(
                            "Error building rest client for [%s] ", clazz.getSimpleName()), e);
        }

        return uri;
    }

    public Optional<ServiceBean> getServiceProfile() throws IOException {
        ServiceBean val = profile.get();
        if (val == null) {
            // if no profile is stored in memory, get it from the service manager
            val = serviceManager.selected().orElseThrow();
            // Set the value only if it's currently null
            if (!profile.compareAndSet(null, val)) {
                // If compareAndSet fails, another thread has already set the value
                val = profile.get();
            }
        }
        return Optional.ofNullable(val);
    }

    @SuppressWarnings("unchecked")
    <T> T newRestClient(final Class<T> clazz, final URI apiBaseUri) {
        return RestClientBuilder.newBuilder()
                .register(ClientObjectMapper.class)
                .baseUri(apiBaseUri)
                .build(clazz);
    }

    URI toApiURI(final URL url) throws IOException, URISyntaxException {
        String raw = url.toString();
        if (raw.endsWith("/")) {
            raw = raw + "api";
        } else {
            if(!raw.endsWith("/api")){
                raw = raw + "/api";
            }
        }
        raw = raw.replaceAll("(?<!(http:|https:))//", "/");
        return new URL(raw).toURI();
    }

}

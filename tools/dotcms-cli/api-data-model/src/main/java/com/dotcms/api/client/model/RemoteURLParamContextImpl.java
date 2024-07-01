package com.dotcms.api.client.model;

import io.quarkus.arc.DefaultBean;
import java.net.URL;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This class is used to pass the token from the CLI to the API client. If the token is present here
 * we use directly
 */
@DefaultBean
@ApplicationScoped
public class RemoteURLParamContextImpl implements RemoteURLParam {

    URL remoteURL;

    @Override
    public void setURL(final URL remoteURL) {
        this.remoteURL = remoteURL;
    }

    @Override
    public Optional<URL> getURL() {
        if (null == remoteURL) {
            return Optional.empty();
        }
        return Optional.of(remoteURL);
    }

    @Override
    public void reset() {
        remoteURL = null;
    }

}
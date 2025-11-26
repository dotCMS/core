package com.dotcms.api.client.model;

import io.quarkus.arc.DefaultBean;
import java.util.Arrays;
import java.util.Optional;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * This class is used to pass the token from the CLI to the API client. If the token is present here we use directly
 */
@DefaultBean
@ApplicationScoped
public class AuthenticationParamContextImpl implements AuthenticationParam {

    char[] token;

    @Override
    public void setToken(final char[] token) {
            this.token = Arrays.copyOf(token, token.length);
    }

    public Optional<char[]> getToken() {
        if (null == token || 0 == token.length) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    @Override
    public void reset() {
        token = null;
    }

}

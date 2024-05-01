package com.dotcms.api.client.model;

import io.quarkus.arc.DefaultBean;
import java.util.Arrays;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

/**
 * This class is used to pass the token from the CLI to the API client. If the token is present here we use directly
 */
@DefaultBean
@ApplicationScoped
public class AuthenticationParamContextImpl implements AuthenticationParam {

    char[] token;

    @Override
    public void setToken(final char[] token) {
        if (null != token) {
            this.token = Arrays.copyOf(token, token.length);
        } else {
            this.token = null;
        }
    }

    public Optional<char[]> getToken() {
        if (null == token || 0 == token.length) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

}

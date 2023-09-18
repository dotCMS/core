package com.dotcms.api.client;

import io.quarkus.arc.DefaultBean;
import java.lang.ref.WeakReference;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

/**
 * This class is used to pass the token from the CLI to the API client. If the token is present here we use directly
 */
@DefaultBean
@ApplicationScoped
public class AuthenticationParamContextImpl implements AuthenticationParam {

    WeakReference<char[]> token;

    @Override
    public void setToken(final char[] token) {
        this.token = new WeakReference<>(token);
    }

    public Optional<char[]> getToken() {
        if (null == token || null == token.get()) {
            return Optional.empty();
        }
        try {
            return Optional.ofNullable(token.get());
        } finally {
            token.clear();
        }
    }

}

package com.dotcms.api.client;

import io.quarkus.arc.DefaultBean;
import java.lang.ref.WeakReference;
import java.util.Optional;
import javax.enterprise.context.ApplicationScoped;

@DefaultBean
@ApplicationScoped
public class ParamAuthenticationContextImpl implements ParamAuthentication {

    WeakReference<String> token;

    @Override
    public void setToken(String token) {
        this.token = new WeakReference<>(token);
    }

    public Optional<String> getToken() {
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

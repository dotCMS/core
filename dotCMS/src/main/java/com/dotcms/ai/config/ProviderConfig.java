package com.dotcms.ai.config;

import com.dotcms.ai.model.AIProvider;

import java.util.Objects;

public class ProviderConfig {

    private final AIProvider provider;

    public ProviderConfig(final AIProvider provider) {
        this.provider = provider;
    }

    public AIProvider getProvider() {
        return provider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderConfig that = (ProviderConfig) o;
        return provider == that.provider;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(provider);
    }

    @Override
    public String toString() {
        return "ProviderConfig{" +
                "provider=" + provider +
                '}';
    }

}

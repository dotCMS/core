package com.dotcms.rest.api.v1.apps.view;

import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Secret;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import java.util.Objects;

public class SecretView implements Comparable<SecretView>{

    private final String name;

    @JsonUnwrapped
    @JsonInclude(Include.NON_NULL)
    final private Secret secret;

    @JsonUnwrapped
    @JsonInclude(Include.NON_NULL)
    final private ParamDescriptor paramDescriptor;

    final private boolean dynamic;

    public SecretView(final String name, final Secret secret, final ParamDescriptor paramDescriptor) {
        this.name = name;
        this.secret = secret;
        this.paramDescriptor = paramDescriptor;
        this.dynamic = null == paramDescriptor;
    }

    public String getName() {
        return name;
    }

    public Secret getSecret() {
        return secret;
    }

    public ParamDescriptor getParamDescriptor() {
        return paramDescriptor;
    }

    public boolean isDynamic() {
        return dynamic;
    }

    @Override
    public int compareTo(final SecretView o) {
        return Boolean.compare(this.dynamic,o.dynamic);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SecretView that = (SecretView) o;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

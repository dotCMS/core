package com.dotcms.rest.api.v1.apps.view;

import com.dotcms.security.apps.ParamDescriptor;
import com.dotcms.security.apps.Secret;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class SecretView implements Comparable<SecretView>{

    @JsonUnwrapped
    @JsonInclude(Include.NON_NULL)
    final private Secret secret;

    @JsonUnwrapped
    @JsonInclude(Include.NON_NULL)
    final private ParamDescriptor paramDescriptor;

    final private boolean dynamic;

    public SecretView(final Secret secret, final ParamDescriptor paramDescriptor) {
        this.secret = secret;
        this.paramDescriptor = paramDescriptor;
        this.dynamic = null == paramDescriptor;
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
}

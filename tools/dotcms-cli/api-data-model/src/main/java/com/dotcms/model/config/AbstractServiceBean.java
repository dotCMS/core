package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = ServiceBean.class)
public interface AbstractServiceBean {
    @NotNull
    String name();
    @NotNull
    Boolean active();

    @Nullable
    CredentialsBean credentials();

}

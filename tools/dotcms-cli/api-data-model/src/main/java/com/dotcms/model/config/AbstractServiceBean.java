package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URL;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = ServiceBean.class)
public interface AbstractServiceBean {
    @NotNull
    String name();
    @Value.Default
    default boolean active() {return  false;}

    @NotNull
    URL url();

    @Nullable
    CredentialsBean credentials();

}

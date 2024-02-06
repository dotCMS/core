package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.net.URI;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
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
    @Value.Default
    default URI uri() {return URI.create("http://localhost:8080/api");}

    @Nullable
    CredentialsBean credentials();

}

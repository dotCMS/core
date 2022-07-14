package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(as = ServiceBean.Builder.class)
public interface AbstractServiceBean {

    boolean active();

    CredentialsBean credentials();

}

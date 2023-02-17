package com.dotcms.model.config;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ValueType
@Value.Immutable
@JsonDeserialize(as = CredentialsBean.class)
public interface AbstractCredentialsBean {
     String user();

     @Nullable
     char[] token();

}

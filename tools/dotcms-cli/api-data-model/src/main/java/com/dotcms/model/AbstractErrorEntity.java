package com.dotcms.model;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.smallrye.common.constraint.Nullable;
import org.immutables.value.Value;


@JsonDeserialize(builder = ErrorEntity.Builder.class)
@ValueType
@Value.Immutable
public interface AbstractErrorEntity  {

    @Nullable
    String errorCode();
    @Nullable
    String message();
    @Nullable
    String fieldName();
}
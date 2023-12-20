package com.dotcms.model;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.smallrye.common.constraint.Nullable;
import org.immutables.value.Value;

@JsonDeserialize(builder = MessageEntity.Builder.class)
@Value.Immutable
@ValueType
public interface AbstractMessageEntity  {

    @Nullable
    String message();
}
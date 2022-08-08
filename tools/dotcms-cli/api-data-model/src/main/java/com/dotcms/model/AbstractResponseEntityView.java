package com.dotcms.model;


import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.vertx.codegen.annotations.Nullable;
import java.util.ArrayList;
import java.util.Map;
import org.immutables.value.Value;

import java.util.HashMap;
import java.util.List;
;

@ValueType
@Value.Immutable
@JsonDeserialize(builder = ResponseEntityView.Builder.class)
public interface AbstractResponseEntityView<T> {
    @Nullable
    T entity();

    @Value.Default
    default List<ErrorEntity> errors() { return new ArrayList<>(); };
    @Value.Default
    default List<MessageEntity> messages() { return new ArrayList<>(); };
    @Value.Default
    default Map<String, String> i18nMessagesMap() { return new HashMap<>(); };
    @Value.Default
    default List<String> permissions() { return new ArrayList<>(); };
}

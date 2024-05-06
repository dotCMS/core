package com.dotcms.model;

import com.dotcms.model.annotation.ValueType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.common.constraint.Nullable;
import org.immutables.value.Value;

@ValueType
@Value.Immutable
@JsonDeserialize(builder = ResponseEntityView.Builder.class)
@JsonIgnoreProperties(value = {
        "pagination"
})
public interface AbstractResponseEntityView<T> {
    @Nullable
    T entity();

    @Value.Default
    default List<ErrorEntity> errors() { return new ArrayList<>(); }
    @Value.Default
    default List<MessageEntity> messages() { return new ArrayList<>(); }
    @Value.Default
    default Map<String, String> i18nMessagesMap() { return new HashMap<>(); }
    @Value.Default
    default List<String> permissions() { return new ArrayList<>(); }
    @Nullable
    Pagination pagination();
}

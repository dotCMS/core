package com.dotcms.content.model.type.keyvalue;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;

public class Entry<T> implements Serializable {

    public final String key;

    public final T value;

    Entry(final String key, final T value) {
        this.key = key;
        this.value = value;
    }

    @JsonCreator
    public static Entry <?> of(@JsonProperty("key") final String key,  @JsonProperty("value") final Object value) {
        return new Entry<>(key, value);
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }
}

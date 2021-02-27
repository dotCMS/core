package com.dotcms.storage.model;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import java.io.Serializable;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

public class Metadata implements Serializable {

    public static final String CUSTOM_PROP_PREFIX = "dot:";

    private final String fieldName;

    private final Map<String, Serializable> fieldsMeta;

    private final Map<String, Serializable> customMeta;

    public Metadata(final String fieldName,
            final Map<String, Serializable> fieldsMeta) {

        this.fieldName = fieldName;
        if (null != fieldsMeta) {
            Map<String, Serializable> meta = fieldsMeta;
            this.customMeta = new ImmutableSortedMap.Builder<String, Serializable>(
                    Comparator.naturalOrder()).putAll(meta.entrySet().stream()
                    .filter(entry -> entry.getKey().startsWith(CUSTOM_PROP_PREFIX))
                    .collect(Collectors.toMap(o ->
                            o.getKey().substring(CUSTOM_PROP_PREFIX.length()
                            ), Entry::getValue))).build();

            meta = meta.entrySet().stream()
                    .filter(entry -> !entry.getKey().startsWith(CUSTOM_PROP_PREFIX))
                    .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
            this.fieldsMeta = new ImmutableSortedMap.Builder<String, Serializable>(
                    Comparator.naturalOrder()).putAll(meta).build();
        } else {
            this.fieldsMeta = ImmutableMap.of();
            this.customMeta = ImmutableMap.of();
        }
    }

    public String getFieldName() {
        return fieldName;
    }

    public Map<String, Serializable> getFieldsMeta() {
        return fieldsMeta;
    }

    public Map<String, Serializable> getCustomMeta() {
        return customMeta;
    }

    public Map<String, Serializable> getMap() {
        return new ImmutableSortedMap.Builder<String, Serializable>(Comparator.naturalOrder()).putAll(fieldsMeta).putAll(customMeta).build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Metadata metadata = (Metadata) o;
        return Objects.equals(fieldName, metadata.fieldName) &&
                fieldsMeta.hashCode() == metadata.fieldsMeta.hashCode() &&
                customMeta.hashCode() == metadata.customMeta.hashCode();
    }

    @Override
    public int hashCode() {

        return Objects.hash(fieldName, fieldsMeta, customMeta);
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldsMeta=" + fieldsMeta +
                ", customMeta=" + customMeta +
                '}';
    }
}

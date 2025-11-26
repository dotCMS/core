package com.dotcms.storage.model;

import static com.dotcms.storage.model.BasicMetadataFields.CONTENT_TYPE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.HEIGHT_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.IS_IMAGE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.LENGTH_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.MOD_DATE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.NAME_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.PATH_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.SHA256_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.SIZE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.TITLE_META_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.VERSION_KEY;
import static com.dotcms.storage.model.BasicMetadataFields.WIDTH_META_KEY;

import com.dotmarketing.util.Logger;
import com.google.common.collect.ImmutableSortedMap;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Class intended to represent metadata for a field

 * Metadata is a class that represents the metadata for a field. It contains the field name and a map of metadata for the field.
 * The class provides methods to access the metadata for the field, as well as methods to access specific metadata values.
 * The class is immutable, meaning that once a Metadata object is created, its values cannot be changed.
 * */
public class Metadata implements Serializable {

    public static final String CUSTOM_PROP_PREFIX = "dot:";
    public static final String UNKNOWN = "unknown";

    private final String fieldName;

    private final Map<String, Serializable> fieldsMeta;

    private final transient AtomicReference<Map<String, Serializable>> stdMeta = new AtomicReference<>();
    private final transient AtomicReference<Map<String, Serializable>> customMeta = new AtomicReference<>();
    private final transient AtomicReference<Map<String, Serializable>> customMetaWithPrefix = new AtomicReference<>();



    /**
     * Constructor for Metadata
     * @param fieldName The name of the field
     * @param fieldsMeta The metadata for the field
     */
    public Metadata(String fieldName, Map<String, Serializable> fieldsMeta) {
        this.fieldName = fieldName;
        // If we use sorted map here we do not need to sort it later
        this.fieldsMeta = fieldsMeta == null ? Map.of() : ImmutableSortedMap.copyOf(fieldsMeta);
    }

    /**
     * Returns the name of the field
     * @return fieldName
     */
    public String getFieldName() {
        return fieldName;
    }

    /**
     * Returns the metadata for the field
     * @return fieldsMeta
     */
    public Map<String, Serializable> getFieldsMeta() {
        Map<String, Serializable> ret = stdMeta.get();
        if (ret != null) {
           return ret;
        }
        return stdMeta.updateAndGet(
                currentMap -> {
                    if (currentMap == null) {
                        TreeMap<String, Serializable> myMap =
                                fieldsMeta.entrySet().stream()
                                        .filter(
                                                entry ->
                                                        !entry.getKey()
                                                                .startsWith(CUSTOM_PROP_PREFIX))
                                        .collect(
                                                Collectors.toMap(
                                                        Entry::getKey,
                                                        Entry::getValue,
                                                        (oldValue, newValue) ->
                                                                oldValue,
                                                        TreeMap::new));
                        return ImmutableSortedMap.copyOf(myMap);
                    }
                    return currentMap;
                });
    }

    /**
     * Returns the custom metadata
     * @return customMeta
     */
    public Map<String, Serializable> getCustomMeta() {
        Map<String, Serializable> ret = customMeta.get();
        if (ret != null) {
            return ret;
        }
        return customMeta.updateAndGet(
                currentMap -> {
                    if (currentMap == null) {
                        TreeMap<String, Serializable> myMap =
                                fieldsMeta.entrySet().stream()
                                        .filter(
                                                entry ->
                                                        entry.getKey()
                                                                .startsWith(CUSTOM_PROP_PREFIX))
                                        .collect(
                                                Collectors.toMap(
                                                        e -> e.getKey().substring(CUSTOM_PROP_PREFIX.length()),
                                                        Entry::getValue,
                                                        (oldValue, newValue) ->
                                                                oldValue,
                                                        TreeMap::new));
                        return ImmutableSortedMap.copyOf(myMap);
                    }
                    return currentMap;
                });

    }

    /**
     * Returns the custom metadata with prefix
     * @return customMetaWithPrefix
     */
    public Map<String, Serializable> getCustomMetaWithPrefix() {
        Map<String, Serializable> ret = customMetaWithPrefix.get();
        if (ret != null) {
            return ret;
        }
        return customMetaWithPrefix.updateAndGet(
                currentMap -> {
                    if (currentMap == null) {
                        TreeMap<String, Serializable> myMap =
                                fieldsMeta.entrySet().stream()
                                        .filter(
                                                entry ->
                                                        entry.getKey()
                                                                .startsWith(CUSTOM_PROP_PREFIX))
                                        .collect(
                                                Collectors.toMap(
                                                        Entry::getKey,
                                                        Entry::getValue,
                                                        (oldValue, newValue) ->
                                                                oldValue,
                                                        TreeMap::new));
                        return ImmutableSortedMap.copyOf(myMap);
                    }
                    return currentMap;
                });
    }

    /**
     * Returns the field value for the given key
     * @param key The key for the field
     * @return The value of the field
     */
    public Serializable getField(String key) {
        return getFieldsMeta().get(key);
    }

    /**
     * Returns all metadata as a map
     * @return fieldsMeta
     */
    public Map<String, Serializable> getMap() {
        // See if we can remove merge this duplicate method
        return fieldsMeta;
    }

    /**
     * Returns the title of the metadata
     * @return title
     */
    public String getTitle(){
        return safelyCast(getFieldsMeta().get(TITLE_META_KEY.key()), String.class)
                .orElse(UNKNOWN);
    }

    /**
     * Returns the name of the metadata
     * @return name
     */
    public String getName(){
        return safelyCast(getFieldsMeta().get(NAME_META_KEY.key()), String.class)
                .orElse(UNKNOWN);
    }

    /**
     * Returns the length of the metadata
     * @return length
     */
    public int getLength() {
        return getNumericValue(LENGTH_META_KEY.key());
    }

    /**
     * Returns the size of the metadata
     * @return size
     */
    public int getSize() {
        return getNumericValue(SIZE_META_KEY.key());
    }

    /**
     * Returns the path of the metadata
     * @return path
     */
    public String getPath() {
        return safelyCast(getFieldsMeta().get(PATH_META_KEY.key()), String.class)
                .orElse(UNKNOWN);
    }

    /**
     * Returns the SHA256 hash of the metadata
     * @return sha256
     */
    public String getSha256() {
        return safelyCast(getFieldsMeta().get(SHA256_META_KEY.key()), String.class)
                .orElse(UNKNOWN);
    }

    /**
     * Returns the content type of the metadata
     * @return contentType
     */
    public String getContentType() {
        return safelyCast(getFieldsMeta().get(CONTENT_TYPE_META_KEY.key()), String.class)
                .orElse(UNKNOWN);
    }

    /**
     * Returns whether the metadata is an image
     * @return isImage
     */
    public boolean isImage() {
        return safelyCast(getFieldsMeta().get(IS_IMAGE_META_KEY.key()), Boolean.class)
                .orElse(false);
    }

    /**
     * Returns the width of the metadata
     * @return width
     */
    public int getWidth() {
        return getNumericValue(WIDTH_META_KEY.key());
    }

    /**
     * Returns the height of the metadata
     * @return height
     */
    public int getHeight() {
        return getNumericValue(HEIGHT_META_KEY.key());
    }

    /**
     * Returns the modification date of the metadata
     * @return modDate
     */
    public long getModDate() {
        return safelyCast(getFieldsMeta().get(MOD_DATE_META_KEY.key()), Number.class)
                .map(Number::longValue)
                .orElseGet(() -> {
                    Logger.debug(Metadata.class, () -> "Invalid or missing numeric value for modification date");
                    return 0L;
                });
    }

    /**
     * Returns the version of the metadata
     * @return version
     */
    public int getVersion() {
        return getNumericValue(VERSION_KEY.key());
    }

    /**
     * Returns whether the metadata is equal to the given object
     * @param o The object to compare
     * @return true if equal, false otherwise
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Metadata metadata = (Metadata) o;
        return Objects.equals(fieldName, metadata.fieldName) &&
                Objects.equals(fieldsMeta, metadata.fieldsMeta);
    }

    /**
     * Returns the hash code of the metadata
     * @return hashCode
     */
    @Override
    public int hashCode() {
        return Objects.hash(fieldName, fieldsMeta);
    }

    /**
     * Returns the string representation of the metadata
     * @return string representation
     */
    @Override
    public String toString() {
        return "Metadata{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldsMeta=" + fieldsMeta +
                ", customMeta=" + getCustomMeta() +
                '}';
    }

    // Helper method to safely cast types
    private <T> Optional<T> safelyCast(Object value, Class<T> type) {
        return Optional.ofNullable(value)
                .filter(type::isInstance)
                .map(type::cast);
    }

    // General method for getting numeric values safely
    private int getNumericValue(String key) {
        return safelyCast(getFieldsMeta().get(key), Number.class)
                .map(Number::intValue)
                .orElseGet(() -> {
                    Logger.debug(Metadata.class, () -> String.format("Invalid or missing numeric value for key `%s`", key));
                    return 0;
                });
    }
}
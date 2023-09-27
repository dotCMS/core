package com.dotcms.model.asset;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Enum representing basic metadata fields for assets.
 */
public enum BasicMetadataFields {

    VERSION_KEY("version"),
    NAME_META_KEY("name"),
    TITLE_META_KEY("title"),
    PATH_META_KEY("path"),
    CONTENT_TYPE_META_KEY("contentType"),
    SHA256_META_KEY("sha256"),
    IS_IMAGE_META_KEY("isImage", Boolean.TYPE),
    LENGTH_META_KEY("length", Integer.TYPE),
    SIZE_META_KEY("fileSize", Integer.TYPE),
    MOD_DATE_META_KEY("modDate", Integer.TYPE),
    WIDTH_META_KEY("width", Integer.TYPE),
    HEIGHT_META_KEY("height", Integer.TYPE);

    private final String key;
    private final Class<?> clazz;

    /**
     * Constructs a BasicMetadataFields enum constant with the specified key.
     * The value type is String by default.
     *
     * @param key the key associated with the metadata field
     */
    BasicMetadataFields(final String key) {
        this(key, String.class);
    }

    /**
     * Constructs a BasicMetadataFields enum constant with the specified key and value type.
     *
     * @param key   the key associated with the metadata field
     * @param clazz the value type of the metadata field
     */
    BasicMetadataFields(final String key, final Class<?> clazz) {
        this.key = key;
        this.clazz = clazz;
    }

    /**
     * Returns the key associated with the metadata field.
     *
     * @return the key of the metadata field
     */
    public String key() {
        return key;
    }

    /**
     * Returns the enum constant itself.
     *
     * @return the enum constant value
     */
    public BasicMetadataFields getValue() {
        return this;
    }

    /**
     * Checks if the value type of the metadata field is numeric.
     *
     * @return true if the value type is numeric, false otherwise
     */
    public boolean isNumericType() {
        return clazz == Integer.TYPE;
    }

    /**
     * Checks if the value type of the metadata field is boolean.
     *
     * @return true if the value type is boolean, false otherwise
     */
    public boolean isBooleanType() {
        return clazz == Boolean.TYPE;
    }

    /**
     * Checks if the value type of the metadata field is String.
     *
     * @return true if the value type is String, false otherwise
     */
    public boolean isStringType() {
        return clazz == String.class;
    }

    /**
     * Returns a map of metadata field keys and their corresponding enum constants.
     *
     * @return a map of metadata field keys and enum constants
     */
    public static Map<String, BasicMetadataFields> keyMap() {
        return Stream.of(BasicMetadataFields.values())
                .collect(Collectors.toMap(BasicMetadataFields::key, e -> e));
    }
}
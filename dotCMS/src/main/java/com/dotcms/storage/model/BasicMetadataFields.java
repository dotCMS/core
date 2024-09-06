package com.dotcms.storage.model;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This Enum provides the list of stand-alone metadata fields that can be calculated without having
 * to use Tika.
 *
 * @author Fabrizzio Araya
 * @since Mar 23rd, 2021
 */
public enum BasicMetadataFields {

    VERSION_KEY            ("version"),
    NAME_META_KEY          ("name"),
    TITLE_META_KEY         ("title"),
    PATH_META_KEY          ("path"),
    CONTENT_TYPE_META_KEY  ("contentType"),
    SHA256_META_KEY        ("sha256"),
    IS_IMAGE_META_KEY      ("isImage", Boolean.TYPE),
    LENGTH_META_KEY        ("length", Integer.TYPE),
    SIZE_META_KEY          ("fileSize", Integer.TYPE),
    MOD_DATE_META_KEY      ("modDate", Integer.TYPE),
    WIDTH_META_KEY         ("width", Integer.TYPE),
    HEIGHT_META_KEY        ("height", Integer.TYPE),
    EDITABLE_AS_TEXT       ("editableAsText", Boolean.TYPE);

    private final String key;
    private final Class<?> clazz;

    BasicMetadataFields(final String key) {
        this(key, String.class);
    }

    BasicMetadataFields(final String key, final Class<?> clazz) {
       this.key = key;
       this.clazz = clazz;
    }

    public String key(){
       return key;
    }

    public BasicMetadataFields getValue(){
      return this;
    }

    public boolean isNumericType(){
       return clazz == Integer.TYPE;
    }

    public boolean isBooleanType(){
        return clazz == Boolean.TYPE;
    }

    public boolean isStringType(){
        return clazz == String.class;
    }

    public static Map<String, BasicMetadataFields> keyMap() {
        return Stream.of(BasicMetadataFields.values())
                .collect(Collectors.toMap(BasicMetadataFields::key, e -> e));
    }

}

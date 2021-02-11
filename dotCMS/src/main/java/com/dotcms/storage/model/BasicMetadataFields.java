package com.dotcms.storage.model;

import com.google.common.collect.ImmutableSet.Builder;
import java.util.Set;

public enum BasicMetadataFields {

    //These are stand-alone metadata fields that can be calculated without having to use Tika

    TITLE_META_KEY         ("title"),
    PATH_META_KEY          ("path"),
    LENGTH_META_KEY        ("length"),
    SIZE_META_KEY          ("fileSize"),
    CONTENT_TYPE_META_KEY  ("contentType"),
    MOD_DATE_META_KEY      ("modDate"),
    SHA256_META_KEY        ("sha256"),
    IS_IMAGE_META_KEY      ("isImage"),
    WIDTH_META_KEY         ("width"),
    HEIGHT_META_KEY        ("height");

    private String key;

    BasicMetadataFields(final String key) {
       this.key = key;
    }

    public String key(){
       return key;
    }


    public static Set<String> keySet() {
        final Builder<String> builder = new Builder<>();
        for (final BasicMetadataFields field : values()) {
            builder.add(field.key());
        }
        return builder.build();
    }

}

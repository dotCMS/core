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
import static com.dotcms.storage.model.BasicMetadataFields.WIDTH_META_KEY;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import io.vavr.control.Try;
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

    public Metadata(final String fieldName,
            final Map<String, Serializable> fieldsMeta) {

        this.fieldName = fieldName;
        if (null != fieldsMeta) {
            this.fieldsMeta = fieldsMeta;
        } else {
            this.fieldsMeta = ImmutableMap.of();
        }
    }

    public String getFieldName() {
        return fieldName;
    }

    public Map<String, Serializable> getFieldsMeta() {
        return fieldsMeta.entrySet().stream()
                .filter(entry -> !entry.getKey().startsWith(CUSTOM_PROP_PREFIX))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));

    }

    public Map<String, Serializable> getCustomMeta() {
        return new ImmutableSortedMap.Builder<String, Serializable>(
                Comparator.naturalOrder()).putAll(fieldsMeta.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(CUSTOM_PROP_PREFIX))
                .collect(Collectors.toMap(o ->
                        o.getKey().substring(CUSTOM_PROP_PREFIX.length()
                        ), Entry::getValue))).build();
    }

    public Map<String, Serializable> getCustomMetaWithPrefix() {
        return new ImmutableSortedMap.Builder<String, Serializable>(
                Comparator.naturalOrder()).putAll(fieldsMeta.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(CUSTOM_PROP_PREFIX))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue))).build();
    }

    public Map<String, Serializable> getMap() {
        return new ImmutableSortedMap.Builder<String, Serializable>(Comparator.naturalOrder()).putAll(fieldsMeta).build();
    }

    public String getTitle(){
        return Try.of(()->getFieldsMeta().get(TITLE_META_KEY.key()).toString()).getOrElse("unknown");
    }

    public String getName(){
        return Try.of(()->getFieldsMeta().get(NAME_META_KEY.key()).toString()).getOrElse("unknown");
    }

    public int getLength(){
        return Try.of(()->(int)getFieldsMeta().get(LENGTH_META_KEY.key())).getOrElse(0);
    }

    public int getSize(){
        return Try.of(()->(int)getFieldsMeta().get(SIZE_META_KEY.key())).getOrElse(0);
    }

    public String getPath(){
        return Try.of(()->getFieldsMeta().get(PATH_META_KEY.key()).toString()).getOrElse("unknown");
    }

    public String getSha256(){
        return Try.of(()->getFieldsMeta().get(SHA256_META_KEY.key()).toString()).getOrElse("unknown");
    }

    public String getContentType(){
       return Try.of(()->getFieldsMeta().get(CONTENT_TYPE_META_KEY.key()).toString()).getOrElse("unknown");
    }

    public boolean isImage(){
        try{
           final Object value = getFieldsMeta().get(IS_IMAGE_META_KEY.key());
           if(value instanceof Boolean){
              return (Boolean) value;
           }
        }catch (Exception e){
           //quite
        }
        return false;
    }

    public int getWidth(){
        return Try.of(()->(int)getFieldsMeta().get(WIDTH_META_KEY.key())).getOrElse(0);
    }

    public int getHeight(){
        return Try.of(()->(int)getFieldsMeta().get(HEIGHT_META_KEY.key())).getOrElse(0);
    }

    public long getModDate(){
        return Try.of(()->(long)getFieldsMeta().get(MOD_DATE_META_KEY.key())).getOrElse(0L);
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
                fieldsMeta.hashCode() == metadata.fieldsMeta.hashCode();
    }

    @Override
    public int hashCode() {

        return Objects.hash(fieldName, fieldsMeta);
    }

    @Override
    public String toString() {
        return "Metadata{" +
                "fieldName='" + fieldName + '\'' +
                ", fieldsMeta=" + getFieldsMeta() +
                ", customMeta=" + getCustomMeta() +
                '}';
    }
}

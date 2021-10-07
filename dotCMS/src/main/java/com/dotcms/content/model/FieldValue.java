package com.dotcms.content.model;


import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.dotcms.content.model.type.AbstractBinaryType;
import com.dotcms.content.model.type.AbstractBoolType;
import com.dotcms.content.model.type.AbstractDateTimeType;
import com.dotcms.content.model.type.AbstractDateType;
import com.dotcms.content.model.type.AbstractFloatType;
import com.dotcms.content.model.type.AbstractKeyValueType;
import com.dotcms.content.model.type.AbstractListType;
import com.dotcms.content.model.type.AbstractLongTextType;
import com.dotcms.content.model.type.AbstractLongType;
import com.dotcms.content.model.type.AbstractTextType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Parameter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "type")

// Adds coupling to register subtypes here.  Can be changed to scan annotations
// to register subtypes on initialization of ObjectMapper;
@JsonSubTypes({
        @JsonSubTypes.Type(name = AbstractBinaryType.TYPENAME, value = AbstractBinaryType.class),
        @JsonSubTypes.Type(name = AbstractDateType.TYPENAME, value = AbstractDateType.class),
        @JsonSubTypes.Type(name = AbstractDateTimeType.TYPENAME, value = AbstractDateTimeType.class),
        @JsonSubTypes.Type(name = AbstractFloatType.TYPENAME, value = AbstractFloatType.class),
        @JsonSubTypes.Type(name = AbstractTextType.TYPENAME, value = AbstractTextType.class),
        @JsonSubTypes.Type(name = AbstractKeyValueType.TYPENAME, value = AbstractKeyValueType.class),
        @JsonSubTypes.Type(name = AbstractListType.TYPENAME, value = AbstractListType.class),
        @JsonSubTypes.Type(name = AbstractLongTextType.TYPENAME, value = AbstractLongTextType.class),
        @JsonSubTypes.Type(name = AbstractLongType.TYPENAME, value = AbstractLongType.class),
        @JsonSubTypes.Type(name = AbstractTextType.TYPENAME, value = AbstractTextType.class),
        @JsonSubTypes.Type(name = AbstractBoolType.TYPENAME, value = AbstractBoolType.class),
})
@ValueTypeStyle
@JsonDeserialize(as = FieldValue.class)
public interface FieldValue<T> {

    @JsonProperty("type")
    default String type() {
        return "unknown";
    };

    @JsonProperty("value")
    @Parameter
    T value();

}


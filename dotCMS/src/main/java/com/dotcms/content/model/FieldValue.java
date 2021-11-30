package com.dotcms.content.model;


import com.dotcms.content.model.annotation.ValueType;
import com.dotcms.content.model.type.AbstractCheckBoxFieldType;
import com.dotcms.content.model.type.AbstractCustomFieldType;
import com.dotcms.content.model.type.AbstractFileFieldType;
import com.dotcms.content.model.type.AbstractImageFieldType;
import com.dotcms.content.model.type.AbstractStoryBlockFieldType;
import com.dotcms.content.model.type.AbstractTextAreaType;
import com.dotcms.content.model.type.AbstractWysiwygType;
import com.dotcms.content.model.type.date.AbstractDateFieldType;
import com.dotcms.content.model.type.date.AbstractDateTimeFieldType;
import com.dotcms.content.model.type.date.AbstractTimeFieldType;
import com.dotcms.content.model.type.hidden.AbstractBoolHiddenFieldType;
import com.dotcms.content.model.type.hidden.AbstractDateHiddenFieldType;
import com.dotcms.content.model.type.hidden.AbstractFloatHiddenFieldType;
import com.dotcms.content.model.type.hidden.AbstractHiddenFieldType;
import com.dotcms.content.model.type.hidden.AbstractLongHiddenFieldType;
import com.dotcms.content.model.type.keyvalue.AbstractKeyValueType;
import com.dotcms.content.model.type.radio.AbstractBoolRadioFieldType;
import com.dotcms.content.model.type.radio.AbstractFloatRadioFieldType;
import com.dotcms.content.model.type.radio.AbstractLongRadioFieldType;
import com.dotcms.content.model.type.radio.AbstractRadioFieldType;
import com.dotcms.content.model.type.select.AbstractBoolSelectFieldType;
import com.dotcms.content.model.type.select.AbstractFloatSelectFieldType;
import com.dotcms.content.model.type.select.AbstractLongSelectFieldType;
import com.dotcms.content.model.type.select.AbstractMultiSelectFieldType;
import com.dotcms.content.model.type.select.AbstractSelectFieldType;
import com.dotcms.content.model.type.system.AbstractBinaryFieldType;
import com.dotcms.content.model.type.system.AbstractCategoryFieldType;
import com.dotcms.content.model.type.system.AbstractConstantFieldType;
import com.dotcms.content.model.type.system.AbstractTagFieldType;
import com.dotcms.content.model.type.text.AbstractFloatTextFieldType;
import com.dotcms.content.model.type.text.AbstractLongTextFieldType;
import com.dotcms.content.model.type.text.AbstractTextFieldType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import org.immutables.value.Value.Parameter;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.EXISTING_PROPERTY, property = "type")

/**
 * This is base class to Map all Field types that a contentlet could have
 * It has been instructed via annotations how to deal with it's descendants when translating to json
 * Adds coupling to register subtypes here.  Can be changed to scan annotations
 * to register subtypes on initialization of ObjectMapper;
 * Also see {@link com.dotcms.contenttype.model.field.Field} and its descendants
 */
@JsonSubTypes({

        //Image
        @JsonSubTypes.Type(name = AbstractImageFieldType.TYPENAME, value = AbstractImageFieldType.class),
        //File
        @JsonSubTypes.Type(name = AbstractFileFieldType.TYPENAME, value = AbstractFileFieldType.class),

        //Dates
        @JsonSubTypes.Type(name = AbstractTimeFieldType.TYPENAME, value = AbstractTimeFieldType.class),
        @JsonSubTypes.Type(name = AbstractDateFieldType.TYPENAME, value = AbstractDateFieldType.class),
        @JsonSubTypes.Type(name = AbstractDateTimeFieldType.TYPENAME, value = AbstractDateTimeFieldType.class),

        //Text Fields
        @JsonSubTypes.Type(name = AbstractTextFieldType.TYPENAME, value = AbstractTextFieldType.class),
        @JsonSubTypes.Type(name = AbstractFloatTextFieldType.TYPENAME, value = AbstractFloatTextFieldType.class),
        @JsonSubTypes.Type(name = AbstractLongTextFieldType.TYPENAME, value = AbstractLongTextFieldType.class),

        //Radios
        @JsonSubTypes.Type(name = AbstractRadioFieldType.TYPENAME, value = AbstractRadioFieldType.class),
        @JsonSubTypes.Type(name = AbstractBoolRadioFieldType.TYPENAME, value = AbstractBoolRadioFieldType.class),
        @JsonSubTypes.Type(name = AbstractFloatRadioFieldType.TYPENAME, value = AbstractFloatRadioFieldType.class),
        @JsonSubTypes.Type(name = AbstractLongRadioFieldType.TYPENAME, value = AbstractLongRadioFieldType.class),

        //Multi-Select
        @JsonSubTypes.Type(name = AbstractMultiSelectFieldType.TYPENAME, value = AbstractMultiSelectFieldType.class),
        //Single-Select
        @JsonSubTypes.Type(name = AbstractSelectFieldType.TYPENAME, value = AbstractSelectFieldType.class),
        @JsonSubTypes.Type(name = AbstractBoolSelectFieldType.TYPENAME, value = AbstractBoolSelectFieldType.class),
        @JsonSubTypes.Type(name = AbstractLongSelectFieldType.TYPENAME, value = AbstractLongSelectFieldType.class),
        @JsonSubTypes.Type(name = AbstractFloatSelectFieldType.TYPENAME, value = AbstractFloatSelectFieldType.class),

        //Checkbox
        @JsonSubTypes.Type(name = AbstractCheckBoxFieldType.TYPENAME, value = AbstractCheckBoxFieldType.class),

        //Key Values and List
        @JsonSubTypes.Type(name = AbstractKeyValueType.TYPENAME, value = AbstractKeyValueType.class),

        //Wysiwyg
        @JsonSubTypes.Type(name = AbstractWysiwygType.TYPENAME, value = AbstractWysiwygType.class),

        //TextArea
        @JsonSubTypes.Type(name = AbstractTextAreaType.TYPENAME, value = AbstractTextAreaType.class),

        //StoryBlockField
        @JsonSubTypes.Type(name = AbstractStoryBlockFieldType.TYPENAME, value = AbstractStoryBlockFieldType.class),

        //CustomField
        @JsonSubTypes.Type(name = AbstractCustomFieldType.TYPENAME, value = AbstractCustomFieldType.class),

        //HiddenField
        @JsonSubTypes.Type(name = AbstractHiddenFieldType.TYPENAME, value = AbstractHiddenFieldType.class),
        @JsonSubTypes.Type(name = AbstractBoolHiddenFieldType.TYPENAME, value = AbstractBoolHiddenFieldType.class),
        @JsonSubTypes.Type(name = AbstractFloatHiddenFieldType.TYPENAME, value = AbstractFloatHiddenFieldType.class),
        @JsonSubTypes.Type(name = AbstractLongHiddenFieldType.TYPENAME, value = AbstractLongHiddenFieldType.class),
        @JsonSubTypes.Type(name = AbstractDateHiddenFieldType.TYPENAME, value = AbstractDateHiddenFieldType.class),

        //System Fields (These are here so they can be included in the json for import export purposes)

        //BinaryField
        @JsonSubTypes.Type(name = AbstractBinaryFieldType.TYPENAME, value = AbstractBinaryFieldType.class),
        //Category Field
        @JsonSubTypes.Type(name = AbstractCategoryFieldType.TYPENAME, value = AbstractCategoryFieldType.class),
        //Tag Fields
        @JsonSubTypes.Type(name = AbstractTagFieldType.TYPENAME, value = AbstractTagFieldType.class),
        @JsonSubTypes.Type(name = AbstractConstantFieldType.TYPENAME, value = AbstractConstantFieldType.class),

})
@ValueType
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public interface FieldValue<T> {

    /**
     * Type property instructs framework how to map type
     * @return
     */
    @JsonProperty("type")
    default String type() {
        return "unknown";
    };

    /**
     * Parametrized Type property instructs framework how to map value
     * @return
     */
    @JsonProperty("value")
    @Parameter
    T value();

}


package com.dotcms.content.model.type.text;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Regular Text Value Text-Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = TextFieldType.class)
@JsonTypeName(value = AbstractTextFieldType.TYPENAME)
public interface AbstractTextFieldType extends FieldValue<String> {

    String TYPENAME = "Text";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    };

    /**
     * {@inheritDoc}
     */
    @JsonProperty("value")
    @Parameter
    String value();
}

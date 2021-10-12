package com.dotcms.content.model.type.text;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Float Stored Value Text-Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = FloatTextFieldType.class)
@JsonTypeName(value = AbstractFloatTextFieldType.TYPENAME)
public interface AbstractFloatTextFieldType extends FieldValue<Float> {

    String TYPENAME = "Float";

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
    Float value();

}

package com.dotcms.content.model.type.radio;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Long Radio Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = LongRadioFieldType.class)
@JsonTypeName(value = AbstractLongRadioFieldType.TYPENAME)
public interface AbstractLongRadioFieldType extends FieldValue<Long> {

    String TYPENAME = "LongRadio";

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
    Long value();

}

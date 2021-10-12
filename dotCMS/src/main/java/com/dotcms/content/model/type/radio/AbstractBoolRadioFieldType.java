package com.dotcms.content.model.type.radio;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Boolean Radio Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = BoolRadioFieldType.class)
@JsonTypeName(value = AbstractBoolRadioFieldType.TYPENAME)
public interface AbstractBoolRadioFieldType extends FieldValue<Boolean> {

    String TYPENAME = "BoolRadio";

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
    Boolean value();

}

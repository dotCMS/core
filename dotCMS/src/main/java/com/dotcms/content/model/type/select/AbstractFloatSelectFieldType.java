package com.dotcms.content.model.type.select;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Float Single-Select Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = FloatSelectFieldType.class)
@JsonTypeName(value = AbstractFloatSelectFieldType.TYPENAME)
public interface AbstractFloatSelectFieldType extends FieldValue<Float> {

    String TYPENAME = "FloatSelect";

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

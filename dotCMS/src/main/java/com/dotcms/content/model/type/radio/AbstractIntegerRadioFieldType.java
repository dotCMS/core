package com.dotcms.content.model.type.radio;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Float Radio Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = IntegerRadioFieldType.class)
@JsonTypeName(value = AbstractIntegerRadioFieldType.TYPENAME)
public interface AbstractIntegerRadioFieldType extends FieldValue<Integer> {

    String TYPENAME = "Integer-Radio";

    /**
     * {@inheritDoc}
     */
    @Override
    default String type() {
        return TYPENAME;
    }

    ;

    /**
     * {@inheritDoc}
     */
    @JsonProperty("value")
    @Parameter
    Integer value();

}

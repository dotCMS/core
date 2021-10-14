package com.dotcms.content.model.type.text;


import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Long Value Stored Text-Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = IntegerTextFieldType.class)
@JsonTypeName(value = AbstractIntegerTextFieldType.TYPENAME)
public interface AbstractIntegerTextFieldType extends FieldValue<Integer> {

    String TYPENAME = "Integer";

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
    Integer value();

}

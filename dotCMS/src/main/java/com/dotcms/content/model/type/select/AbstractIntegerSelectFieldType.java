package com.dotcms.content.model.type.select;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Long Integer-Select Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = IntegerSelectFieldType.class)
@JsonTypeName(value = AbstractIntegerSelectFieldType.TYPENAME)
public interface AbstractIntegerSelectFieldType extends FieldValue<Integer> {

    String TYPENAME = "Integer-Select";

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

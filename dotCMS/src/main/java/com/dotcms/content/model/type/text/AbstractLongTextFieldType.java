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
@JsonDeserialize(as = LongTextFieldType.class)
@JsonTypeName(value = AbstractLongTextFieldType.TYPENAME)
public interface AbstractLongTextFieldType extends FieldValue<Long> {

    String TYPENAME = "Long";

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

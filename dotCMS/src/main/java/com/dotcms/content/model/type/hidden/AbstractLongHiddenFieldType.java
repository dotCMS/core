package com.dotcms.content.model.type.hidden;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Custom Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = LongHiddenFieldType.class)
@JsonTypeName(value = AbstractLongHiddenFieldType.TYPENAME)
public interface AbstractLongHiddenFieldType extends FieldValue<Long> {

    String TYPENAME = "Long-Hidden";

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

package com.dotcms.content.model.type.select;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Text Multi-Select Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = MultiSelectFieldType.class)
@JsonTypeName(value = AbstractMultiSelectFieldType.TYPENAME)
public interface AbstractMultiSelectFieldType extends FieldValue<String> {

    String TYPENAME = "Multi-Select";

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

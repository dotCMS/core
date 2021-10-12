package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Binary Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = BinaryType.class)
@JsonTypeName(value = AbstractBinaryType.TYPENAME)
public interface AbstractBinaryType extends FieldValue<String> {

    String TYPENAME = "Binary";

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

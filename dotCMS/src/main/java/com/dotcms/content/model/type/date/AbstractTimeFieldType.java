package com.dotcms.content.model.type.date;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

/**
 * Time Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = TimeFieldType .class)
@JsonTypeName(value = AbstractTimeFieldType.TYPENAME)
public interface AbstractTimeFieldType extends FieldValue<Instant> {

    String TYPENAME = "Time";

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
    Instant value();

}

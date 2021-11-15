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
 * DateTime Field json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = DateTimeFieldType.class)
@JsonTypeName(value = AbstractDateTimeFieldType.TYPENAME)
public interface AbstractDateTimeFieldType extends FieldValue<Instant> {

    String TYPENAME = "DateTime";

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    default String type() {
        return TYPENAME;
    };

    /**
     * {@inheritDoc}
     * @return
     */
    @JsonProperty("value")
    @Parameter
    Instant value();

}

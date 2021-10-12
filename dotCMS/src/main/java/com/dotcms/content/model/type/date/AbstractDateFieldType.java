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
 * DateField json representation
 */
@ValueTypeStyle
@Immutable
@JsonDeserialize(as = DateFieldType.class)
@JsonTypeName(value = AbstractDateFieldType.TYPENAME)
public interface AbstractDateFieldType extends FieldValue<Instant> {

    String TYPENAME = "Date";

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

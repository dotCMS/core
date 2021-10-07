package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@ValueTypeStyle
@Immutable
@JsonDeserialize(as = DateTimeType.class)
@JsonTypeName(value = AbstractDateTimeType.TYPENAME)
public interface AbstractDateTimeType extends FieldValue<Instant> {

    String TYPENAME = "date-time";

    @Override
    default String type() {
        return TYPENAME;
    };

    @JsonProperty("value")
    @Parameter
    Instant value();

}

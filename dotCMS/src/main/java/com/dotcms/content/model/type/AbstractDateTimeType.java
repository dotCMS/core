package com.dotcms.content.model.type;

import com.dotcms.content.model.type.DateTimeType;
import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.dotcms.contenttype.model.field.BinaryField;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Instant;
import org.immutables.value.Value.Auxiliary;
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

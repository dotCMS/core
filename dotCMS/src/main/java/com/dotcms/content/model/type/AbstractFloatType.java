package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.FloatType;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.dotcms.contenttype.model.field.DateTimeField;
import com.dotcms.contenttype.model.field.TextField;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Auxiliary;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@ValueTypeStyle
@Immutable
@JsonDeserialize(as = FloatType.class)
@JsonTypeName(value = AbstractFloatType.TYPENAME)
public interface AbstractFloatType extends FieldValue<Float> {

    String TYPENAME = "Float";

    @Override
    default String type() {
        return TYPENAME;
    };

    @JsonProperty("value")
    @Parameter
    Float value();

}

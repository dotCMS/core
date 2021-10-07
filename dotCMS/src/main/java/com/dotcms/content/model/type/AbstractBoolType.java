package com.dotcms.content.model.type;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.annotation.ValueTypeStyle;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@ValueTypeStyle
@Immutable
@JsonDeserialize(as = BoolType.class)
@JsonTypeName(value = AbstractBoolType.TYPENAME)
public interface AbstractBoolType extends FieldValue<Boolean> {

    String TYPENAME = "Bool";

    @Override
    default String type() {
        return TYPENAME;
    };

    @JsonProperty("value")
    @Parameter
    Boolean value();

}

package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableJSONField.class)
@JsonDeserialize(as = ImmutableJSONField.class)
public abstract class JSONField extends Field{

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.LONG_TEXT;
    }
}

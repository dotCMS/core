package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableConstantField.class)
@JsonDeserialize(as = ImmutableConstantField.class)
public abstract class ConstantField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.SYSTEM;
    };

}

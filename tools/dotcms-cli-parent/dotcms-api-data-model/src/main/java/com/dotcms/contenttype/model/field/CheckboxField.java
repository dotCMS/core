package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCheckboxField.class)
@JsonDeserialize(as = ImmutableCheckboxField.class)
public abstract class CheckboxField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.TEXT;
    };

}

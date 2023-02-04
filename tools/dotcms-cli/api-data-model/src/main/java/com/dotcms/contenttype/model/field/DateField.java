package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableDateField.class)
@JsonDeserialize(as = ImmutableDateField.class)
public abstract class DateField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.DATE;
    };

}

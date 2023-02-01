package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRadioField.class)
@JsonDeserialize(as = ImmutableRadioField.class)
public abstract class RadioField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.TEXT;
    };

}

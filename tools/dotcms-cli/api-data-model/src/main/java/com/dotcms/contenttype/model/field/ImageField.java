package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableImageField.class)
@JsonDeserialize(as = ImmutableImageField.class)
public abstract class ImageField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.TEXT;
    };

}

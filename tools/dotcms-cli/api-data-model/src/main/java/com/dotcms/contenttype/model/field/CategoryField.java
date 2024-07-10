package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableCategoryField.class)
@JsonDeserialize(as = ImmutableCategoryField.class)
public abstract class CategoryField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.SYSTEM;
    }

    @Nullable
    public abstract Category categories();

}


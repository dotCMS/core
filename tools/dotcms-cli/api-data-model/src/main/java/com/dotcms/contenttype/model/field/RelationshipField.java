package com.dotcms.contenttype.model.field;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableRelationshipField.class)
@JsonDeserialize(as = ImmutableRelationshipField.class)
public abstract class RelationshipField extends Field {

    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.SYSTEM;
    }

    @Nullable
    public abstract Relationships relationships();

    @Value.Default
    @Nullable
    public Boolean skipRelationshipCreation() {
        return false;
    }

}

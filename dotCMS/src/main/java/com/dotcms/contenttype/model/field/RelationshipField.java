package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collection;
import java.util.List;
import org.immutables.value.Value;

/**
 * Content type's field to use relationships
 * @author nollymar
 */
@JsonSerialize(as = ImmutableRelationshipField.class)
@JsonDeserialize(as = ImmutableRelationshipField.class)
@Value.Immutable
public abstract class RelationshipField extends Field{

    private static final long serialVersionUID = 1L;

    @Override
    public Class type() {
        return RelationshipField.class;
    }

    @Value.Default
    @Override
    public boolean indexed() {
        return true;
    };
    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.SYSTEM;
    }
    @Override
    public final List<DataTypes> acceptedDataTypes() {
        return ImmutableList.of(DataTypes.SYSTEM);
    }

    @Value.Default
    public boolean skipRelationshipCreation() {
        return false;
    }

    public abstract static class Builder implements RelationshipFieldBuilder {

    }

    @JsonIgnore
    public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
        return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
                ContentTypeFieldProperties.RELATIONSHIPS, ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.SEARCHABLE);
    }
}

package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Collection;
import java.util.List;
import org.immutables.value.Value;

/**
 * @author nollymar
 */
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
    public abstract static class Builder implements FieldBuilder {}

    @JsonIgnore
    public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
        return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
                ContentTypeFieldProperties.RELATIONSHIPS, ContentTypeFieldProperties.VALUES,
                ContentTypeFieldProperties.SEARCHABLE);
    }
}

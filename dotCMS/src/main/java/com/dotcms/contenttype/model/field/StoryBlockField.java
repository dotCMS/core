package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableStoryBlockField.class)
@JsonDeserialize(as = ImmutableStoryBlockField.class)
@Value.Immutable
public abstract class StoryBlockField extends Field {


    private static final long serialVersionUID = 1L;

    @Override
    public  Class type() {
        return  StoryBlockField.class;
    }

    @JsonIgnore
    @Value.Derived
    @Override
    public List<DataTypes> acceptedDataTypes(){
        return ImmutableList.of(DataTypes.LONG_TEXT);
    }
    @Value.Default
    @Override
    public DataTypes dataType(){
        return DataTypes.LONG_TEXT;
    };
    public abstract static class Builder implements FieldBuilder {}

    @JsonIgnore
    public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
        return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
                ContentTypeFieldProperties.DEFAULT_VALUE, ContentTypeFieldProperties.HINT,
                ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED);
    }
}
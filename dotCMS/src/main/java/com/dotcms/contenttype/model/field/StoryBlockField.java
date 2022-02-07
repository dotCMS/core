package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.StoryBlockFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
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

    /**
     * {@inheritDoc}
     * @return
     */
    @Override
    public Optional<FieldValueBuilder> fieldValue(Object value) {
        if (value instanceof String) {
            return Optional.of(StoryBlockFieldType.builder().value((String) value));
        }
        return Optional.empty();
    }
    
    	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Story-Block";
	}
}

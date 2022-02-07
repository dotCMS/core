package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.date.TimeFieldType;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import java.util.Optional;
import org.immutables.value.Value;

import com.dotcms.contenttype.util.FieldUtil;
import com.google.common.collect.ImmutableList;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableTimeField.class)
@JsonDeserialize(as = ImmutableTimeField.class)
@Value.Immutable
public abstract class TimeField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  TimeField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.DATE);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.DATE;
	};
	public abstract static class Builder implements FieldBuilder {}
	
    @Value.Check
    public void check() {

        if(UtilMethods.isSet(defaultValue())){
          Preconditions.checkArgument(new FieldUtil().validTime(defaultValue()), this.getClass().getSimpleName() + " invalid default Value:" + defaultValue());
        }
    }

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED);
	}

	/**
	 * {@inheritDoc}
     * @return
     */
	@Override
	public Optional<FieldValueBuilder> fieldValue(Object value) {
		if (value instanceof Date) {
			return Optional.of(TimeFieldType.builder().value(((Date) value).toInstant()));
		}
		return Optional.empty();
	}
}

package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.date.DateTimeFieldType;
import com.dotmarketing.portlets.rules.parameter.type.DateTimeType;
import java.time.Instant;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import java.util.Optional;
import org.immutables.value.Value;

import com.dotcms.contenttype.util.FieldUtil;
import com.google.common.collect.ImmutableList;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableDateTimeField.class)
@JsonDeserialize(as = ImmutableDateTimeField.class)
@Value.Immutable
public abstract class DateTimeField extends Field {


	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  DateTimeField.class;
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.DATE;
	};

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.DATE);
	}
	public abstract static class Builder implements FieldBuilder {}
	
    @Value.Check
    public void check() {

        Preconditions.checkArgument(new FieldUtil().validDateTime(defaultValue()), this.getClass().getSimpleName() + " invalid default Value:" + defaultValue());

    }

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Date-and-Time";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<FieldValue<?>> fieldValue(Object value) {
		if (value instanceof Date) {
			return Optional.of(DateTimeFieldType.of(((Date) value).toInstant()));
		}
		return Optional.empty();
	}
}

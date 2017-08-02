package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import org.immutables.value.Value;

import com.dotcms.contenttype.util.FieldUtil;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
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
		return list(ContentTypeFieldProperties.LABEL, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.DEFAULT_TEXT,
				ContentTypeFieldProperties.USER_SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED, ContentTypeFieldProperties.DATA_TYPE, ContentTypeFieldProperties.RADIO_DATE);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Date-and-Time";
	}
}

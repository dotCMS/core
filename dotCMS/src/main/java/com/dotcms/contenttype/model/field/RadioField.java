package com.dotcms.contenttype.model.field;


import static com.dotcms.util.CollectionsUtils.list;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.radio.BoolRadioFieldType;
import com.dotcms.content.model.type.radio.FloatRadioFieldType;
import com.dotcms.content.model.type.radio.LongRadioFieldType;
import com.dotcms.content.model.type.radio.RadioFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableRadioField.class)
@JsonDeserialize(as = ImmutableRadioField.class)
@Value.Immutable
public abstract class RadioField extends SelectableValuesField {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Override
	public  Class type() {
		return  RadioField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.TEXT, DataTypes.BOOL, DataTypes.FLOAT,DataTypes.INTEGER);
	}
	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};
	

	
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.VALUES, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED, ContentTypeFieldProperties.DATA_TYPE);
	}

	/**
	 * {@inheritDoc}
     * @return
     */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value){
      if(null != value) {
		  if (value instanceof String) {
			  return Optional.of(RadioFieldType.builder().value((String) value));
		  }

		  if (value instanceof Boolean) {
			  return Optional.of(BoolRadioFieldType.builder().value((Boolean) value));
		  }

		  if (value instanceof Long) {
			  return Optional.of(LongRadioFieldType.builder().value((Long) value));
		  }

		  if (value instanceof Integer) {
			  return Optional.of(LongRadioFieldType.builder().value(((Integer) value).longValue()));
		  }

		  if (value instanceof Float) {
			  return Optional.of(FloatRadioFieldType.builder().value((Float) value));
		  }
	  } else {
		  final DataTypes dataType = dataType();
		  switch (dataType) {
			  case BOOL:
				  return Optional.of(BoolRadioFieldType.builder().value((false)));
			  case FLOAT:
				  return Optional.of(FloatRadioFieldType.builder().value((0F)));
			  case INTEGER:
				  return Optional.of(LongRadioFieldType.builder().value((0L)));
		  }
	  }
	  return Optional.empty();
	}

}

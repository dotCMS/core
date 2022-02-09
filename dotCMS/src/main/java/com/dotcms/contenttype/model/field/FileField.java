package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.business.Unexportable;
import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.FileFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableFileField.class)
@JsonDeserialize(as = ImmutableFileField.class)
@Value.Immutable
@Unexportable
public abstract class FileField extends Field {

	@Override
	public Class type() {
		return FileField.class;
	}
	private static final long serialVersionUID = 1L;

	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.TEXT;
	};

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes(){
		return ImmutableList.of(DataTypes.TEXT);
	}
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.HINT);
	}

	/**
	 * {@inheritDoc}
     * @return
     */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value){
		if (value instanceof String) {
			return Optional.of(FileFieldType.builder().value((String) value));
		}
		return Optional.empty();
	}
}

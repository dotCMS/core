package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableBinaryField.class)
@JsonDeserialize(as = ImmutableBinaryField.class)
@Value.Immutable
public abstract class BinaryField extends Field {

	private static final long serialVersionUID = 1L;

	@Override
	public Class type() {
		return BinaryField.class;
	}
	
	@Override
	public List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.SYSTEM);
	}

	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.SYSTEM;
	}

	
	public abstract static class Builder implements FieldBuilder {}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
	return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME,
				ContentTypeFieldProperties.HINT);
	}
}

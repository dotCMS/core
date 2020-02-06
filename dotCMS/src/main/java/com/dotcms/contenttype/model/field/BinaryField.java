package com.dotcms.contenttype.model.field;

import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.immutables.value.Value;

import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableBinaryField.class)
@JsonDeserialize(as = ImmutableBinaryField.class)
@Value.Immutable
public abstract class BinaryField extends Field {

	/**
	 * Supports things such as application/pdf or application/*
	 */
	public static final String ALLOWED_FILE_TYPES = "allowedFileTypes";

	/**
	 * Supports things such as 100 (100 bytes) 1kb (1024 bytes) 1mb (1024 kb) 1gb (1024 mb)
	 */
	public static final String MAX_FILE_LENGTH    = "maxFileLength";

	private static final long serialVersionUID = 1L;

	@JsonIgnore
	@Override
	public List<String> fieldVariableKeys() {
		return ImmutableList.of(ALLOWED_FILE_TYPES, MAX_FILE_LENGTH);
	}

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

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
	 * Constant for limit which type of files are allowed on the field.
	 * Field Variable is called accept since that is the name of the
	 * html attribute to specify what file types the user can pick.
	 *
	 * Supports things such as application/pdf or application/*
	 */
	public static final String ALLOWED_FILE_TYPES = "accept";

	/**
	 * Supports things such as 100 (100 bytes) 1kb (1024 bytes) 1mb (1024 kb) 1gb (1024 mb)
	 */
	public static final String MAX_FILE_LENGTH    = "maxFileLength";

	/**
	 * By default we use the value INDEX_METADATA_FIELDS on dotmarketing-config.properties, but you it can be set comma separated list for a single field to override the global value.
	 */
	public static final String INDEX_METADATA_FIELDS    = "indexMetadataFields";

	private static final long serialVersionUID = 1L;

	@JsonIgnore
	@Override
	public List<String> fieldVariableKeys() {
		return ImmutableList.of(ALLOWED_FILE_TYPES, MAX_FILE_LENGTH, INDEX_METADATA_FIELDS);
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
			    ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.HINT, ContentTypeFieldProperties.LISTED);
	}
}

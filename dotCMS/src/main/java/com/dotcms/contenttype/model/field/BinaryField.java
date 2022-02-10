package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.system.BinaryFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

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

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value){
		if (value instanceof String) {
			return Optional.of(BinaryFieldType.builder().value((String) value));
		}

		if (value instanceof File) {
			final File file = (File) value;
			return Optional.of(BinaryFieldType.builder().value(file.getName()));
		}

		if (value instanceof Path) {
			final Path path = (Path) value;
			return Optional.of(BinaryFieldType.builder().value(path.getFileName().toString()));
		}

		return Optional.empty();
	}

}

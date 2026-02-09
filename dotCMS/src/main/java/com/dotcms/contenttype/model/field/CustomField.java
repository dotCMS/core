package com.dotcms.contenttype.model.field;

import com.dotcms.content.model.FieldValueBuilder;
import com.dotcms.content.model.type.CustomFieldType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import org.immutables.value.Value;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static com.dotcms.util.CollectionsUtils.list;

@JsonSerialize(as = ImmutableCustomField.class)
@JsonDeserialize(as = ImmutableCustomField.class)
@Value.Immutable
public abstract class CustomField extends Field {

	private static final long serialVersionUID = 1L;

	@Override
	public Class type() {
		return CustomField.class;
	}

	@Value.Default
	@Override
	public DataTypes dataType(){
		return DataTypes.LONG_TEXT;
	};

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.LONG_TEXT,DataTypes.TEXT);
	}

	public abstract static class Builder implements FieldBuilder {
	}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.REQUIRED, ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.VALUES,
				ContentTypeFieldProperties.REGEX_CHECK, ContentTypeFieldProperties.DEFAULT_VALUE,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED,
				ContentTypeFieldProperties.LISTED, ContentTypeFieldProperties.UNIQUE, ContentTypeFieldProperties.HINT);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "Custom-Field";
	}

	/**
	 * {@inheritDoc}
     * @return
     */
	@Override
	public Optional<FieldValueBuilder> fieldValue(final Object value){
		if (value instanceof String) {
			return Optional.of(CustomFieldType.builder().value((String) value));
		}
		return Optional.empty();
	}

	/**
	 * Enumerates the possible render modes for a Custom Field. The represent the way dotCMS can
	 * render the code in it. For instance, {@link RenderMode#IFRAME} means the code will be parsed
	 * via the Velocity Utility class we already use by default. And {@link RenderMode#COMPONENT}
	 * means that we let the Angular code decide how to render it.
	 */
	public enum RenderMode {

		IFRAME,
		COMPONENT

	}

}

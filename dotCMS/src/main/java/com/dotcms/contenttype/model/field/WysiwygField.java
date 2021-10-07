package com.dotcms.contenttype.model.field;

import static com.dotcms.util.CollectionsUtils.list;

import com.dotcms.content.model.FieldValue;
import com.dotcms.content.model.type.ListType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableWysiwygField.class)
@JsonDeserialize(as = ImmutableWysiwygField.class)
@Value.Immutable
public abstract class WysiwygField extends Field {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@Override
	public Class type() {
		return WysiwygField.class;
	}

	@JsonIgnore
	@Value.Derived
	@Override
	public List<DataTypes> acceptedDataTypes() {
		return ImmutableList.of(DataTypes.LONG_TEXT);
	}

	@Value.Default
	@Override
	public DataTypes dataType() {
		return DataTypes.LONG_TEXT;
	};

	public abstract static class Builder implements FieldBuilder {
	}

	@JsonIgnore
	public Collection<ContentTypeFieldProperties> getFieldContentTypeProperties(){
		return list(ContentTypeFieldProperties.NAME, ContentTypeFieldProperties.REQUIRED,
				ContentTypeFieldProperties.REGEX_CHECK, ContentTypeFieldProperties.DEFAULT_VALUE, ContentTypeFieldProperties.HINT,
				ContentTypeFieldProperties.SEARCHABLE, ContentTypeFieldProperties.INDEXED);
	}

	@JsonIgnore
	public String getContentTypeFieldLabelKey(){
		return "WYSIWYG";
	}


	@Override
	public Optional<FieldValue<?>> fieldValue(final Object value) {
		if (value instanceof String) {
            /*

            final String wysiwyg = (String) map.get(DISABLED_WYSIWYG);
        if( UtilMethods.isSet(wysiwyg) ) {
            final List<String> wysiwygFields = new ArrayList<String>();
            final StringTokenizer st = new StringTokenizer(wysiwyg,StringPool.COMMA);
            while( st.hasMoreTokens() ) wysiwygFields.add(st.nextToken().trim());
            contentlet.setDisabledWysiwyg(wysiwygFields);
        }

             */


			return Optional.of(ListType.of((List<?>) value));
		}
		return Optional.empty();
	}

}

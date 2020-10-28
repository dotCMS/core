package com.dotcms.contenttype.model.type;

import java.util.ArrayList;
import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableBinaryField;
import com.dotcms.contenttype.model.field.ImmutableCustomField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.contenttype.model.field.ImmutableTagField;
import com.dotcms.contenttype.model.field.ImmutableTextAreaField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonSerialize(as = ImmutablePersonaContentType.class)
@JsonDeserialize(as = ImmutablePersonaContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class PersonaContentType extends ContentType implements Expireable, EnterpriseType {



	private static final long serialVersionUID = 1L;
	public static final String PERSONA_HOST_FOLDER_FIELD_VAR = "hostFolder";
	public static final String PERSONA_NAME_FIELD_VAR = "name";
	public static final String PERSONA_KEY_TAG_FIELD_VAR = "keyTag";
	public static final String PERSONA_PHOTO_FIELD_VAR = "photo";
	public static final String PERSONA_OTHER_TAGS_FIELD_VAR = "tags";
	public static final String PERSONA_DESCRIPTION_FIELD_VAR = "description";

	@Override
	public  BaseContentType baseType() {
		return  BaseContentType.PERSONA;
	}

	
	
	public abstract static class Builder implements ContentTypeBuilder {}
	
	
	
	public  List<Field> requiredFields(){
		List<Field> fields = new ArrayList<Field>();

		
		fields.add(
			ImmutableHostFolderField.builder()
				.name("Site/Folder")
				.variable(PERSONA_HOST_FOLDER_FIELD_VAR)
				.dataType(DataTypes.SYSTEM)
				.required(true)
				.indexed(true)
				.fixed(true)
				.sortOrder(1)
				.build()
		);

		
		fields.add(
			ImmutableTextField.builder()
				.name("Name")
				.variable(PERSONA_NAME_FIELD_VAR)
				.dataType(DataTypes.TEXT)
				.required(true)
				.indexed(true)
				.listed(true)
				.sortOrder(2)
				.fixed(true)
				.searchable(true)
				.build()
		);

		fields.add(
			ImmutableCustomField.builder()
				.name("Key Tag")
				.variable(PERSONA_KEY_TAG_FIELD_VAR)
				.dataType(DataTypes.TEXT)
				.required(true)
				.indexed(true)
				.listed(true)
				.values("$velutil.mergeTemplate('/static/personas/keytag_custom_field.vtl')")
				.regexCheck("[a-zA-Z0-9]+")
				.sortOrder(3)
				.fixed(true)
				.searchable(true)
				.build()
		);
		
		fields.add(
			ImmutableBinaryField.builder()
				.name("Photo")
				.variable(PERSONA_PHOTO_FIELD_VAR)
				.sortOrder(4)
				.fixed(true)
				.build()
		);
		 
		fields.add(
			ImmutableTagField.builder()
				.name("Other Tags")
				.variable(PERSONA_OTHER_TAGS_FIELD_VAR)
				.dataType(DataTypes.SYSTEM)

				.sortOrder(5)
				.fixed(true)
				.indexed(true)
				.searchable(true)
				.build()
		);
		
		fields.add(
			ImmutableTextAreaField.builder()
				.name("Description")
				.variable(PERSONA_DESCRIPTION_FIELD_VAR)
				.dataType(DataTypes.LONG_TEXT)
				.sortOrder(6)
				.fixed(true)
				.indexed(true)
				.searchable(true)
				.build()
		);
	
		return ImmutableList.copyOf(fields);
	}
	

}

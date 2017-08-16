package com.dotcms.contenttype.model.type;

import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
@JsonSerialize(as = ImmutableFormContentType.class)
@JsonDeserialize(as = ImmutableFormContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class FormContentType extends ContentType implements Expireable{

	public abstract static class Builder implements ContentTypeBuilder {}

	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentType baseType() {
		return  BaseContentType.FORM;
	}
	
	@Value.Default
	public boolean multilingualable(){
		return false;
	}
	
	
	public  List<Field> requiredFields(){
		
		
		Field titleField = ImmutableConstantField.builder()
				.name("Form Title")
				.variable("formTitle")
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.searchable(true)
				.build();
		
		Field emailField = ImmutableConstantField.builder()
				.name("Form Email")
				.variable("formEmail")
				.sortOrder(2)
				.fixed(true)
				.readOnly(true)
				.searchable(true)
				.build();
		
		
		Field returnField = ImmutableConstantField.builder()
				.name("Form Return Page")
				.variable("formReturnPage")
				.sortOrder(3)
				.fixed(true)
				.readOnly(true)
				.searchable(true)
				.build();
		
		Field usageField = ImmutableHostFolderField.builder()
				.name("Form Host")
				.variable("formHost")
				.sortOrder(4)
				.fixed(true)
				.searchable(true)
				.build();
		

		return ImmutableList.of(titleField,emailField,returnField,usageField);
	}
}

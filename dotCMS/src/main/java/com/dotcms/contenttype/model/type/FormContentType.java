package com.dotcms.contenttype.model.type;

import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
@JsonSerialize(as = ImmutableFormContentType.class)
@JsonDeserialize(as = ImmutableFormContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class FormContentType extends ContentType implements Expireable{

	public abstract static class Builder implements ContentTypeBuilder {}

	private static final long serialVersionUID = 1L;

	public static final String FORM_HOST_FIELD_VAR = "formHost";
	public static final String FORM_TITLE_FIELD_VAR = "formTitle";
	public static final String FORM_EMAIL_FIELD_VAR = "formEmail";
	public static final String FORM_RETURN_PAGE_FIELD_VAR = "formReturnPage";
  public static final String FORM_SUCCESS_CALLBACK = "formSuccessCallback";

  
  
	@Override
	public  BaseContentType baseType() {
		return  BaseContentType.FORM;
	}
	
	@Value.Default
	public boolean multilingualable(){
		return false;
	}
	
	
	public  List<Field> requiredFields(){
		

		Field emailField = ImmutableConstantField.builder()
				.name("Form Email")
				.variable(FORM_EMAIL_FIELD_VAR)
				.sortOrder(2)
				.fixed(false)
				.readOnly(false)
				.searchable(true)
				.build();

		Field returnField = ImmutableConstantField.builder()
				.name("Success Callback")
				.variable(FORM_SUCCESS_CALLBACK)
				.sortOrder(3)
				.fixed(false)
				.readOnly(false)
				.searchable(true)
				.values("// contentlet is an object\n// e.g. contentlet.inode, contentlet.firstName\n\nwindow.location='/thank-you?id=' + contentlet.identifier")
				.build();
		
		Field usageField = ImmutableHostFolderField.builder()
				.name("Form Host")
				.variable(FORM_HOST_FIELD_VAR)
				.sortOrder(4)
				.fixed(false)
				.searchable(true)
				.build();
		

		return ImmutableList.of(emailField,returnField,usageField);
	}
}

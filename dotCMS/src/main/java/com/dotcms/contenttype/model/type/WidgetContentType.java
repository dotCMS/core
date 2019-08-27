package com.dotcms.contenttype.model.type;

import java.util.List;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableConstantField;
import com.dotcms.contenttype.model.field.ImmutableTextField;
import com.google.common.collect.ImmutableList;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
@JsonSerialize(as = ImmutableWidgetContentType.class)
@JsonDeserialize(as = ImmutableWidgetContentType.class)
@Gson.TypeAdapters
@Value.Immutable
public abstract class WidgetContentType extends ContentType implements Expireable{
	
	public static final String WIDGET_CODE_FIELD_NAME = "Widget Code";
	public static final String WIDGET_CODE_FIELD_VAR = "widgetCode";
	public static final String WIDGET_USAGE_FIELD_NAME = "Widget Usage";
	public static final String WIDGET_USAGE_FIELD_VAR = "widgetUsage";
	public static final String WIDGET_TITLE_FIELD_NAME = "Widget Title";
	public static final String WIDGET_TITLE_FIELD_VAR = "widgetTitle";
	public static final String WIDGET_PRE_EXECUTE_FIELD_NAME = "Widget Pre-Execute";
	public static final String WIDGET_PRE_EXECUTE_FIELD_VAR = "widgetPreexecute";
	public abstract static class Builder implements ContentTypeBuilder {}
	private static final long serialVersionUID = 1L;

	@Override
	public BaseContentType baseType() {
		return BaseContentType.WIDGET;
	}
	@Value.Default
	public boolean multilingualable(){
		return false;
	}
	
	
	public  List<Field> requiredFields(){
		Field titleField = ImmutableTextField.builder()
				.name(WIDGET_TITLE_FIELD_NAME)
				.dataType(DataTypes.TEXT)
				.variable(WIDGET_TITLE_FIELD_VAR)
				.required(true)
				.listed(true)
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.searchable(true)
				.build();
		
		Field preExecute = ImmutableConstantField.builder()
				.name(WIDGET_PRE_EXECUTE_FIELD_NAME)
				.variable(WIDGET_PRE_EXECUTE_FIELD_VAR)
				.sortOrder(4)
				.fixed(false)
				.readOnly(false)
				.searchable(true)
				.build();
		
		
		Field codeField = ImmutableConstantField.builder()
				.name(WIDGET_CODE_FIELD_NAME)
				.variable(WIDGET_CODE_FIELD_VAR)
				.sortOrder(3)
				.fixed(true)
				.readOnly(true)
				.searchable(true)
				.build();
		
		Field usageField = ImmutableConstantField.builder()
				.name(WIDGET_USAGE_FIELD_NAME)
				.variable(WIDGET_USAGE_FIELD_VAR)
				.sortOrder(2)
				.fixed(false)
				.readOnly(false)
				.searchable(true)
				.build();
		

		
		return ImmutableList.of(titleField,usageField,codeField,preExecute);
		
		
		
	}
}

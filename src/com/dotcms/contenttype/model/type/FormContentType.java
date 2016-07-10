package com.dotcms.contenttype.model.type;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.google.common.base.Preconditions;


@Value.Immutable
public abstract class FormContentType extends ContentType{

	public abstract static class Builder implements ContentTypeBuilder {}

	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentTypes baseType() {
		return  BaseContentTypes.FORM;
	}
	
	@Value.Default
	public boolean multilingualable(){
		return false;
	}
	
	@Value.Check
	protected void check() {
		Preconditions.checkArgument(pagedetail()==null,"Detail Page cannot be set for forms");
		Preconditions.checkArgument(urlMapPattern()==null,"urlmap cannot be set for forms");
		Preconditions.checkArgument(expireDateVar()==null,"expireDate cannot be set for forms");
		Preconditions.checkArgument(publishDateVar()==null,"expireDate cannot be set for forms");
	}
	
	
	
	public  List<Field> requiredFields(){
		

		
		Field titleField = ImmutableHiddenField.builder()
				.name("Form Title")
				.dataType(DataTypes.CONSTANT)
				.variable("formTitle")
				.indexed(true)
				.sortOrder(1)
				.fixed(true)
				.searchable(true)
				.build();
		
		Field emailField = ImmutableHiddenField.builder()
				.name("Form Email")
				.dataType(DataTypes.CONSTANT)
				.variable("formEmail")
				.sortOrder(2)
				.fixed(true)
				.readOnly(true)
				.searchable(true)
				.build();
		
		
		Field returnField = ImmutableHiddenField.builder()
				.name("Form Return Page")
				.dataType(DataTypes.CONSTANT)
				.variable("formReturnPage")
				.sortOrder(3)
				.fixed(true)
				.readOnly(true)
				.searchable(true)
				.build();
		
		Field usageField = ImmutableHostFolderField.builder()
				.name("Form Host")
				.dataType(DataTypes.SYSTEM)
				.variable("formHost")
				.sortOrder(4)
				.fixed(true)
				.readOnly(false)
				.searchable(true)
				.build();
		

		return ImmutableList.of(titleField,emailField,returnField,usageField);
	}
}

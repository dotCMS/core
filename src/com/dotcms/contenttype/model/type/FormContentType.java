package com.dotcms.contenttype.model.type;

import java.util.List;

import org.immutables.value.Value;

import com.dotcms.contenttype.model.field.DataTypes;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.HiddenField;
import com.dotcms.contenttype.model.field.ImmutableHiddenField;
import com.dotcms.contenttype.model.field.ImmutableHostFolderField;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;


@Value.Immutable
public abstract class FormContentType extends ContentType{



	private static final long serialVersionUID = 1L;

	@Override
	public  BaseContentTypes baseType() {
		return  BaseContentTypes.FORM;
	}
	
	@Value.Default
	public boolean multilingualable(){
		return false;
	}

	
	public  List<Field> requiredFields(){
		

		
		Field titleField = ImmutableHiddenField.builder()
				.name("Form Title")
				.dataType(DataTypes.CONSTANT)
				.variable("formTitle")
				.required(true)
				.listed(true)
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
				.dataType(DataTypes.TEXT)
				.variable("formHost")
				.sortOrder(4)
				.fixed(true)
				.readOnly(false)
				.searchable(true)
				.build();
		

		return ImmutableList.of(titleField,emailField,returnField,usageField);
	}
}

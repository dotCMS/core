package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.*;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotmarketing.exception.DotDataException;

public class FieldApiImpl implements FieldApi {

	private List<Class> baseFieldTypes = ImmutableList.of(BinaryField.class, ButtonField.class, CategoriesTabField.class,
			CategoryField.class, ConstantField.class, CheckboxField.class, CustomField.class, DateField.class, DateTimeField.class,
			FileField.class, HiddenField.class, HostFolderField.class, ImageField.class, KeyValueField.class, LineDividerField.class,
			MultiSelectField.class, PermissionTabField.class, RadioField.class, RelationshipsTabField.class, SelectField.class,
			TabDividerField.class, TagField.class, TextAreaField.class, TimeField.class, WysiwygField.class);

	FieldFactory fac = new FieldFactoryImpl();

	@Override
	public Field save(Field field) throws DotDataException {
		return fac.save(field);
	}

	@Override
	public List<Class> fieldTypes() {


		return baseFieldTypes;
	}

	@Override
	public void registerFieldType(FieldType type) {

	}

	@Override
	public void deRegisterFieldType(FieldType type) {

	}
}

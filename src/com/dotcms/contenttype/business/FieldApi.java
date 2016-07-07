package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldType;
import com.dotmarketing.exception.DotDataException;

public interface FieldApi {

	static FieldApi api = new FieldApiImpl();

	default FieldApi instance() {
		return api;
	}

	Field save(Field field) throws DotDataException;

	List<Class> fieldTypes();

	void registerFieldType(FieldType type);

	void deRegisterFieldType(FieldType type);

}

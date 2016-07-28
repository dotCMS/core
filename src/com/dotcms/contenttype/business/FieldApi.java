package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldType;
import com.dotcms.contenttype.model.type.ContentType;
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

	void delete(Field field) throws DotDataException;

	void deleteFieldsByContentType(ContentType type) throws DotDataException;

	List<Field> byContentType(ContentType type) throws DotDataException;

	Field byContentTypeAndVar(ContentType type, String fieldVar) throws DotDataException;

}

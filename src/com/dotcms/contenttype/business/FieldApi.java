package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;

public interface FieldApi {

	static FieldApi api = new FieldApiImpl();

	default FieldApi instance() {
		return api;
	}

	List<Class> fieldTypes();

	void registerFieldType(Field type);

	void deRegisterFieldType(Field type);

	void delete(Field field) throws DotDataException;

	void deleteFieldsByContentType(ContentType type) throws DotDataException;

	Field byContentTypeAndVar(ContentType type, String fieldVar) throws DotDataException;
	
	Field find(String id) throws DotDataException;
	
	List<Field> byContentTypeId(String typeId) throws DotDataException;

	Field save(Field field, User user) throws DotDataException, DotSecurityException;

}

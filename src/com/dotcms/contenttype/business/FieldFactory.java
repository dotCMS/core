package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotmarketing.exception.DotDataException;

public interface FieldFactory {


	Field byId(String id) throws DotDataException;

	List<Field> byContentTypeId(String id) throws DotDataException;

	List<Field> byContentTypeVar(String var) throws DotDataException;

	void delete(String id) throws DotDataException;

	Field save(Field field) throws DotDataException;





}

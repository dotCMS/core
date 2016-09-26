package com.dotcms.contenttype.business;

import java.util.List;
import java.util.Set;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
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

	static Set<String> RESERVED_FIELD_VARS= ImmutableSet.of(
			Contentlet.INODE_KEY,
			Contentlet.LANGUAGEID_KEY,
			Contentlet.STRUCTURE_INODE_KEY,
			Contentlet.LAST_REVIEW_KEY,
			Contentlet.NEXT_REVIEW_KEY,
			Contentlet.REVIEW_INTERNAL_KEY,
			Contentlet.DISABLED_WYSIWYG_KEY,
			Contentlet.LOCKED_KEY,
			Contentlet.ARCHIVED_KEY,
			Contentlet.LIVE_KEY,
			Contentlet.WORKING_KEY,
			Contentlet.MOD_DATE_KEY,
			Contentlet.MOD_USER_KEY,
			Contentlet.OWNER_KEY,
			Contentlet.IDENTIFIER_KEY,
			Contentlet.SORT_ORDER_KEY,
			Contentlet.HOST_KEY,
			Contentlet.FOLDER_KEY);


	
}

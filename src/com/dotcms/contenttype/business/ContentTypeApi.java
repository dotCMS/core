package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.liferay.portal.model.User;


public interface ContentTypeApi {

	void delete(ContentType st, User user) throws DotSecurityException, DotDataException;

	ContentType find(String inode, User user) throws DotSecurityException, DotDataException;

	List<ContentType> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	List<ContentType> find(User user, boolean respectFrontendRoles, String condition, String orderBy,
			int limit, int offset, String direction) throws DotDataException, DotSecurityException;

	ContentType findByVarName(String varName, User user) throws DotSecurityException, DotDataException;

	int countContentType(String condition) throws DotDataException;

	void saveContentType(ContentType type, List<Field> fields, User user) throws DotDataException, DotSecurityException;




}

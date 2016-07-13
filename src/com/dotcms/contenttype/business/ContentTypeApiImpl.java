package com.dotcms.contenttype.business;

import java.util.List;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.google.common.base.Preconditions;
import com.liferay.portal.model.User;

public class ContentTypeApiImpl implements ContentTypeApi {

	
	ContentTypeFactory fac = FactoryLocator.getContentTypeFactory2();
	FieldFactory ffac = FactoryLocator.getFieldFactory2();
	@Override
	public void delete(ContentType st, User user) throws DotSecurityException, DotDataException {
		
	}

	@Override
	public ContentType find(String inode, User user) throws DotSecurityException, DotDataException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ContentType> findAll(User user, boolean respectFrontendRoles) throws DotSecurityException,DotDataException {
		// TODO Auto-generated method stub
		
		return APILocator.getPermissionAPI().filterCollection(this.fac.findAll(), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		

	}

	@Override
	public List<ContentType> find(User user, boolean respectFrontendRoles, String condition, String orderBy, int limit, int offset,
			String direction) throws DotDataException, DotSecurityException {
		return APILocator.getPermissionAPI().filterCollection(this.fac.search(condition, orderBy, offset,limit), PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		

	}

	@Override
	public ContentType findByVarName(String varName, User user) throws DotSecurityException, DotDataException {
		
		ContentType type = this.fac.findByVar(varName);
		if(APILocator.getPermissionAPI().doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user)){
			return type;
		}
		throw new DotSecurityException("User " + user + " does not have READ permissions on ContentType " + type);
	}

	@Override
	public int countContentType(String condition) throws DotDataException {
		return this.fac.searchCount(condition);
	}
	@Override
	public void saveContentType(ContentType type, List<Field> fields, User user) throws DotDataException, DotSecurityException {
		if(!APILocator.getPermissionAPI().doesUserHavePermission(type, PermissionAPI.PERMISSION_EDIT, user)){
			throw new DotSecurityException("User " + user + " does not have READ permissions on ContentType " + type);
		}
		Preconditions.checkNotNull(fields);
		
		type =	this.fac.save(type);
		int i=0;
		for(Field field : fields){
			field = FieldBuilder.builder(field).contentTypeId(type.inode()).sortOrder(i++).build();
			ffac.save(field);
		}
		
		
	}
}

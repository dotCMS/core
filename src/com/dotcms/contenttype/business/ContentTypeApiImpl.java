package com.dotcms.contenttype.business;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.python.modules.newmodule;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.google.common.base.Preconditions;
import com.liferay.portal.model.User;

public class ContentTypeApiImpl implements ContentTypeApi {

	ContentTypeFactory fac = FactoryLocator.getContentTypeFactory2();
	FieldFactory ffac = FactoryLocator.getFieldFactory2();

	@Override
	public void delete(ContentType type, User user) throws DotSecurityException, DotDataException {
		if (!APILocator.getPermissionAPI().doesUserHavePermission(type, PermissionAPI.PERMISSION_WRITE, user)) {
			throw new DotSecurityException("User " + user + " does not have WRITE permissions on ContentType " + type);
		}
		// checking if there is containers using this structure
		List<Container> containers = APILocator.getContainerAPI().findContainersForStructure(type.inode());

		StringBuilder names = new StringBuilder("Structure" + type.name()
				+ " can't be deleted because the following containers are using it: ");
		for (Container c : containers) {
			String hostTitle = APILocator.getHostAPI().findParentHost(c, user, false).getTitle();
			names.append(hostTitle + " : " + c.getTitle() + "</br>");
		}
		if (containers.size() > 0) {
			throw new DotStateException("Structure " + type.name() + " can't be deleted because the following containers are using it: "
					+ names);
		}

		// permission check delete related contentlets
		int limit = 200;
		int offset = 0;
		ContentletAPI conAPI = APILocator.getContentletAPI();
		List<ContentletSearch> contentlets = null;
		do {
			contentlets = conAPI.searchIndex("+contenttype:" + type.inode(), limit, offset, "mod_date", user, false);
			for (ContentletSearch contentlet : contentlets) {
				PermissionableProxy proxy = new PermissionableProxy();
				proxy.setIdentifier(contentlet.getIdentifier());
				proxy.setInode(contentlet.getInode());
				proxy.setOwner(null);
				proxy.setType(new Contentlet().getType());
				if (!APILocator.getPermissionAPI().doesUserHavePermission(proxy, PermissionAPI.PERMISSION_PUBLISH, user)) {
					throw new DotSecurityException("Cannot delete structure. " + user + " does not have DELETE permissions on Content id:"
							+ proxy.getIdentifier());
				}
			}

		} while (contentlets.size() > 0);

		fac.delete(type);
	}

	@Override
	public ContentType find(String inode, User user) throws DotSecurityException, DotDataException {
		ContentType type = this.fac.find(inode);
		if (APILocator.getPermissionAPI().doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user)) {
			return type;
		}
		throw new DotSecurityException("User " + user + " does not have READ permissions on ContentType " + type);
	}

	@Override
	public List<ContentType> findAll(User user, boolean respectFrontendRoles) throws DotDataException {
		// TODO Auto-generated method stub

		try {
			return APILocator.getPermissionAPI().filterCollection(this.fac.findAll(), PermissionAPI.PERMISSION_READ, respectFrontendRoles,
					user);
		} catch (DotSecurityException e) {
			Logger.warn(this.getClass(), e.getMessage(), e);
			return ImmutableList.of();
		}

	}

	@Override
	public List<ContentType> find(User user, boolean respectFrontendRoles, String condition, String orderBy, int limit, int offset,
			String direction) throws DotDataException {
		try {
			return APILocator.getPermissionAPI().filterCollection(this.fac.search(condition, orderBy, offset, limit),
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		} catch (DotSecurityException e) {
			return ImmutableList.of();
		}

	}

	@Override
	public ContentType findByVarName(String varName, User user) throws DotSecurityException, DotDataException {

		ContentType type = this.fac.findByVar(varName);
		if (APILocator.getPermissionAPI().doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user)) {
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
		if (!APILocator.getPermissionAPI().doesUserHavePermission(type, PermissionAPI.PERMISSION_PUBLISH, user)) {
			throw new DotSecurityException("User " + user + " does not have READ permissions on ContentType " + type);
		}
		Preconditions.checkNotNull(fields);

		type = this.fac.save(type);
		int i = 0;
		for (Field field : fields) {
			field = FieldBuilder.builder(field).contentTypeId(type.inode()).sortOrder(i++).build();
			ffac.save(field);
		}

	}
}

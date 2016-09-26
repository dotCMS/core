package com.dotcms.contenttype.business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.dotcms.contenttype.exception.NotFoundInDbException;
import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.field.FieldBuilder;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotcms.contenttype.model.type.ContentTypeBuilder;
import com.dotcms.contenttype.model.type.FormContentType;
import com.dotcms.contenttype.transform.contenttype.StructureTransformer;
import com.dotcms.repackage.com.google.common.collect.ImmutableList;
import com.dotcms.repackage.org.python.modules.newmodule;
import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.PermissionableProxy;
import com.dotmarketing.business.APILocator;
import com.dotmarketing.business.CacheLocator;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.business.FactoryLocator;
import com.dotmarketing.business.PermissionAPI;
import com.dotmarketing.business.PermissionLevel;
import com.dotmarketing.business.Permissionable;
import com.dotmarketing.common.model.ContentletSearch;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.contentlet.business.ContentletAPI;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.business.FolderAPI;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.action.EditStructureAction;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotmarketing.quartz.job.IdentifierDateJob;
import com.dotmarketing.util.ActivityLogger;
import com.dotmarketing.util.AdminLogger;
import com.dotmarketing.util.HostUtil;
import com.dotmarketing.util.Logger;
import com.dotmarketing.util.UtilMethods;
import com.dotcms.repackage.com.google.common.base.Preconditions;
import com.liferay.portal.language.LanguageUtil;
import com.liferay.portal.model.User;

public class ContentTypeApiImpl implements ContentTypeApi {

	ContentTypeFactory fac = FactoryLocator.getContentTypeFactory2();
	FieldFactory ffac = FactoryLocator.getFieldFactory2();
	PermissionAPI perms = APILocator.getPermissionAPI();
	@Override
	public void delete(ContentType type, User user) throws DotSecurityException, DotDataException {
		perms.checkPermission(type, PermissionLevel.PUBLISH, user);
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
				perms.checkPermission(proxy, PermissionLevel.PUBLISH, user);
			}

		} while (contentlets.size() > 0);

		fac.delete(type);
	}

	@Override
	public ContentType find(String inode, User user) throws DotSecurityException, DotDataException {
		ContentType type = this.fac.find(inode);
		if (perms.doesUserHavePermission(type, PermissionAPI.PERMISSION_READ, user)) {
			return type;
		}
		throw new DotSecurityException("User " + user + " does not have READ permissions on ContentType " + type);
	}

	@Override
	public List<ContentType> findAll(User user, boolean respectFrontendRoles) throws DotDataException {
		// TODO Auto-generated method stub

		try {
			return perms.filterCollection(this.fac.findAll(), PermissionAPI.PERMISSION_READ, respectFrontendRoles,
					user);
		} catch (DotSecurityException e) {
			Logger.warn(this.getClass(), e.getMessage(), e);
			return ImmutableList.of();
		}

	}
	
	@Override
	public List<ContentType> find(String condition,User user, boolean respectFrontendRoles) throws DotDataException {
		try {
			return perms.filterCollection(this.fac.search(condition, "mod_date", 0, 10000),
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		} catch (DotSecurityException e) {
			return ImmutableList.of();
		}

	}
	
	@Override
	public List<ContentType> find(String condition, String orderBy, int limit, int offset,
			String direction,User user, boolean respectFrontendRoles) throws DotDataException {
		try {
			return perms.filterCollection(this.fac.search(condition, orderBy, offset, limit),
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		} catch (DotSecurityException e) {
			return ImmutableList.of();
		}

	}

	@Override
	public ContentType findByVarName(String varName, User user) throws DotSecurityException, DotDataException {

		ContentType type = this.fac.findByVar(varName);
		perms.checkPermission(type, PermissionLevel.READ, user);
		return type;

	}

	@Override
	public int count(String condition) throws DotDataException {
		return this.fac.searchCount(condition);
	}
	
	@Override
	public int count(String condition, User user) throws DotDataException {
		return find(condition, user, false).size();
	}
	@Override
	public ContentType save(ContentType type, List<Field> fields, User user) throws DotDataException, DotSecurityException {

		Preconditions.checkNotNull(fields);

		type = save(type, user);
		int i = 0;
		for (Field field : fields) {
			field = FieldBuilder.builder(field).contentTypeId(type.inode()).sortOrder(i++).build();
			ffac.save(field);
		}
		return type;
	}
	@Override
	public ContentType save(ContentType type, User user) throws DotDataException, DotSecurityException {


		Permissionable parent =  type.getParentPermissionable();


		if (!perms.doesUserHavePermissions(parent, "PARENT:" + PermissionAPI.PERMISSION_CAN_ADD_CHILDREN + ", STRUCTURES:" + PermissionAPI.PERMISSION_PUBLISH, user)) {
			throw new DotSecurityException("User-does-not-have-add-children-or-structure-permission-on-host-folder:" + parent);
		}
		
		// set to system folder if on system host or the host id of the folder it is on
		if (!UtilMethods.isSet(type.host()) || type.host().equals(Host.SYSTEM_HOST)) {
			type = ContentTypeBuilder.builder(type).host(Host.SYSTEM_HOST).build();
			type = ContentTypeBuilder.builder(type).folder(Folder.SYSTEM_FOLDER).build();
		}
		if (UtilMethods.isSet(type.folder()) && !type.folder().equals(Folder.SYSTEM_FOLDER)) {
			type = ContentTypeBuilder.builder(type).host(APILocator.getFolderAPI().find(type.folder(), user, false).getHostId()).build();
		}else {
			type = ContentTypeBuilder.builder(type).folder(Folder.SYSTEM_FOLDER).build();
		}
		
		ContentType oldType = type;
		try{
			if(type.inode()!=null)
				oldType = this.fac.find(type.inode());
		}
		catch(NotFoundInDbException notThere){
			// not logging, expected when inserting new from separate environment
		}
		type = this.fac.save(type);
		
		if(oldType!=null ){
			if(fireUpdateIdentifiers(oldType.expireDateVar(), type.expireDateVar())){
				IdentifierDateJob.triggerJobImmediately(oldType, user);	
			}
			else if(fireUpdateIdentifiers(oldType.publishDateVar(), type.publishDateVar())){
				IdentifierDateJob.triggerJobImmediately(oldType, user);	
			}
			perms.resetPermissionReferences(type);
		}
		ActivityLogger.logInfo(getClass(), "Save ContentType Action", "User " +user.getUserId() + "/" + user.getFullName() + " added ContentType "
				+ type.name() + " to host id:" + type.host());
		AdminLogger.log(getClass(), "ContentType", "ContentType saved : " + type.name(), user);
		return type;

	}
	@Override
	public synchronized String suggestVelocityVar(final String tryVar) throws DotDataException{
		if(!UtilMethods.isSet(tryVar)){
			return UUID.randomUUID().toString();
		}else{
			return this.fac.suggestVelocityVar(tryVar);
		}
	}
	@Override
	public ContentType setAsDefault(ContentType type, User user) throws DotDataException, DotSecurityException{
		perms.checkPermission(type, PermissionLevel.READ, user);
		return fac.setAsDefault(type);

	}
	
	@Override
	public ContentType findDefault(User user) throws DotDataException, DotSecurityException{
		ContentType type = fac.findDefaultType();
		perms.checkPermission(type, PermissionLevel.READ, user);
		return type;

	}
	
	@Override
	public List<ContentType> findByBaseType(BaseContentType type, String orderBy, int limit, int offset, User user, boolean respectFrontendRoles) throws DotDataException {
		try {
			return perms.filterCollection(this.fac.search("1=1", type, orderBy, offset, limit),
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		} catch (DotSecurityException e) {
			return ImmutableList.of();
		}

	}

	@Override
	public List<ContentType> findByType(BaseContentType type, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException {

		try {
			return perms.filterCollection(this.fac.findByBaseType(type),
					PermissionAPI.PERMISSION_READ, respectFrontendRoles, user);
		} catch (DotSecurityException e) {
			return ImmutableList.of();
		}
	}
	
	@Override
	public List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException{
		List<SimpleStructureURLMap> res = new ArrayList<SimpleStructureURLMap>();
		
		for(ContentType type : fac.findUrlMapped()){
			res.add(new SimpleStructureURLMap(type.inode(), type.urlMapPattern()));
		}		
		return ImmutableList.copyOf(res);
	}
	
	@Override
	public void moveToSystemFolder(Folder folder) throws DotDataException{

		List<ContentType> types = APILocator.getContentTypeAPI2().find("folder='" + folder.getIdentifier() + "'", "mod_date", 10000, 0, "asc", APILocator.systemUser(), false);

		for(ContentType  type : types){
			ContentTypeBuilder builder = ContentTypeBuilder.builder(type);
			builder.host(folder.getHostId());
			builder.folder(Folder.SYSTEM_FOLDER);
			


			type=	fac.save(builder.build());
			CacheLocator.getContentTypeCache2().remove(type);
			perms.resetPermissionReferences(type);
		}
	}
	
	private boolean fireUpdateIdentifiers(String oldVal, String newVal){
		if(oldVal !=null || newVal!=null){
			if(oldVal!=null){
				if(!oldVal.equals(newVal)){
					return true;
				}
			}
			if(newVal!=null){
				if(!newVal.equals(oldVal)){
					return true;
				}
			}
		}
		return false;
	}

	
}

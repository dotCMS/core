package com.dotcms.contenttype.business;

import java.util.List;
import java.util.Set;

import com.dotcms.contenttype.model.field.Field;
import com.dotcms.contenttype.model.type.BaseContentType;
import com.dotcms.contenttype.model.type.ContentType;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.structure.model.SimpleStructureURLMap;
import com.dotcms.repackage.com.google.common.collect.ImmutableSet;
import com.liferay.portal.model.User;


public interface ContentTypeApi {

	void delete(ContentType st, User user) throws DotSecurityException, DotDataException;

	ContentType find(String inode, User user) throws DotSecurityException, DotDataException;

	List<ContentType> findAll(User user, boolean respectFrontendRoles) throws DotDataException;

	List<ContentType> find(String condition, String orderBy,
			int limit, int offset, String direction,User user, boolean respectFrontendRoles) throws DotDataException;

	ContentType findByVarName(String varName, User user) throws DotSecurityException, DotDataException;

	// based on a condition
	int count(String condition) throws DotDataException;
	
	// based on a condition and a user
	int count(String condition,User user) throws DotDataException;
	
	// all
    int count() throws DotDataException;
	String suggestVelocityVar(String tryVar) throws DotDataException;

	ContentType setAsDefault(ContentType type, User user) throws DotDataException, DotSecurityException;

	ContentType findDefault(User user) throws DotDataException, DotSecurityException;


	List<ContentType> findByBaseType(BaseContentType type, String orderBy, int limit, int offset, User user, boolean respectFrontendRoles)
			throws DotDataException;

	List<ContentType> findByType(BaseContentType type, User user, boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException;

	List<SimpleStructureURLMap> findStructureURLMapPatterns() throws DotDataException;

	void moveToSystemFolder(Folder folder) throws DotDataException;

	ContentType save(ContentType type, List<Field> fields, User user) throws DotDataException, DotSecurityException;

	ContentType save(ContentType type, User user) throws DotDataException, DotSecurityException;

	List<ContentType> find(String condition, User user, boolean respectFrontendRoles) throws DotDataException;

	
	List<ContentType> recentlyUsed(BaseContentType type, User user, int numberToShow) throws DotDataException;
	    
	
	
	Set<String> reservedStructureNames = ImmutableSet.of("host", "folder", "file","forms", "html page", "menu link", "virtual link", "container", "template", "user" );

    Set<String> reservedStructureVars = ImmutableSet.of("host", "folder", "file", "forms","htmlpage", "menulink", "virtuallink", "container", "template", "user", "calendarEvent" );




	
	

}

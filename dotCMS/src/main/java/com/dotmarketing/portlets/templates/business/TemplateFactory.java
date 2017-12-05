package com.dotmarketing.portlets.templates.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpageasset.model.HTMLPageAsset;
import com.dotmarketing.portlets.htmlpageasset.model.IHTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public interface TemplateFactory {

	List<Template> findTemplatesAssignedTo(Host parentHost, final boolean includeArchived) throws DotDataException;
		
	List<Template> findTemplatesUserCanUse(User user, String hostName, String query,boolean searchHost, int offset, int limit) throws DotDataException, DotSecurityException ;

	void delete(Template template) throws DotDataException;
	
	/**
	 * Save template into a persistent repository.
	 * 
	 * @param template
	 * @throws DotDataException
	 */
	void save(Template template) throws DotDataException;
	void save(Template template, String existingId)throws DotDataException;
	
	/**
	 * Delete template from cache.
	 * 
	 * @param template
	 * @throws DotDataException
	 */
	void deleteFromCache(Template template) throws DotDataException;

	Template findWorkingTemplateByName(String name, Host host) throws DotDataException;
	
	List<Template> findTemplates(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;
	
	void associateContainers(List<Container> containerIdentifiers,Template template) throws DotHibernateException;

	Template find(String inode) throws DotStateException, DotDataException;
	

	List<Container> getContainersInTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	Template copyTemplate(Template currentTemplate, Host host) throws DotDataException, DotSecurityException;
	
	/**
	 *
	 * Updates the template's theme without creating new version.
	 * @param templateInode
	 * @param theme
	 *
	 */
	void updateThemeWithoutVersioning(String templateInode, String theme) throws DotDataException;

   /**
	 * Method will replace user references of the given userId in templates
	 * with the replacement user Id  
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	 void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException;
}

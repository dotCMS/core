package com.dotmarketing.portlets.templates.business;

import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

public interface TemplateFactory {
	
	List<Template> findTemplatesUnder(Folder parentFolder) throws DotDataException;

	public List<Template> findTemplatesAssignedTo(Host parentHost, boolean includeArchived) throws DotDataException;
		
	public List<Template> findTemplatesUserCanUse(User user, String hostName, String query,boolean searchHost, int offset, int limit) throws DotDataException, DotSecurityException ;

	void delete(Template template) throws DotDataException;
	
	/**
	 * Save template into a persistent repository.
	 * 
	 * @param template
	 * @throws DotDataException
	 */
	public void save(Template template) throws DotDataException;
	
	/**
	 * Delete template from cache.
	 * 
	 * @param template
	 * @throws DotDataException
	 */
	public void deleteFromCache(Template template) throws DotDataException;

	public Template findWorkingTemplateByName(String name, Host host) throws DotDataException;
	
	public List<Template> findTemplates(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;
	
	public List<HTMLPage> getPagesUsingTemplate(Template template) throws DotDataException;
	
	public void associateContainers(List<Container> containerIdentifiers,Template template) throws DotHibernateException;
	
}

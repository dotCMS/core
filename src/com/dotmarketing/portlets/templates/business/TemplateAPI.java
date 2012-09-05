package com.dotmarketing.portlets.templates.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.htmlpages.business.HTMLPageAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.htmlpages.model.HTMLPage;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.PortalException;
import com.liferay.portal.SystemException;
import com.liferay.portal.model.User;

public interface TemplateAPI {

	List<Template> findTemplatesUnder(Folder parentFolder) throws DotDataException;

	/**
	 *
	 * Retrieves all non-archived templates assigned to the given host
	 *
	 */
	List<Template> findTemplatesAssignedTo(Host parentHost) throws DotDataException;

	/**
	 *
	 * Retrieves all templates assigned to the given host
	 * @param includeArchived if true it also retrieves all archived templates
	 *
	 */
	List<Template> findTemplatesAssignedTo(Host parentHost, boolean includeArchived) throws DotDataException;

	/**
	 * Retrieves a paginated list of templates the given user can read. Return working templates.  This method uses DB
	 *
	 * @param user
	 * @param hostName
	 * @param query
	 * @param searchHost
	 * @param hostId
	 * @param offset
	 * @param limit
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Template> findTemplatesUserCanUse(User user, String hostName,  String query, boolean searchHost, int offset, int limit) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the working version of a template given its identifier
	 * @param id
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template findWorkingTemplate(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	/**
	 * Retrieves the live version of a template given its identifier
	 * @param id
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template findLiveTemplate(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Copies a template to another host.
	 *
	 * @param template
	 * @param destination
	 * @param forceOverwrite
	 * @param copySourceContainers
	 * @param user
	 * @param respectFrontendRoles
	 * @return Template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template copy(Template template, Host destination, boolean forceOverwrite, boolean copySourceContainers, User user, boolean respectFrontendRoles)
		throws DotDataException, DotSecurityException;

	/**
	 *
	 * Copies a template to another host. This method does not copy containers inside instead it remaps the containers based on the containerMappings given.
	 *
	 * @param sourceTemplate
	 * @param destinationHost
	 * @param forceOverwrite
	 * @param containerMappings
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template copy(Template sourceTemplate, Host destinationHost, boolean forceOverwrite, List<ContainerRemapTuple> containerMappings, User user,
			boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Return the list of container identifiers used in a template body.
	 *
	 * @param body
	 * @return List<Identifier>
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	List<Container> getContainersInTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Return the list of pages that use a  template body.
	 *
	 * @param body
	 * @return List<Identifier>
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
	List<HTMLPage> getPagesUsingTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Update template body code to use the new parse container syntax
	 *
	 * @param template
	 */
	void updateParseContainerSyntax(Template template);

	/**
	 * Retrieves the template associated to a host
	 * @param template
	 * @return
	 * @throws DotDataException
	 */
	Host getTemplateHost(Template template) throws DotDataException;

	/**
	 * Save a template
	 *
	 * @param template
	 * @param destination
	 * @param user
	 * @param respectFrontendRoles
	 * @return Template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Template saveTemplate(Template template, Host destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Delete the specified template
	 *
	 * @param template
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	public boolean delete(Template template, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception;

    /**
     * Retrieves a paginated list of templates the user can use
     * @param user
     * @param includeArchived
     * @param params
     * @param hostId
     * @param inode
     * @param identifier
     * @param parent
     * @param offset
     * @param limit
     * @param orderBy
     * @return
     * @throws DotSecurityException
     * @throws DotDataException
     */
	public List<Template> findTemplates(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;

	public void associateContainers( List<Container> containerIdentifiers,Template template) throws DotHibernateException;

	public String checkDependencies(String templateInode, User user, Boolean respectFrontendRoles) throws PortalException, SystemException, DotDataException, DotSecurityException;

    public int deleteOldVersions(Date assetsOlderThan) throws DotStateException, DotDataException;

}

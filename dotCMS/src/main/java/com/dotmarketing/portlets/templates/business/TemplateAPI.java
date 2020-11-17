package com.dotmarketing.portlets.templates.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.Identifier;
import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.containers.model.Container;
import com.dotmarketing.portlets.htmlpageasset.business.HTMLPageAssetAPI.TemplateContainersReMap.ContainerRemapTuple;
import com.dotmarketing.portlets.templates.design.bean.ContainerUUID;
import com.dotmarketing.portlets.templates.design.bean.TemplateLayout;
import com.dotmarketing.portlets.templates.model.Template;
import com.liferay.portal.model.User;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Api to interact with Templates
 * You can create and edit templates, publish/unpubish, archive/unarchive, delete, etc.
 */
public interface TemplateAPI {

	/**
	 * Retrieves all non-archived templates assigned to the given host. It uses DB directly.
	 * @param parentHost host where the template lives.
	 * @return list of templates that lives in the host
	 * @throws DotDataException
	 */
	List<Template> findTemplatesAssignedTo(final Host parentHost) throws DotDataException;

	/**
	 * Retrieves all templates assigned to the given host. It uses DB directly.
	 * @param parentHost host where the template lives.
	 * @param includeArchived boolean if true archived templates will be included
	 * @return list of templates that lives in the host
	 * @throws DotDataException
	 */
	List<Template> findTemplatesAssignedTo(final Host parentHost, final boolean includeArchived) throws DotDataException;

	/**
	 * Retrieves a paginated list of templates the given user can read. Return working templates and non-archived. Order them by title
	 * This method uses DB directly
	 * @param user user to make the search
	 * @param hostId host id where the templates lives
	 * @param query
	 * @param searchHost is not used
	 * @param offset
	 * @param limit
	 * @return list of templates
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Template> findTemplatesUserCanUse(final User user, final String hostId, final String query, final boolean searchHost, final int offset, final int limit) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the working version of a template given its identifier
	 * @param id identifier of the template
	 * @param user user to make the search
	 * @param respectFrontendRoles
	 * @return working template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template findWorkingTemplate(final String id, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	/**
	 * Retrieves the live version of a template given its identifier
	 * @param id identifier of the template
	 * @param user user to make the search
	 * @param respectFrontendRoles
	 * @return live template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template findLiveTemplate(final String id, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Copies a template to another host.
	 *
	 * @param template template to copy
	 * @param destination where to copy
	 * @param forceOverwrite
	 * @param copySourceContainers
	 * @param user user to make the request
	 * @param respectFrontendRoles
	 * @return copy of the template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template copy(final Template template, final Host destination, final boolean forceOverwrite, final boolean copySourceContainers, final User user, final boolean respectFrontendRoles)
		throws DotDataException, DotSecurityException;

	/**
	 *
	 * Copies a template to another host. This method does not copy containers inside instead it remaps the containers based on the containerMappings given.
	 *
	 * @param sourceTemplate template to copy
	 * @param destinationHost where to copy
	 * @param forceOverwrite
	 * @param containerMappings
	 * @param user user to make the request
	 * @param respectFrontendRoles
	 * @return copied template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	Template copy(final Template sourceTemplate, final Host destinationHost, final boolean forceOverwrite, final List<ContainerRemapTuple> containerMappings, final User user,
			final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Return the list of container identifiers used in a template body.
	 *
	 * @param template
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Container>
	 *
	 * @throws DotSecurityException
	 * @throws DotDataException
	 *
	 */
	List<Container> getContainersInTemplate(Template template, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	List<ContainerUUID> getContainersUUID(TemplateLayout layout);

	/**
	 * Gets the {@link ContainerUUID} from a draw template body
	 * @param drawTemplateBody {@link String}
	 * @return List of ContainerUUID, empty if nothing
	 */
	List<ContainerUUID> getContainersUUIDFromDrawTemplateBody(String drawTemplateBody);

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
	Template saveTemplate(Template template, Host destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Publish a template if has the appropiate permissions
	 * @param template {@link Template} to publish (valid template)
	 * @param user     {@link User} user to check the permissions
	 * @param respectFrontendRoles {@link Boolean}
	 * @return boolean true if publish
	 */
	boolean publishTemplate(Template template, User user, boolean respectFrontendRoles);

	/**
	 * Unpublish a template if has the appropiate permissions
	 * @param template {@link Template} to unpublish (valid template)
	 * @param user     {@link User} user to check the permissions
	 * @param respectFrontendRoles {@link Boolean}
	 * @return boolean true if publish
	 */
	boolean unpublishTemplate(Template template, User user, boolean respectFrontendRoles);

	/**
	 * Unlock the template
	 * @param template {@link Template}
	 * @param user     {@link User}
	 */
	void unlock (Template template, User user);

	/**
	 * Archive the template, it should be unpublish, but if it is not, then will be unpublish and consequently archive.
	 * @param template {@link Template}
	 * @param user     {@link User}
	 * @param respectFrontendRoles
	 * @return boolean true if success
	 */
	boolean archive (Template template, User user, boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException;

	/**
	 * If the template is archive will unarchive it
	 * @param template {@link Template}
	 * @param user     {@link User}
	 */
	boolean unarchive (Template template, User user) throws DotDataException, DotSecurityException;

	/**
	 * Delete the specified template.
	 *
	 * Template should be archive, otherwise DotStateException
	 * Should have write permission, otherwise SecurityException
	 *
	 * @param template {@link Template}
	 * @param user     {@link User}
	 * @param respectFrontendRoles {@link Boolean}
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	boolean deleteTemplate(Template template, User user, boolean respectFrontendRoles);

	/**
	 * Deletes the template by inode
	 * @param inode String
	 */
	void deleteByInode(String inode);

	/**
	 * Delete the specified template
	 *
	 * @deprecated uses {@link #deleteTemplate(Template, User, boolean)}
	 * @param template
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	@Deprecated
	boolean delete(Template template, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception;

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
	List<Template> findTemplates(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;


	/**
	 * Check if there are Contentlet Pages using this Template
	 * @param templateInode
	 * @param user
	 * @param respectFrontendRoles
	 * @return List of Contentlet Pages (page's titles) using the specified Template
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	String checkDependencies(String templateInode, User user, Boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Check if there are Contentlet Pages using this Template
	 * @param template {@link Template}
	 * @param user     {@link User}
	 * @param respectFrontendRoles Boolean
	 * @return Map, String Name -> Host Name : Page Name
	 */
	Map<String, String> checkPageDependencies(Template template, User user, boolean respectFrontendRoles);

	int deleteOldVersions(Date assetsOlderThan) throws DotStateException, DotDataException;

	/**
	 * Finds a template by its inode.
	 * @param inode inode of the template
	 * @param user user to make the search
	 * @param respectFrontEndRoles
	 * @return Template, if not exists null
	 * @throws DotSecurityException
	 * @throws DotDataException
	 */
    Template find(final String inode, final User user, final boolean respectFrontEndRoles) throws DotSecurityException, DotDataException;

	/**
	 * Makes a copy of sourceTemplate and returns the new one
	 * @param sourceTemplate
	 * @param user
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
    Template copy(Template sourceTemplate, User user)throws DotDataException, DotSecurityException ;
    
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

	/**
	 * Brings all the versions of a specific template
	 * @param identifier id of the template
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Template> findAllVersions(final Identifier identifier, final User user, final boolean respectFrontendRoles)
			throws DotDataException, DotSecurityException;

	/**
	 * Brings the versions of a specific template, if bringOldVersions is true brings all the versions,
	 * if is set to false, only brings the working and the live version
	 * @param identifier id of the template
	 * @param user
	 * @param respectFrontendRoles
	 * @param bringOldVersions true = all versions, false = only live and working
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	List<Template> findAllVersions(final Identifier identifier, final User user, final boolean respectFrontendRoles, final boolean bringOldVersions)
			throws DotDataException, DotSecurityException;

	/**
	 * Finds the templates where the containerInode is set as a parent in the tree table.
	 * Was created to recreate InodeFactory.getChildrenClass(Inode p, Class c) since it uses Hibernate
	 * and Templates were remove from the hbm files.
	 * @param containerInode
	 * @return
	 * @throws DotDataException
	 */
	List<Template> findTemplatesByContainerInode(final String containerInode) throws DotDataException;

	/**
	 * Check if a template is archived.
	 * @param template
	 * @return true if template is archived, false if not.
	 * @throws DotDataException
	 */
	boolean isArchived(final Template template) throws DotDataException;

}

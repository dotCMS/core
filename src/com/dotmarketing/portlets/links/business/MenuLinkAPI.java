package com.dotmarketing.portlets.links.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.business.DotStateException;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.portlets.links.model.Link;
import com.liferay.portal.model.User;

public interface MenuLinkAPI {
	
	/**
	 * Copy a menu link to the specified folder.
	 * 
	 * @param sourceLink
	 * @param destination
	 * @param user
	 * @param respectFrontendRoles
	 * @return Link
	 * @exception DotDataException
	 * @exception DotSecurityException
	 */
	public Link copy(Link sourceLink, Folder destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Preserves the menuLink in the given folder, if link already exists creates a new version of the given link and moves it to the given folder
	 * 
	 * @param menuLink
	 * @param destination
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void save(Link menuLink, Folder destination, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;
	
	/**
	 * Preserves the menuLink in the given folder, if link already exists creates a new version of the given link
	 * 
	 * @param menuLink
	 * @param destination
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void save(Link menuLink, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;	
	
	/**
	 * Delete the specified menu link.
	 * 
	 * @param menuLink
	 * @param user
	 * @param respectFrontendRoles
	 * @return boolean
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	public boolean delete(Link menuLink, User user, boolean respectFrontendRoles) throws DotSecurityException, Exception;
	
	/**
	 * 
	 * Finds a link by its id
	 * 
	 * @param id
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotSecurityException
	 * @throws Exception
	 */
	public Link findWorkingLinkById(String id, User user, boolean respectFrontendRoles) throws DotSecurityException, DotDataException;

	/**
	 * Retrieves the list of all working versions of menu links associated to a folder
	 * @param sourceHost
	 * @return
	 * @throws DotDataException 
	 * @throws DotStateException 
	 * @throws DotSecurityException 
	 */
	public List<Link> findFolderMenuLinks(Folder sourceFolder) throws DotStateException, DotDataException, DotSecurityException;
	
	/**
	 * Retrieves a paginated list of links the given user can read 
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
	public List<Link> findLinks(User user, boolean includeArchived, Map<String,Object> params, String hostId, String inode, String identifier, String parent, int offset, int limit, String orderBy) throws DotSecurityException, DotDataException;

    public int deleteOldVersions(Date assetsOlderThan) throws DotDataException, DotHibernateException;
	
}
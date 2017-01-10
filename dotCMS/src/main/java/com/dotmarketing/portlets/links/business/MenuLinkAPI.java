package com.dotmarketing.portlets.links.business;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.dotmarketing.beans.Host;
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



    /**
     * Finds a link based on its inode
     * @param inode
     * @return
     * @throws DotDataException 
     * @throws DotSecurityException 
     */
    public Link find(String inode, User user, boolean respectFrontEndRoles) throws DotDataException, DotSecurityException;

    /**
     * Moves the link to the specified host
     * 
     * @param link link to move 
     * @param host target host
     * @param user user to check permissions on
     * @param respectFrontEndRoles check for anonymous role permissions
     * @throws DotDataException if permission denied
     * @throws DotSecurityException data related exceptions 
     * @return if the link were moved. mostly if didn't existing in the target host
     */
    public boolean move(Link link, Host host, User user, boolean respectFrontEndRoles) throws DotSecurityException, DotDataException;
    
    /**
     * Moves the link to the specified folder
     * 
     * @param link link to move 
     * @param folder target folder
     * @param user user to check permissions on
     * @param respectFrontEndRoles check for anonymous role permissions
     * @throws DotDataException if permission denied
     * @throws DotSecurityException data related exceptions
     * @return if the link were moved. mostly if didn't existing in the target folder
     */
    public boolean move(Link link, Folder folder, User user, boolean respectFrontEndRoles) throws DotSecurityException, DotDataException;
    
    /**
	 * Method will replace user references of the given userId in MenuLinks 
	 * with the replacement user id 
	 * @param userId User Identifier
	 * @param replacementUserId The user id of the replacement user
	 * @throws DotDataException There is a data inconsistency
	 * @throws DotStateException There is a data inconsistency
	 * @throws DotSecurityException 
	 */
	public void updateUserReferences(String userId, String replacementUserId)throws DotDataException, DotSecurityException;

}
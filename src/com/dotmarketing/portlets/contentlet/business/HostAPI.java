/**
 *
 */
package com.dotmarketing.portlets.contentlet.business;

import java.util.List;

import org.apache.lucene.queryParser.ParseException;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.liferay.portal.model.User;

/**
 * @author jtesser
 * @author david torres
 *
 */
public interface HostAPI {

	/**
	 * Will return a List of a host's aliases If no host aliases will return empty list
	 * @param host
	 * @return
	 */
	public List<String> parseHostAliases(Host host);

	/**
	 *
	 * @return the default host
	 */
	public Host findDefaultHost(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @return the system host
	 */
	public Host findSystemHost() throws DotDataException;

	/**
	 *
	 * @param hostName
	 * @return the host with the passed in name
	 */
	public Host findByName(String hostName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 *
	 * @param hostName
	 * @return the host with the passed in name
	 */
	public Host findByAlias(String hostName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Marks the given host as archived
	 * @throws ParseException
	 * @throws DotContentletStateException
	 */
	public void archive(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;


	/**
	 * unmarks the given host as archived
	 * @throws ParseException
	 * @throws DotContentletStateException
	 */
	public void unarchive(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Removes the given host plus all assets under it, use with caution
	 * @throws ParseException
	 * @throws DotContentletStateException
	 */
	public void delete(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Retrieves a host given its id
	 */
	public Host find(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of all hosts in the system, that the given user has permissions to see
	 * @throws DotSecurityException
	 *
	 */
	public List<Host> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the list of all hosts in the system, that the given user has permissions to see
	 * @throws DotSecurityException
	 *
	 */
	public List<Host> findAllFromDB(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Saves the host into the system
	 */
	public Host save(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves all host the user has the required permission on
	 * @param permissionType
	 * @param includeArchived
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Host>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Host> getHostsWithPermission(int permissionType, boolean includeArchived, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves all host (including archived hosts) the user has the required permission on
	 * @param permissionType
	 * @param user
	 * @param respectFrontendRoles
	 * @return List<Host>
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public List<Host> getHostsWithPermission(int permissionType, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the system host
	 * @return
	 * @throws DotDataException
	 */
	public Host findSystemHost (User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the parent host of the given folder
	 * @param f
	 * @return
	 */
	public Host findParentHost(Folder f, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Retrieves the parent host of the given webasset
	 * @param asset
	 * @return
	 */
	public Host findParentHost(WebAsset asset, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;


	/**
	 * Retrieves the parent host of the given treeable
	 * @param asset
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Host findParentHost(Treeable asset, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Checks if a host contains a folder has its direct child
	 * @param parent Parent host
	 * @param folderName Name of the folder child of the host
	 * @return true if the folder is child of the parent host, false otherwise
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotSecurityException
	 */
	public boolean doesHostContainsFolder(Host parent, String folderName) throws DotDataException, DotSecurityException;

	/**
	 * Publishes (makes it live - available to view in the front-end)
	 * @param host
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 */
	public void publish(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;

	/**
	 * Un-publishes (makes it live - available to view in the front-end)
	 * @param host
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 */
	public void unpublish(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;

	/**
	 * Un-publishes (makes it live - available to view in the front-end)
	 * @param host
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotSecurityException
	 * @throws DotDataException
	 * @throws DotContentletStateException
	 */
	public void makeDefault(Host host, User user, boolean respectFrontendRoles) throws DotContentletStateException, DotDataException, DotSecurityException;

	/**
	 * Search for the specified host in the DB
	 *
	 * @param id
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Host DBSearch(String id, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Update the Host cache with the specified host
	 *
	 * @param host
	 */
	public void updateCache(Host host);

	/**
	 * Updates the VirtualLinks of the host with the new hostname
	 *
	 * @param host
	 */
	public void updateVirtualLinks(Host workinghost,Host updatedhost) throws DotDataException;

	/**
	 * Updates the MenuLinks of the host with the new hostname
	 *
	 * @param host
	 */
	public void updateMenuLinks(Host workinghost,Host updatedhost) throws DotDataException;

	/**
	 * Retrieves all hosts that matches a host's tag storage identifier
	 *
	 * @param host
	 */
	public List<Host> retrieveHostsPerTagStorage (String tagStorageId, User user);
}
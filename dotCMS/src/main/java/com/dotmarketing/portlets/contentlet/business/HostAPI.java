package com.dotmarketing.portlets.contentlet.business;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

/**
 * Encapsulate the Host maintenance operations
 * @author jtesser
 * @author david torres
 *
 */
public interface HostAPI {

	/**
	 * Will return a List of a host's aliases If no host aliases will return empty list
	 * @param host Host
	 * @return List
	 */
	public List<String> parseHostAliases(Host host);

	/**
	 * Find the default host
	 * @return the default host
	 */
	public Host findDefaultHost(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Find the system host
	 * @return the system host
	 */
	public Host findSystemHost() throws DotDataException;

	/**
	 * Find a host by name
	 * @param hostName
	 * @return the host with the passed in name
	 */
	public Host findByName(String hostName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Find a host based on the alias
	 * @param hostName
	 * @return the host with the passed in name
	 */
	public Host findByAlias(String hostName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Marks the given host as archived
     *
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotContentletStateException
     */
    public void archive(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;


    /**
     * Unmarks the given host as archived
     *
     * @param host
     * @param user
     * @param respectFrontendRoles
     * @throws DotDataException
     * @throws DotSecurityException
     * @throws DotContentletStateException
     */
	public void unarchive(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Removes the given host plus all assets under it, use with caution.
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public void delete(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException, DotContentletStateException;

	/**
	 * Removes the given host plus all assets under it, use with caution.
	 * It does all the job in a separated thread and returns immediately (it returns an optional Future that returns true when the runAsSeparatedThread is true).
	 *
	 * @throws DotDataException
	 * @throws DotSecurityException
	 * @throws DotContentletStateException
	 */
	public Optional<Future<Boolean>> delete(Host host, User user, boolean respectFrontendRoles, boolean runAsSeparatedThread) throws DotDataException, DotSecurityException, DotContentletStateException;

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
	public List<Host> findAll(User user, int limit, int offset, String sortBy, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	 * Updates default host information on the current host list, if the current host is marked as default and there is
	 * any other host marked as well, only the host sent as parameter will remain as default host.
	 * @param host
	 * @param user
	 * @param respectFrontendRoles
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public void updateDefaultHost(Host host, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	 * Updates the MenuLinks of the host with the new hostname
	 *
	 * @param workinghost
	 * @param updatedhost
	 */
	public void updateMenuLinks(Host workinghost, Host updatedhost) throws DotDataException;

	/**
	 * Retrieves all hosts that matches a host's tag storage identifier
	 *
	 * @param tagStorageId
	 * @param user
	 */
	public List<Host> retrieveHostsPerTagStorage (String tagStorageId, User user);
	
	
	/**
	 * This method takes a server name (from a web request) and maps it to a host.
	 * It is designed to do a lightweight cache lookup to get the mapping from server name -> host
	 * and to prevent unnecessary lucene lookups.
	 * If does not exists a host with that serverName then the default host is returned.
	 * @param serverName
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Host resolveHostName(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;

	/**
	 * This method takes a server name (from a web request) and maps it to a host.
	 * It is designed to do a lightweight cache lookup to get the mapping from server name -> host
	 * and to prevent unnecessary lucene lookups.
	 * If does not exists a host with that serverName then null is returned
	 * @param serverName
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public Host resolveHostNameWithoutDefault(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;


	/**
	 * Retrieves the subset of all hosts in the system
	 * that matches the current host name filter, offset and limit
	 * 
	 * @param filter characters to search in the host name
	 * @param showArchived boolean true if its requires that also archived content are returned, false if not
	 * @param showSystemHost boolean true if the system host could be included among the results, false if not 
	 * @param limit Max number of element to return
	 * @param offset First element of the subset to return
	 * @param user Current user to validate permissions
	 * @param respectFrontendRoles boolean true if its requires that front end role are take in count in the search, false if not
	 * @return PaginatedArrayList<Host> the subset of hosts that accomplish the search condition
	 * @throws DotDataException
	 * @throws DotSecurityException
	 */
	public PaginatedArrayList<Host> search(String filter, boolean showArchived, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles);

	/**
	 * Retrieves the subset of all hosts in the system
	 * that matches the current host name filter, offset and limit
	 *
	 * @param filter characters to search in the host name
	 * @param showStopped boolean true if its requires that also stopped host are returned, false if not
	 * @param showSystemHost boolean true if the system host could be included among the results, false if not
	 * @param limit Max number of element to return
	 * @param offset First element of the subset to return
	 * @param user Current user to validate permissions
	 * @param respectFrontendRoles boolean true if its requires that front end role are take in count in the search, false if not
	 * @return PaginatedArrayList<Host> the subset of hosts that accomplish the search condition
	 */
	public PaginatedArrayList<Host> searchByStopped(String filter, boolean showStopped, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles);

	/**
	 * Retrieves the subset of all hosts in the system
	 * that matches the current host name filter, offset and limit
	 *
	 * @param filter characters to search in the host name
	 * @param showArchived boolean true if its requires that also archived host are returned, false if not
	 * @param showStopped boolean true if its requires that also stopped host are returned, false if not
	 * @param showSystemHost boolean true if the system host could be included among the results, false if not
	 * @param limit Max number of element to return
	 * @param offset First element of the subset to return
	 * @param user Current user to validate permissions
	 * @param respectFrontendRoles boolean true if its requires that front end role are take in count in the search, false if not
	 * @return PaginatedArrayList<Host> the subset of hosts that accomplish the search condition
	 */
	public PaginatedArrayList<Host> search(String filter, boolean showArchived, boolean showStopped, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles);

	/**
	 * Retrieves the subset of all hosts in the system
	 * that matches the current host name filter, offset and limit
	 *
	 * @param filter characters to search in the host name
	 * @param showSystemHost boolean true if the system host could be included among the results, false if not
	 * @param limit Max number of element to return
	 * @param offset First element of the subset to return
	 * @param user Current user to validate permissions
	 * @param respectFrontendRoles boolean true if its requires that front end role are take in count in the search, false if not
	 * @return PaginatedArrayList<Host> the subset of hosts that accomplish the search condition
	 */
	public PaginatedArrayList<Host> search(String filter, boolean showSystemHost, int limit, int offset, User user, boolean respectFrontendRoles);

	/**
	 * Return the number of sites for user
	 *
	 * @param user
	 * @param respectFrontendRoles
	 * @return
	 */
	public long count(User user, boolean respectFrontendRoles);
}
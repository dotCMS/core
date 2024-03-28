package com.dotmarketing.portlets.contentlet.business;

import com.dotmarketing.beans.Host;
import com.dotmarketing.beans.WebAsset;
import com.dotmarketing.business.Treeable;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.contentlet.model.Contentlet;
import com.dotmarketing.portlets.folders.model.Folder;
import com.dotmarketing.util.PaginatedArrayList;
import com.liferay.portal.model.User;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

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

    /**
     * Returns the aliases of a given Site in the form of a list. In the Site's properties, aliases can be separated by:
     * <ul>
     *     <li>Commas.</li>
     *     <li>Blank spaces.</li>
     *     <li>Line breaks.</li>
     * </ul>
     *
     * @param site The Site whose aliases must be returned.
     *
     * @return The {@link List} of aliases for a Site.
     */
    List<String> parseHostAliases(Host site);

    /**
     * Returns the "default" Site in the current data repository.
     *
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The default host from cache.  If not found, returns from content search and adds to cache
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    Host findDefaultHost(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Find the system host
	 * @return the system host
	 */
	public Host findSystemHost() throws DotDataException;

    /**
     * Finds a Site in the repository, based on its name. If it cannot be found or if an error ocurred, the "default"
     * Site will be returned instead.
     *
     * @param siteName             The name of the Site
     * @param user                 The {@link User} that is calling this method.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The Site with the specified name, or the "default" Site.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    Host findByName(String siteName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns the Site that matches the specified Identifier or Site Key -- aka, Site Name.
	 *
	 * @param siteIdOrKey          The Identifier or Site Key of the Site.
	 * @param user                 The {@link User} that is calling this method.
	 * @param respectFrontendRoles If the User's front-end roles need to be taken into account in
	 *                             order to perform this operation, set to {@code true}. Otherwise,
	 *                             set to {@code false}.
	 *
	 * @return The {@link Host} object that matches the specified Identifier or Site Key.
	 *
	 * @throws DotDataException     An error occurred when accessing the database.
	 * @throws DotSecurityException The specified User does not have the required permissions to
	 *                              perform this operation.
	 */
	Optional<Host> findByIdOrKey(final String siteIdOrKey, final User user,
								 final boolean respectFrontendRoles) throws DotDataException,
			DotSecurityException;

    /**
     * Returns the Site that matches the specified alias. Depending on the existing data, the result may vary:
     * <ol>
     *  <li>If one single Site matches the alias, then such a Site will be returned.</li>
     *  <li>If no Site matches the alias, then a {@code null} value is returned.</li>
     *  <li>If two or more Sites matches the alias, then:
     *      <ol>
     *          <li>If one of those Sites is the "deault" Site, then it will be returned.</li>
     *          <li>Otherwise, the first Site in the result set is returned.</li>
     *      </ol>
     *  </li>
     * </ol>
     *
     * @param alias                The alias of the Site.
     * @param user                 The {@link User} that is calling this method.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The {@link Host} object matching the alias, the "default" Site, or the first Site from the result set.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    Host findByAlias(final String alias, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	 * Retrieves a host given a {@link Contentlet} to resolve from there the actual host id
	 */
	Host find(Contentlet contentlet,
			  User user,
			  boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * @deprecated This method is basically duplicated code. Use one of the following methods instead:
	 * <ul>
	 *     <li>{@link #findAllFromDB(User, boolean)}</li>
	 *     <li>{@link #findAllFromCache(User, boolean)}</li>
	 *     <li>{@link #search(String, boolean, boolean, int, int, User, boolean)}</li>
	 * </ul>
     *
     * Retrieves the list of all hosts in the system, that the given user has permissions to see
     *
     * @throws DotSecurityException
     */
    @Deprecated
    List<Host> findAll(User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns the list of Sites in your dotCMS repository retrieved <b>directly from the data source</b> matching the
	 * specified search criteria.
	 *
	 * @param user                 The {@link User} that is calling this method.
	 * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
	 * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
	 * @param sortBy               Optional sorting criterion, as specified by the available columns in: {@link
	 *                             com.dotmarketing.common.util.SQLUtil#ORDERBY_WHITELIST}
	 * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
	 *                             operation, set to {@code true}. Otherwise, set to {@code false}.
	 *
	 * @return The list of {@link Host} objects.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException The specified User does not have the required permissions to perform this
	 *                              operation.
	 */
	List<Host> findAll(User user, int limit, int offset, String sortBy, boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Returns the complete list of Sites in your dotCMS repository retrieved <b>directly from the data source</b>,
	 * including the System Host.
     *
     * @param user                 The {@link User} that is calling this method.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    List<Host> findAllFromDB(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

	/**
	 * Returns the complete list of Sites in your dotCMS repository retrieved <b>directly from the data source</b>. This
	 * method allows you to <b>EXCLUDE</b> the System Host from the result list.
	 *
	 * @param user                 The {@link User} that is calling this method.
	 * @param includeSystemHost    If the System Host must be included in the results, set to {@code true}.
	 * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
	 *                             operation, set to {@code true}. Otherwise, set to {@code false}.
	 *
	 * @return The list of {@link Host} objects.
	 *
	 * @throws DotDataException     An error occurred when accessing the data source.
	 * @throws DotSecurityException The specified User does not have the required permissions to perform this
	 *                              operation.
	 */
	List<Host> findAllFromDB(final User user, final boolean includeSystemHost, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Returns the complete list of Sites in your dotCMS repository retrieved from the cache. If no data is currently
     * available, it will be retrieved from the data source, and put into the respective cache region.
     *
     * @param user                 The {@link User} that is calling this method.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
    List<Host> findAllFromCache(final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
     * @deprecated The {@code includeArchived} parameter allows you to retrieve archived Sites. However, non-live
     * contents cannot be checked for permissions, and an empty list will always be returned. Use the following method
     * instead:
     * <ul>
     *     <li>{{@link #getHostsWithPermission(int, User, boolean)}</li>
     * </ul>
     */
    @Deprecated
    List<Host> getHostsWithPermission(final int permissionType, final boolean includeArchived, final User user, final
    boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

    /**
     * Returns all Sites in the dotCMS content repository which match the specified permission criteria, whether
	 * they're archived or not. The currently available Permission Types are the following:
     * <ul>
     *  <li>{@link com.dotmarketing.business.PermissionAPI#PERMISSION_READ}</li>
     *  <li>{@link com.dotmarketing.business.PermissionAPI#PERMISSION_USE}</li>
     *  <li>{@link com.dotmarketing.business.PermissionAPI#PERMISSION_EDIT}</li>
     *  <li>{@link com.dotmarketing.business.PermissionAPI#PERMISSION_WRITE}</li>
     *  <li>{@link com.dotmarketing.business.PermissionAPI#PERMISSION_PUBLISH}</li>
     *  <li>{@link com.dotmarketing.business.PermissionAPI#PERMISSION_EDIT_PERMISSIONS}</li>
     *  <li>{@link com.dotmarketing.business.PermissionAPI#PERMISSION_CAN_ADD_CHILDREN}</li>
     * </ul>
     *
     * @param permissionType       The type of Permission that must be checked for all the Sites in the result set.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects that match the required filtering criteria.
     *
     * @throws DotDataException     An error occurred when accessing the data source.
     * @throws DotSecurityException The specified User does not have the required permissions to perform this
     *                              operation.
     */
	List<Host> getHostsWithPermission(final int permissionType, final User user, final boolean respectFrontendRoles) throws DotDataException, DotSecurityException;

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
	public Optional<Host> resolveHostNameWithoutDefault(String serverName, User user, boolean respectFrontendRoles) throws DotDataException, DotSecurityException ;

    /**
     * Returns the list of Sites – with pagination capabilities – that match the specified search criteria. This method
     * allows users to specify three search parameters:
     * <ol>
     *  <li>{@code filter}: Finds Sites whose name starts with the specified String.</li>
     *  <li>{@code showArchived}: Determines if archived Sites are returned in the result set.</li>
     *  <li>{@code showSystemHost}: Determines whether the System Host must be returned or not.</li>
     * </ol>
     *
     * @param filter               The initial part or full name of the Site you need to look up.
     * @param showArchived         If archived Sites must be returned, set to {@code true}. Otherwise, se to {@code
     *                             false}.
     * @param showSystemHost       If the System Host object must be returned, set to {@code true}. Otherwise, se to
     *                             {@code false}.
     * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects that match the specified search criteria.
     */
    PaginatedArrayList<Host> search(final String filter, final boolean showArchived, final boolean showSystemHost,
                                    final int limit, final int offset, final User user, final boolean
                                            respectFrontendRoles);

    /**
     * Returns the list of Sites – with pagination capabilities – that match the specified search criteria. This method
     * allows users to specify three search parameters:
     * <ol>
     *  <li>{@code filter}: Finds Sites whose name starts with the specified String.</li>
     *  <li>{@code showStopped}: Determines if stopped Sites are returned in the result set.</li>
     *  <li>{@code showSystemHost}: Determines whether the System Host must be returned or not.</li>
     * </ol>
	 * It's very important to note that, if the {@code showStopped} parameter is set to {@code true}, then all archived
	 * Sites will also be returned because they're considered stopped Sites.
     *
     * @param filter               The initial part or full name of the Site you need to look up.
     * @param showStopped          If stopped Sites must be returned, set to {@code true}. Otherwise, se to {@code
     *                             false}.
     * @param showSystemHost       If the System Host object must be returned, set to {@code true}. Otherwise, se to
     *                             {@code false}.
     * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects that match the specified search criteria.
     */
    PaginatedArrayList<Host> searchByStopped(final String filter, final boolean showStopped, final boolean
            showSystemHost, final int limit, final int offset, final User user, final boolean respectFrontendRoles);

    /**
     * Returns the list of Sites – with pagination capabilities – that match the specified search criteria. This method
     * allows users to specify three search parameters:
     * <ol>
     *  <li>{@code filter}: Finds Sites whose name starts with the specified String.</li>
     *  <li>{@code showArchived}: Determines if archived Sites are returned in the result set.</li>
     *  <li>{@code showStopped}: Determines if stopped Sites are returned in the result set.</li>
     *  <li>{@code showSystemHost}: Determines whether the System Host must be returned or not.</li>
     * </ol>
     *
     * @param filter               The initial part or full name of the Site you need to look up.
     * @param showArchived         If archived Sites must be returned, set to {@code true}. Otherwise, se to {@code
     *                             false}.
     * @param showStopped          If stopped Sites must be returned, set to {@code true}. Otherwise, se to {@code
     *                             false}.
     * @param showSystemHost       If the System Host object must be returned, set to {@code true}. Otherwise, se to
     *                             {@code false}.
     * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects that match the specified search criteria.
     */
    PaginatedArrayList<Host> search(final String filter, final boolean showArchived, final boolean showStopped, final
    boolean showSystemHost, final int limit, final int offset, final User user, final boolean respectFrontendRoles);

    /**
     * Returns the list of live Sites – with pagination capabilities – that match the specified search criteria. This
	 * method allows users to specify three search parameters:
     * <ol>
     *  <li>{@code filter}: Finds Sites whose name starts with the specified String.</li>
     *  <li>{@code showSystemHost}: Determines whether the System Host must be returned or not.</li>
     * </ol>
     *
     * @param filter               The initial part or full name of the Site you need to look up.
     * @param showSystemHost       If the System Host object must be returned, set to {@code true}. Otherwise, se to
     *                             {@code false}.
     * @param limit                Limit of results returned in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param offset               Expected offset of results in the response, for pagination purposes. If set equal or
	 *                             lower than zero, this parameter will be ignored.
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The list of {@link Host} objects that match the specified search criteria.
     */
    PaginatedArrayList<Host> search(final String filter, final boolean showSystemHost, final int limit, final int
            offset, final User user, final boolean respectFrontendRoles);

    /**
     * Returns the total number of Sites that exist in your content repository.
     *
     * @param user                 The {@link User} performing this action.
     * @param respectFrontendRoles If the User's front-end roles need to be taken into account in order to perform this
     *                             operation, set to {@code true}. Otherwise, set to {@code false}.
     *
     * @return The total number of Sites.
     */
    long count(final User user, final boolean respectFrontendRoles);

}
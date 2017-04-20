package com.dotmarketing.portlets.virtuallinks.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotDataException;
import com.dotmarketing.exception.DotDuplicateDataException;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.exception.DotRuntimeException;
import com.dotmarketing.exception.DotSecurityException;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;
import com.liferay.portal.model.User;

/**
 * This API provides access to the information related to Vanity URLs (a.k.a.
 * Virtual Links) in dotCMS. Vanity URLs are alternate reference paths to
 * internal or external URL's. Vanity URLs are most commonly used to give
 * visitors to the website a more user-friendly or memorable way of reaching an
 * HTML page or File, that might actually live “buried” in a much deeper path.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public interface VirtualLinkAPI {

	/** Separator character used for building the complete URL. */
	public String URL_SEPARATOR = ":";
	
	/**
	 * Exposes the different ways that records can be ordered by when querying
	 * information from the data source.
	 */
	public enum OrderBy {
		TITLE,
		DATE_ADDED,
		URL
	}

	/**
	 * Returns all the {@link VirtualLink} objects filtered by the title and/or
	 * URL.
	 *
	 * @param title
	 *            - The Vanity URL title (optional).
	 * @param url
	 *            - The URL of the Vanity URL.
	 * @param orderby
	 *            - The order-by clause (optional). See
	 *            {@link VirtualLinkAPI.OrderBy}.
	 * @return The list of {@link VirtualLink} that match the search criteria.
	 */
	public List<VirtualLink> getVirtualLinks(String title, String url, OrderBy orderby);

	/**
	 * Copies the specified {@link VirtualLink} object to the specified
	 * destination site.
	 * 
	 * @param sourceVirtualLink
	 *            - The Vanity URL that will be copied.
	 * @param destinationSite
	 *            - The site where the Vanity URL will be copied.
	 * @return The copied {@link VirtualLink} object.
	 * @throws DotHibernateException
	 *             An error occurred when copying the information in the data
	 *             source.
	 */
	public VirtualLink copyVirtualLink(VirtualLink sourceVirtualLink, Host destinationSite) throws DotHibernateException;

	/**
	 * Returns all the {@link VirtualLink} objects associated to a site
	 * 
	 * @param site
	 *            - The site whose Vanity URLs will be returned.
	 * @return The Vanity URLs that belong to the specified site.
	 */
	public List<VirtualLink> getHostVirtualLinks(Host site);

	/**
	 * Returns all the {@link VirtualLink} objects filtered by title that belong
	 * to the list of specified sites.
	 *
	 * @param title
	 *            - The Vanity URL title (optional).
	 * @param sites
	 *            - The sites whose Vanity URLs will be retrieved.
	 * @param orderby
	 *            - The order-by clause (optional). See
	 *            {@link VirtualLinkAPI.OrderBy}.
	 * @return The list of {@link VirtualLink} that match the search criteria.
	 */
	public List<VirtualLink> getVirtualLinks(String title, List<Host> hosts, OrderBy orderby);

	/**
	 * Determines the list of {@link VirtualLink} objects that the specified
	 * {@link User} can modify. The result list is based on the sites where such
	 * a user has permissions to edit Vanity URLs.
	 * 
	 * @param list
	 *            - The list of Vanity URLs that will be verified.
	 * @param user
	 *            - The user whose edit permissions on the Vanity URLs will be
	 *            checked.
	 * @return The list of Vanity URLs with granted permissions to the
	 *         specified user.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the data
	 *             source.
	 * @throws DotSecurityException
	 */
	public List<VirtualLink> checkListForCreateVirtualLinkspermission(List<VirtualLink> list,User user) throws DotDataException, DotSecurityException;

	/**
	 * Checks if the specified {@link User} has permissions to edit the
	 * specified {@link VirtualLink}.
	 * 
	 * @param link
	 *            - The Vanity URL to edit.
	 * @param user
	 *            - The user whose permissions will be verified.
	 * @return If the user has permissions to edit the Vanity URL, returns the
	 *         {@link VirtualLink} object. Otherwise, returns {@code null}.
	 * @throws DotDataException
	 *             An error occurred when retrieving information from the data
	 *             source.
	 * @throws DotSecurityException
	 */
	public VirtualLink checkVirtualLinkForEditPermissions(VirtualLink link,User user) throws DotDataException, DotSecurityException;

	/**
	 * Returns all the <b>active and non-active</b> {@link VirtualLink} objects
	 * associated to the specified URI.
	 * 
	 * @param uri
	 *            - The URI associated to one or more Vanity URLs.
	 * @return The Vanity URLs whose URI matches the specified value.
	 */
	public List<VirtualLink> getVirtualLinksByURI(String uri);

	/**
	 * Returns the list of <b>active</b> {@link VirtualLink} objects that point
	 * to the specified URI. This URI represents a path to a page inside the
	 * dotCMS content repository.
	 * 
	 * @param uri
	 *            - The page URI inside dotCMS.
	 * @return The list of Vanity URLs.
	 */
	public List<VirtualLink> getIncomingVirtualLinks(String uri);

	/**
	 * Returns the {@link VirtualLink} object associated to the specified URL.
	 * This URL represents the custom URL that maps to a real page path inside
	 * dotCMS.
	 * 
	 * @param url
	 *            - The custom Vanity URL.
	 * @return The associated Vanity URL.
	 * @throws DotHibernateException
	 *             An error occurred when retrieving the data.
	 */
	public VirtualLink getVirtualLinkByURL(String url) throws DotHibernateException;

	/**
	 * Returns the list of all {@link VirtualLink} objects that are active in
	 * the system.
	 * 
	 * @return The list of active Vanity URLs.
	 */
	public List<VirtualLink> getActiveVirtualLinks();

	/**
	 * Returns the {@link VirtualLink} associated to the specified Inode.
	 * 
	 * @param inode
	 *            - The Vanity URL's Inode.
	 * @return The associated Vanity URL.
	 */
	public VirtualLink getVirtualLink(String inode);

	/**
	 * Returns all the {@link VirtualLink} objects filtered by the specified
	 * condition. This condition can represent either the title or the URL of
	 * the Vanity URL.
	 * 
	 * @param condition
	 *            - A piece of or the complete title or URL.
	 * @param orderby
	 *            - The order-by clause: "title", "iDate", or "url".
	 * @return The list of Vanity URLs that match the search criteria.
	 */
	public List<VirtualLink> getVirtualLinks(String condition, String orderby);

	/**
	 * Creates a {@link VirtualLink} object.
	 * 
	 * @param title
	 *            - The title.
	 * @param url
	 *            - The custom URL.
	 * @param uri
	 *            - The internal dotCMS URI.
	 * @param isActive
	 *            - If set to {@code true}, the Vanity URL is active. Otherwise,
	 *            set to {@code false}.
	 * @param site
	 *            - The site where the Vanity URL will be created.
	 * @param user
	 *            - The {@link User} calling this method.
	 * @return The complete Vanity URL object.
	 * @throws DotRuntimeException
	 *             The user does not have access to the specified {@link Host}.
	 */
	public VirtualLink create(final String title, final String url, final String uri, final boolean isActive, Host site,
			User user);

	/**
	 * Saves or updates the specified {@link VirtualLink} object in dotCMS.
	 * 
	 * @param virtualLink
	 *            - The Vanity URL to save.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @return The saved Vanity URL.
	 * @throws DotDuplicateDataException
	 *             Another Vanity URL with the same URL already exists.
	 * @throws DotDataException
	 *             An error occurred when saving the data.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform this
	 *             action.
	 */
	public void save(VirtualLink vanityUrl, User user) throws DotDataException, DotSecurityException;

	/**
	 * Deletes the specified {@link VirtualLink} object.
	 * 
	 * @param vanityUrl
	 *            - The Vanity URL to delete.
	 * @param user
	 *            - The {@link User} performing this action.
	 * @throws DotDataException
	 *             An error occurred when saving the data.
	 * @throws DotSecurityException
	 *             The specified user does not have permissions to perform this
	 *             action.
	 */
	public void delete(VirtualLink vanityUrl, User user) throws DotDataException, DotSecurityException;

}

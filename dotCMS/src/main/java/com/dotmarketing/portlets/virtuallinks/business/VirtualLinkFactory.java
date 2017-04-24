package com.dotmarketing.portlets.virtuallinks.business;

import java.util.List;

import com.dotmarketing.beans.Host;
import com.dotmarketing.exception.DotHibernateException;
import com.dotmarketing.portlets.virtuallinks.model.VirtualLink;

/**
 * This class provides methods to interact with Vanity URLs (a.k.a.
 * Virtual Links) in dotCMS.
 * 
 * @author root
 * @version 1.x
 * @since Mar 22, 2012
 *
 */
public interface VirtualLinkFactory {

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
	public List<VirtualLink> getVirtualLinks(String title, String url, VirtualLinkAPI.OrderBy orderby);

	/**
	 * Returns all the {@link VirtualLink} objects associated to a site
	 * 
	 * @param site
	 *            - The site whose Vanity URLs will be returned.
	 * @return The Vanity URLs that belong to the specified site.
	 */
	public List<VirtualLink> getHostVirtualLinks(Host site);

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
	public List<VirtualLink> getVirtualLinks(String title, List<Host> sites, VirtualLinkAPI.OrderBy orderby);

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
	 * @return The associated Vanity URL
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
	 * Saves or updates the specified {@link VirtualLink} object in dotCMS.
	 * 
	 * @param virtualLink
	 *            - The Vanity URL.
	 */
	public void save(VirtualLink vanityUrl);

	/**
	 * Deletes the specified {@link VirtualLink} object in dotCMS.
	 * 
	 * @param virtualLink
	 *            - The Vanity URL.
	 * @throws DotHibernateException
	 *             An error occurred when deleting the data.
	 */
	public void delete(VirtualLink vanityUrl) throws DotHibernateException;

}
